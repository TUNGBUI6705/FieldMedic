package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "record_invite",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["patient_id"]),
        Index(value = ["status"])
    ]
)
data class RecordInvite(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "role")
    val role: RecordRole,

    @ColumnInfo(name = "code_hint")
    val codeHint: String,

    @ColumnInfo(name = "purpose")
    val purpose: String? = null,

    @ColumnInfo(name = "consent_confirmed")
    val consentConfirmed: Boolean,

    @ColumnInfo(name = "consent_by")
    val consentBy: ConsentBy,

    @ColumnInfo(name = "expires_at")
    val expiresAt: Long,

    @ColumnInfo(name = "max_uses")
    val maxUses: Int = 1,

    @ColumnInfo(name = "used_count")
    val usedCount: Int = 0,

    @ColumnInfo(name = "status")
    val status: InviteStatus = InviteStatus.ACTIVE,

    @ColumnInfo(name = "created_by")
    val createdBy: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "revoked_at")
    val revokedAt: Long? = null,

    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long
)
