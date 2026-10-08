package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "referral",
    foreignKeys = [
        ForeignKey(
            entity = Encounter::class,
            parentColumns = ["id"],
            childColumns = ["encounter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["encounter_id"]),
        Index(value = ["patient_id"])
    ]
)
data class Referral(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "encounter_id")
    val encounterId: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "urgency")
    val urgency: ReferralUrgency,

    @ColumnInfo(name = "destination_name")
    val destinationName: String,

    @ColumnInfo(name = "reason")
    val reason: String? = null,

    @ColumnInfo(name = "status")
    val status: ReferralStatus = ReferralStatus.ISSUED,

    @ColumnInfo(name = "issued_at")
    val issuedAt: Long,

    @ColumnInfo(name = "issued_by")
    val issuedBy: String,

    @ColumnInfo(name = "slip_snapshot_json")
    val slipSnapshotJson: String? = null,

    @ColumnInfo(name = "dek_version")
    val dekVersion: Int? = null,

    @Embedded
    val syncMeta: SyncMeta
)
