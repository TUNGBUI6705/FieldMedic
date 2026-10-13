package com.example.fieldmedic.data.local.repository

import androidx.room.withTransaction
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.QueueOperation
import com.example.fieldmedic.data.local.entity.QueueStatus
import com.example.fieldmedic.data.local.entity.SyncEntityType
import com.example.fieldmedic.data.local.entity.SyncQueue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.util.UUID

class SyncEnqueuer(private val dbProvider: () -> AppDatabase) {

    companion object {
        const val ALERT_ENCOUNTER = 0
        const val PATIENT = 1
        const val ENCOUNTER = 2
        const val PHOTO = 3
    }

    suspend fun enqueue(
        entityType: SyncEntityType,
        entityId: String,
        patientId: String?,
        operation: QueueOperation,
        changedFieldsJson: String,
        baseValuesJson: String,
        baseVersion: Int,
        priority: Int,
        dependsOn: String?,
        now: Long = System.currentTimeMillis()
    ) {
        val db = dbProvider()
        db.withTransaction {
            val dao = db.syncDao()
            val existing = dao.findPending(entityType, entityId)

            if (existing != null && existing.status == QueueStatus.PENDING) {
                when {
                    existing.operation == QueueOperation.CREATE && operation == QueueOperation.UPDATE -> {
                        val existingObj = Json.parseToJsonElement(existing.payload).jsonObject
                        val newObj = Json.parseToJsonElement(changedFieldsJson).jsonObject
                        val merged = JsonObject(existingObj + newObj)
                        val mergedPayload = Json.encodeToString(JsonObject.serializer(), merged)

                        val updated = existing.copy(
                            payload = mergedPayload,
                            priority = minOf(existing.priority, priority),
                            dependsOn = dependsOn ?: existing.dependsOn
                        )
                        dao.upsert(updated)
                    }
                    existing.operation == QueueOperation.CREATE && operation == QueueOperation.DELETE -> {
                        dao.delete(existing.id)
                    }
                    existing.operation == QueueOperation.UPDATE && operation == QueueOperation.DELETE -> {
                        val updated = existing.copy(
                            operation = QueueOperation.DELETE,
                            payload = changedFieldsJson,
                            priority = minOf(existing.priority, priority),
                            dependsOn = dependsOn ?: existing.dependsOn
                        )
                        dao.upsert(updated)
                    }
                    else -> {
                        if (existing.operation == QueueOperation.UPDATE && operation == QueueOperation.UPDATE) {
                            val existingObj = Json.parseToJsonElement(existing.payload).jsonObject
                            val newObj = Json.parseToJsonElement(changedFieldsJson).jsonObject
                            val merged = JsonObject(existingObj + newObj)
                            val mergedPayload = Json.encodeToString(JsonObject.serializer(), merged)

                            val updated = existing.copy(
                                payload = mergedPayload,
                                priority = minOf(existing.priority, priority),
                                dependsOn = dependsOn ?: existing.dependsOn
                            )
                            dao.upsert(updated)
                        } else {
                            val newItem = SyncQueue(
                                id = UUID.randomUUID().toString(),
                                entityType = entityType,
                                entityId = entityId,
                                patientId = patientId,
                                operation = operation,
                                payload = changedFieldsJson,
                                baseValues = baseValuesJson,
                                baseVersion = baseVersion,
                                status = QueueStatus.PENDING,
                                priority = priority,
                                dependsOn = dependsOn,
                                idempotencyKey = UUID.randomUUID().toString(),
                                createdAt = now,
                                nextAttemptAt = now
                            )
                            dao.upsert(newItem)
                        }
                    }
                }
            } else {
                val newItem = SyncQueue(
                    id = UUID.randomUUID().toString(),
                    entityType = entityType,
                    entityId = entityId,
                    patientId = patientId,
                    operation = operation,
                    payload = changedFieldsJson,
                    baseValues = baseValuesJson,
                    baseVersion = baseVersion,
                    status = QueueStatus.PENDING,
                    priority = priority,
                    dependsOn = dependsOn,
                    idempotencyKey = UUID.randomUUID().toString(),
                    createdAt = now,
                    nextAttemptAt = now
                )
                dao.upsert(newItem)
            }
        }
    }
}
