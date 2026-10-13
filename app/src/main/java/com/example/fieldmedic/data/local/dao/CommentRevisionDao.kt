package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.RecordCommentRevision
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentRevisionDao {
    @Upsert
    suspend fun insert(revision: RecordCommentRevision)

    @Query("SELECT * FROM record_comment_revision WHERE comment_id = :commentId ORDER BY edited_at DESC")
    fun forComment(commentId: String): Flow<List<RecordCommentRevision>>
}
