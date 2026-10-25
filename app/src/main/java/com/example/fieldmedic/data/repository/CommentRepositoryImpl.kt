package com.example.fieldmedic.data.repository

import androidx.room.withTransaction
import com.example.fieldmedic.data.local.auth.SessionManager
import com.example.fieldmedic.data.local.dao.CommentDao
import com.example.fieldmedic.data.local.dao.CommentReadStateDao
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.AuditAction
import com.example.fieldmedic.data.local.entity.AuditResult
import com.example.fieldmedic.data.local.entity.CommentTarget
import com.example.fieldmedic.data.local.entity.CommentType
import com.example.fieldmedic.data.local.entity.QueueOperation
import com.example.fieldmedic.data.local.entity.RecordComment
import com.example.fieldmedic.data.local.entity.SyncEntityType
import com.example.fieldmedic.data.local.entity.SyncMeta
import com.example.fieldmedic.data.local.entity.SyncStatus
import com.example.fieldmedic.data.local.repository.AuditLogger
import com.example.fieldmedic.data.local.repository.SyncEnqueuer
import com.example.fieldmedic.domain.model.AppError
import com.example.fieldmedic.domain.model.AppResult
import com.example.fieldmedic.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CommentRepositoryImpl(
    private val dbProvider: () -> AppDatabase,
    private val commentDao: CommentDao,
    private val readStateDao: CommentReadStateDao,
    private val syncEnqueuer: SyncEnqueuer,
    private val auditLogger: AuditLogger,
    private val sessionManager: SessionManager
) : CommentRepository {

    override fun observeThread(patientId: String): Flow<List<RecordComment>> {
        return commentDao.observeThread(patientId)
    }

    override fun observeUnreadCount(patientId: String): Flow<Int> {
        val currentStaffId = sessionManager.currentStaff.value?.id ?: ""
        return commentDao.observeUnread(patientId, currentStaffId)
    }

    override suspend fun addComment(patientId: String, text: String): AppResult<Unit, AppError> {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > 2000) {
            return AppResult.Error(AppError.ValidationFailed("Comment text must be between 1 and 2000 characters"))
        }

        val currentStaff = sessionManager.currentStaff.value
            ?: return AppResult.Error(AppError.Unauthorized("Not logged in"))

        return try {
            val db = dbProvider()
            db.withTransaction {
                val now = System.currentTimeMillis()
                val commentId = UUID.randomUUID().toString()

                val comment = RecordComment(
                    id = commentId,
                    patientId = patientId,
                    targetType = CommentTarget.RECORD,
                    authorId = currentStaff.id,
                    authorNameSnapshot = currentStaff.fullName,
                    authorRoleSnapshot = currentStaff.role.name,
                    type = CommentType.NOTE,
                    body = trimmed,
                    syncMeta = SyncMeta(createdAt = now, updatedAt = now, syncStatus = SyncStatus.PENDING)
                )

                commentDao.upsert(comment)

                syncEnqueuer.enqueue(
                    entityType = SyncEntityType.RECORD_COMMENT,
                    entityId = commentId,
                    patientId = patientId,
                    operation = QueueOperation.CREATE,
                    changedFieldsJson = "{}",
                    baseValuesJson = "{}",
                    baseVersion = 0,
                    priority = SyncEnqueuer.PATIENT,
                    dependsOn = null,
                    now = now
                )

                auditLogger.log(
                    actorId = currentStaff.id,
                    action = AuditAction.COMMENT_ADD,
                    entityType = "RECORD_COMMENT",
                    entityId = commentId,
                    patientId = patientId,
                    result = AuditResult.OK,
                    reason = "Added record comment"
                )
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e))
        }
    }

    override suspend fun markThreadRead(patientId: String): AppResult<Unit, AppError> {
        return try {
            val now = System.currentTimeMillis()
            readStateDao.markRead(patientId, now)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e))
        }
    }
}
