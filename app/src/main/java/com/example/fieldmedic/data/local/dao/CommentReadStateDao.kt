package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.CommentReadState

@Dao
interface CommentReadStateDao {
    @Upsert
    suspend fun upsert(state: CommentReadState)

    @Query("INSERT OR REPLACE INTO comment_read_state (patient_id, last_read_at) VALUES (:patientId, :at)")
    suspend fun markRead(patientId: String, at: Long): Long
}
