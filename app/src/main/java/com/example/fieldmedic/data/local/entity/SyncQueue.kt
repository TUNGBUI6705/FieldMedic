package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["status", "priority", "created_at"]),
        Index(value = ["entity_type", "entity_id"]),
        Index(value = ["depends_on"]),
    ]
)
data class SyncQueue(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "entity_type")
    val entityType: SyncEntityType,

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String? = null,

    @ColumnInfo(name = "operation")
    val operation: QueueOperation,

    @ColumnInfo(name = "payload")
    val payload: String,

    @ColumnInfo(name = "base_values")
    val baseValues: String,

    @ColumnInfo(name = "base_version")
    val baseVersion: Int,

    @ColumnInfo(name = "status")
    val status: QueueStatus = QueueStatus.PENDING,

    @ColumnInfo(name = "priority")
    val priority: Int,

    @ColumnInfo(name = "depends_on")
    val dependsOn: String? = null,

    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String,

    @ColumnInfo(name = "attempts")
    val attempts: Int = 0,

    @ColumnInfo(name = "next_attempt_at")
    val nextAttemptAt: Long = 0L,

    @ColumnInfo(name = "last_error")
    val lastError: String? = null,

    @ColumnInfo(name = "error_category")
    val errorCategory: SyncErrorCategory? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
