package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_log",
    indices = [
        Index(value = ["seq"], unique = true),
        Index(value = ["patient_id", "created_at"]),
        Index(value = ["sync_status"]),
    ]
)
data class AuditLog(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "seq")
    val seq: Long,

    @ColumnInfo(name = "actor_id")
    val actorId: String,

    @ColumnInfo(name = "action")
    val action: AuditAction,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "entity_id")
    val entityId: String? = null,

    @ColumnInfo(name = "patient_id")
    val patientId: String? = null,

    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @ColumnInfo(name = "app_version")
    val appVersion: String,

    @ColumnInfo(name = "result")
    val result: AuditResult = AuditResult.OK,

    @ColumnInfo(name = "reason")
    val reason: String? = null,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}",

    @ColumnInfo(name = "prev_hash")
    val prevHash: String,

    @ColumnInfo(name = "entry_hash")
    val entryHash: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
)
