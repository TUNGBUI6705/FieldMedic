package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_conflict",
    indices = [
        Index(value = ["status"]),
        Index(value = ["entity_type", "entity_id"]),
        Index(value = ["patient_id"]),
    ]
)
data class SyncConflict(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "entity_type")
    val entityType: SyncEntityType,

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String? = null,

    @ColumnInfo(name = "field_diffs_json")
    val fieldDiffsJson: String,

    @ColumnInfo(name = "remote_version")
    val remoteVersion: Int,

    @ColumnInfo(name = "remote_updated_by")
    val remoteUpdatedBy: String? = null,

    @ColumnInfo(name = "remote_updated_at")
    val remoteUpdatedAt: Long? = null,

    @ColumnInfo(name = "required_role")
    val requiredRole: StaffRole = StaffRole.NURSE,

    @ColumnInfo(name = "status")
    val status: ConflictStatus = ConflictStatus.OPEN,

    @ColumnInfo(name = "detected_at")
    val detectedAt: Long,

    @ColumnInfo(name = "resolved_by")
    val resolvedBy: String? = null,

    @ColumnInfo(name = "resolved_at")
    val resolvedAt: Long? = null,

    @ColumnInfo(name = "resolution_json")
    val resolutionJson: String? = null,
)
