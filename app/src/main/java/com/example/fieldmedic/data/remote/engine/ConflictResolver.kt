package com.example.fieldmedic.data.remote.engine

import com.example.fieldmedic.data.local.dao.SyncConflictDao
import com.example.fieldmedic.data.local.entity.SyncConflict
import kotlinx.coroutines.flow.Flow

class ConflictResolver(
    private val conflictDao: SyncConflictDao
) {

    fun observeOpenConflicts(): Flow<List<SyncConflict>> {
        return conflictDao.observeOpen()
    }

    suspend fun getConflict(conflictId: String): SyncConflict? {
        return conflictDao.getById(conflictId)
    }

    suspend fun resolveConflict(
        conflictId: String,
        resolvedBy: String,
        resolutionJson: String,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val count = conflictDao.resolve(
            id = conflictId,
            by = resolvedBy,
            at = now,
            resolutionJson = resolutionJson
        )
        return count > 0
    }
}
