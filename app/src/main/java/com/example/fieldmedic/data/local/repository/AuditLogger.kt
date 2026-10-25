package com.example.fieldmedic.data.local.repository

import androidx.room.withTransaction
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.AuditAction
import com.example.fieldmedic.data.local.entity.AuditLog
import com.example.fieldmedic.data.local.entity.AuditResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

data class ChainVerifyResult(
    val ok: Boolean,
    val brokenAtSeq: Int?
)

class AuditLogger(
    private val dbProvider: () -> AppDatabase,
    private val deviceId: String,
    private val appVersion: String,
    private val clock: () -> Long = System::currentTimeMillis
) {
    // LƯU Ý: Metadata KHÔNG được chứa dữ liệu bệnh nhân (chỉ ghi id, tên hành động).
    suspend fun log(
        actorId: String,
        action: AuditAction,
        entityType: String,
        entityId: String?,
        patientId: String?,
        result: AuditResult = AuditResult.OK,
        reason: String? = null,
        metadata: Map<String, String> = emptyMap()
    ) {
        val db = dbProvider()
        db.withTransaction {
            val dao = db.auditLogDao()
            val last = dao.lastEntry()

            val seq = (last?.seq ?: 0L) + 1L
            val prevHash = last?.entryHash ?: "GENESIS"
            val createdAt = clock()

            val metadataJson = Json.encodeToString(metadata)

            val rawContent = listOf(
                prevHash,
                seq.toString(),
                actorId,
                action.name,
                entityType,
                entityId ?: "",
                patientId ?: "",
                result.name,
                reason ?: "",
                createdAt.toString(),
                metadataJson,
                deviceId,
                appVersion
            ).joinToString("\u001F")

            val entryHash = sha256Hex(rawContent)

            val auditLog = AuditLog(
                id = java.util.UUID.randomUUID().toString(),
                seq = seq,
                actorId = actorId,
                action = action,
                entityType = entityType,
                entityId = entityId,
                patientId = patientId,
                deviceId = deviceId,
                appVersion = appVersion,
                result = result,
                reason = reason,
                metadataJson = metadataJson,
                prevHash = prevHash,
                entryHash = entryHash,
                createdAt = createdAt
            )

            dao.insert(auditLog)
        }
    }

    suspend fun verifyChain(): ChainVerifyResult {
        val db = dbProvider()
        val allLogs = db.auditLogDao().allOrdered()
        var expectedPrevHash = "GENESIS"

        for (log in allLogs) {
            val rawContent = listOf(
                expectedPrevHash,
                log.seq.toString(),
                log.actorId,
                log.action.name,
                log.entityType,
                log.entityId ?: "",
                log.patientId ?: "",
                log.result.name,
                log.reason ?: "",
                log.createdAt.toString(),
                log.metadataJson,
                log.deviceId,
                log.appVersion
            ).joinToString("\u001F")

            val calculatedHash = sha256Hex(rawContent)
            if (calculatedHash != log.entryHash) {
                return ChainVerifyResult(false, log.seq.toInt())
            }
            expectedPrevHash = log.entryHash
        }

        return ChainVerifyResult(true, null)
    }

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.byteInputStream().readBytes())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
