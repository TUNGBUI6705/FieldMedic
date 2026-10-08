package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "record_member",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["patient_id", "staff_id"], unique = true)
    ]
)
data class RecordMember(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "staff_id")
    val staffId: String,

    @ColumnInfo(name = "display_name_snapshot")
    val displayNameSnapshot: String,

    @ColumnInfo(name = "role")
    val role: RecordRole,

    @ColumnInfo(name = "source")
    val source: MemberSource,

    @ColumnInfo(name = "invite_id")
    val inviteId: String? = null,

    @ColumnInfo(name = "granted_by")
    val grantedBy: String,

    @ColumnInfo(name = "granted_at")
    val grantedAt: Long,

    @ColumnInfo(name = "expires_at")
    val expiresAt: Long? = null,

    @ColumnInfo(name = "status")
    val status: MemberStatus = MemberStatus.ACTIVE,

    @ColumnInfo(name = "revoked_at")
    val revokedAt: Long? = null,

    @ColumnInfo(name = "revoked_by")
    val revokedBy: String? = null,

    @Embedded
    val syncMeta: SyncMeta
)
