package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.RecordComment
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Upsert
    suspend fun upsert(comment: RecordComment)

    @Query("SELECT * FROM record_comment WHERE patient_id = :patientId AND is_deleted = 0 ORDER BY created_at ASC")
    fun observeThread(patientId: String): Flow<List<RecordComment>>

    @Query(
        """
        SELECT COUNT(*) FROM record_comment
        WHERE patient_id = :patientId
          AND is_deleted = 0
          AND author_id != :me
          AND created_at > COALESCE((SELECT last_read_at FROM comment_read_state WHERE patient_id = :patientId), 0)
        """
    )
    fun observeUnread(patientId: String, me: String): Flow<Int>
}
