package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.SyncConflict
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncConflictDao {
    @Upsert
    suspend fun upsert(conflict: SyncConflict)

    @Query("SELECT * FROM sync_conflict WHERE status IN ('OPEN', 'ESCALATED') ORDER BY detected_at DESC")
    fun observeOpen(): Flow<List<SyncConflict>>

    @Query("SELECT * FROM sync_conflict WHERE id = :id")
    suspend fun getById(id: String): SyncConflict?

    @Query(
        """
        UPDATE sync_conflict
        SET status = 'RESOLVED',
            resolved_by = :by,
            resolved_at = :at,
            resolution_json = :resolutionJson
        WHERE id = :id
        """
    )
    suspend fun resolve(id: String, by: String, at: Long, resolutionJson: String): Int
}
