package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.SyncEntityType
import com.example.fieldmedic.data.local.entity.SyncErrorCategory
import com.example.fieldmedic.data.local.entity.SyncQueue
import com.example.fieldmedic.data.local.view.SyncSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    @Query("SELECT * FROM v_sync_summary")
    fun observeSummary(): Flow<SyncSummary>

    @Query(
        """
        SELECT * FROM sync_queue
        WHERE status = 'PENDING' AND next_attempt_at <= :now
          AND (depends_on IS NULL OR NOT EXISTS (SELECT 1 FROM sync_queue q2 WHERE q2.entity_id = sync_queue.depends_on))
        ORDER BY priority ASC, created_at ASC
        LIMIT :limit
        """
    )
    suspend fun nextBatch(now: Long, limit: Int): List<SyncQueue>

    @Upsert
    suspend fun upsert(item: SyncQueue)

    @Query("UPDATE sync_queue SET status = 'IN_FLIGHT' WHERE id = :id")
    suspend fun markInFlight(id: String): Int

    @Query(
        """
        UPDATE sync_queue
        SET status = 'ERROR',
            error_category = :category,
            last_error = :message,
            next_attempt_at = :nextAttemptAt,
            attempts = :attempts
        WHERE id = :id
        """
    )
    suspend fun markError(id: String, category: SyncErrorCategory, message: String, nextAttemptAt: Long, attempts: Int): Int

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT * FROM sync_queue WHERE entity_type = :entityType AND entity_id = :entityId LIMIT 1")
    suspend fun findPending(entityType: SyncEntityType, entityId: String): SyncQueue?

    @Query("SELECT * FROM sync_queue WHERE (:status IS NULL OR status = :status) ORDER BY created_at ASC")
    fun observeFiltered(status: String?): Flow<List<SyncQueue>>
}
