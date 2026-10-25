package com.example.fieldmedic.di

import com.example.fieldmedic.data.local.dao.SyncCursorDao
import com.example.fieldmedic.data.local.dao.SyncDao
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.remote.engine.FirestoreRealtimeListener
import com.example.fieldmedic.data.remote.engine.PullSyncEngine
import com.example.fieldmedic.data.remote.transport.FirestoreSyncTransport

class NetworkModule(
    private val dbProvider: () -> AppDatabase,
    private val syncDaoProvider: () -> SyncDao,
    private val syncCursorDaoProvider: () -> SyncCursorDao
) {

    val firestoreSyncTransport: FirestoreSyncTransport by lazy {
        FirestoreSyncTransport(syncDaoProvider())
    }

    val pullSyncEngine: PullSyncEngine by lazy {
        PullSyncEngine(dbProvider, syncCursorDaoProvider())
    }

    val realtimeListener: FirestoreRealtimeListener by lazy {
        FirestoreRealtimeListener(pullSyncEngine)
    }
}
