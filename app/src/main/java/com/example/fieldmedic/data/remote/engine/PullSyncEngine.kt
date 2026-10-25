package com.example.fieldmedic.data.remote.engine

import com.example.fieldmedic.data.local.dao.SyncCursorDao
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.SyncCursor
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.tasks.await

class PullSyncEngine(
    private val dbProvider: () -> AppDatabase,
    private val syncCursorDao: SyncCursorDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
    }
) {

    suspend fun pullCollection(collectionName: String, now: Long = System.currentTimeMillis()): Int {
        val cursor = syncCursorDao.get(collectionName)
        val lastPullTs = cursor?.lastPullServerTs ?: 0L

        val snapshot = firestore.collection(collectionName)
            .whereGreaterThan("serverUpdatedAt", lastPullTs)
            .get()
            .await()

        if (snapshot.isEmpty) {
            syncCursorDao.upsert(
                SyncCursor(
                    collection = collectionName,
                    lastPullServerTs = lastPullTs,
                    lastSuccessAt = now
                )
            )
            return 0
        }

        var maxServerTs = lastPullTs
        val documents = snapshot.documents

        for (doc in documents) {
            val serverTs = doc.getLong("serverUpdatedAt") ?: 0L
            if (serverTs > maxServerTs) {
                maxServerTs = serverTs
            }
        }

        syncCursorDao.upsert(
            SyncCursor(
                collection = collectionName,
                lastPullServerTs = maxServerTs,
                lastSuccessAt = now
            )
        )

        return documents.size
    }

    suspend fun pullAll(now: Long = System.currentTimeMillis()): Map<String, Int> {
        val collections = listOf("patients", "encounters", "vitals", "photos")
        val results = mutableMapOf<String, Int>()

        for (col in collections) {
            try {
                val pulledCount = pullCollection(col, now)
                results[col] = pulledCount
            } catch (_: Exception) {
                results[col] = -1
            }
        }

        return results
    }
}
