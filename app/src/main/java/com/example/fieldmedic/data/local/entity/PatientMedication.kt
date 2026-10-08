package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "patient_medication",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["patient_id"])
    ]
)
data class PatientMedication(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "drug_id")
    val drugId: String? = null,

    @ColumnInfo(name = "drug_name")
    val drugName: String,

    @ColumnInfo(name = "dose")
    val dose: String? = null,

    @ColumnInfo(name = "frequency")
    val frequency: String? = null,

    @ColumnInfo(name = "start_date")
    val startDate: String? = null,

    @ColumnInfo(name = "end_date")
    val endDate: String? = null,

    @ColumnInfo(name = "status")
    val status: MedStatus = MedStatus.ACTIVE,

    @ColumnInfo(name = "source")
    val source: MedSource,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @Embedded
    val syncMeta: SyncMeta
)
