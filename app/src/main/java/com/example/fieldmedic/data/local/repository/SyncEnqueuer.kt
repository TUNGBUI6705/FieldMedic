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

    private fun mergeBaseValues(oldBaseJson: String, newBaseJson: String): String {
        val oldObj = try { Json.parseToJsonElement(oldBaseJson).jsonObject } catch (_: Exception) { JsonObject(emptyMap()) }
        val newObj = try { Json.parseToJsonElement(newBaseJson).jsonObject } catch (_: Exception) { JsonObject(emptyMap()) }
        val merged = JsonObject(newObj + oldObj)
        return Json.encodeToString(JsonObject.serializer(), merged)
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
        require(operation != QueueOperation.CREATE || baseVersion == 0) { "CREATE must have baseVersion 0" }
        val db = dbProvider()
        db.withTransaction {
            val dao = db.syncDao()
            val existing = dao.findByStatus(entityType, entityId, QueueStatus.PENDING)

            if (existing != null) {
                when {
                    existing.operation == QueueOperation.CREATE && operation == QueueOperation.UPDATE -> {
                        val existingObj = Json.parseToJsonElement(existing.payload).jsonObject
                        val newObj = Json.parseToJsonElement(changedFieldsJson).jsonObject
                        val merged = JsonObject(existingObj + newObj)
                        val mergedPayload = Json.encodeToString(JsonObject.serializer(), merged)

                        val mergedBase = mergeBaseValues(existing.baseValues, baseValuesJson)

                        val updated = existing.copy(
                            payload = mergedPayload,
                            baseValues = mergedBase,
                            priority = minOf(existing.priority, priority),
                            dependsOn = dependsOn ?: existing.dependsOn
                        )
                        dao.upsert(updated)
                    }
                    existing.operation == QueueOperation.CREATE && operation == QueueOperation.DELETE -> {
                        dao.delete(existing.id)
                    }
                    existing.operation == QueueOperation.UPDATE && operation == QueueOperation.DELETE -> {
                        val mergedBase = mergeBaseValues(existing.baseValues, baseValuesJson)

                        val updated = existing.copy(
                            operation = QueueOperation.DELETE,
                            payload = changedFieldsJson,
                            baseValues = mergedBase,
                            priority = minOf(existing.priority, priority),
                            dependsOn = dependsOn ?: existing.dependsOn
                        )
                        dao.upsert(updated)
                    }
                    existing.operation == QueueOperation.UPDATE && operation == QueueOperation.UPDATE -> {
                        val existingObj = Json.parseToJsonElement(existing.payload).jsonObject
                        val newObj = Json.parseToJsonElement(changedFieldsJson).jsonObject
                        val merged = JsonObject(existingObj + newObj)
                        val mergedPayload = Json.encodeToString(JsonObject.serializer(), merged)

                        val mergedBase = mergeBaseValues(existing.baseValues, baseValuesJson)

                        val updated = existing.copy(
                            payload = mergedPayload,
                            baseValues = mergedBase,
                            priority = minOf(existing.priority, priority),
                            dependsOn = dependsOn ?: existing.dependsOn
                        )
                        dao.upsert(updated)
                    }
                    else -> {
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
