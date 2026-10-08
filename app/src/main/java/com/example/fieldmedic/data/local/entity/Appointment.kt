package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "appointment",
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
        Index(value = ["scheduled_at", "status"])
    ]
)
data class Appointment(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "station_id")
    val stationId: String,

    @ColumnInfo(name = "assignee_staff_id")
    val assigneeStaffId: String? = null,

    @ColumnInfo(name = "scheduled_at")
    val scheduledAt: Long,

    @ColumnInfo(name = "reason")
    val reason: String,

    @ColumnInfo(name = "status")
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,

    @ColumnInfo(name = "encounter_id")
    val encounterId: String? = null,

    @Embedded
    val syncMeta: SyncMeta
)
