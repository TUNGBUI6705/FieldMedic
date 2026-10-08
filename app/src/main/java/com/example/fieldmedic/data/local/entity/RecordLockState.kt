package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "record_lock_state",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RecordLockState(
    @PrimaryKey
    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "failed_attempts")
    val failedAttempts: Int = 0,

    @ColumnInfo(name = "locked_until")
    val lockedUntil: Long? = null,

    @ColumnInfo(name = "last_unlock_at")
    val lastUnlockAt: Long? = null
)
