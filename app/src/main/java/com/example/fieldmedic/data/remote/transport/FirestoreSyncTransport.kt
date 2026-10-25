package com.example.fieldmedic.data.remote.transport

import com.example.fieldmedic.data.local.dao.SyncDao
import com.example.fieldmedic.data.local.entity.QueueOperation
import com.example.fieldmedic.data.local.entity.SyncErrorCategory
import com.example.fieldmedic.data.local.entity.SyncQueue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

class FirestoreSyncTransport(
    private val syncDao: SyncDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // Tắt persistence SDK theo quy định trong AGENTS.md
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
    }
) {

    suspend fun pushBatch(now: Long = System.currentTimeMillis(), limit: Int = 20): Int {
        val batch = syncDao.nextBatch(now, limit)
        if (batch.isEmpty()) return 0

        var successCount = 0
        for (item in batch) {
            syncDao.markInFlight(item.id)
            try {
                processQueueItem(item)
                syncDao.delete(item.id)
                successCount++
            } catch (e: Exception) {
                val category = determineErrorCategory(e)
                val backoffMs = calculateBackoff(item.attempts + 1)
                val nextAttemptAt = now + backoffMs
                syncDao.markError(
                    id = item.id,
                    category = category,
                    message = e.localizedMessage ?: e.toString(),
                    nextAttemptAt = nextAttemptAt,
                    attempts = item.attempts + 1
                )
            }
        }
        return successCount
    }

    private suspend fun processQueueItem(item: SyncQueue) {
        val collectionName = getCollectionName(item.entityType.name)
        val docRef = firestore.collection(collectionName).document(item.entityId)

        when (item.operation) {
            QueueOperation.CREATE, QueueOperation.UPDATE -> {
                val payloadMap = jsonToMap(Json.parseToJsonElement(item.payload))
                docRef.set(payloadMap).await()
            }
            QueueOperation.DELETE -> {
                docRef.delete().await()
            }
        }
    }

    private fun getCollectionName(entityTypeName: String): String {
        return entityTypeName.lowercase() + "s"
    }

    private fun jsonToMap(element: JsonElement): Map<String, Any?> {
        val obj = element as? JsonObject ?: return emptyMap()
        val result = mutableMapOf<String, Any?>()
        for ((key, value) in obj) {
            result[key] = jsonElementToAny(value)
        }
        return result
    }

    private fun jsonElementToAny(element: JsonElement): Any? {
        return when (element) {
            is JsonNull -> null
            is JsonObject -> jsonToMap(element)
            is JsonArray -> element.map { jsonElementToAny(it) }
            else -> {
                val primitive = element.jsonPrimitive
                when {
                    primitive.isString -> primitive.content
                    primitive.booleanOrNull != null -> primitive.boolean
                    primitive.longOrNull != null -> primitive.long
                    primitive.doubleOrNull != null -> primitive.double
                    else -> primitive.content
                }
            }
        }
    }

    private fun determineErrorCategory(e: Exception): SyncErrorCategory {
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("network") || msg.contains("unavailable") -> SyncErrorCategory.NETWORK
            msg.contains("permission") || msg.contains("denied") -> SyncErrorCategory.PERMISSION
            msg.contains("unauthenticated") -> SyncErrorCategory.AUTH_EXPIRED
            else -> SyncErrorCategory.SERVER_BUSY
        }
    }

    private fun calculateBackoff(attempts: Int): Long {
        return when (attempts) {
            1 -> 5000L       // 5s
            2 -> 15000L      // 15s
            3 -> 60000L      // 1m
            4 -> 300000L     // 5m
            else -> 1800000L // 30m
        }
    }
}
