package com.example.fieldmedic.domain.repository

import com.example.fieldmedic.data.local.entity.RecordComment
import com.example.fieldmedic.domain.model.AppError
import com.example.fieldmedic.domain.model.AppResult
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    fun observeThread(patientId: String): Flow<List<RecordComment>>
    fun observeUnreadCount(patientId: String): Flow<Int>
    suspend fun addComment(patientId: String, text: String): AppResult<Unit, AppError>
    suspend fun markThreadRead(patientId: String): AppResult<Unit, AppError>
}
