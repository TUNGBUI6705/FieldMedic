package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "patient_allergy",
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
        Index(value = ["patient_id", "allergy_class_code"])
    ]
)
data class PatientAllergy(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "allergen_code")
    val allergenCode: String? = null,

    @ColumnInfo(name = "allergen_name")
    val allergenName: String,

    @ColumnInfo(name = "allergy_class_code")
    val allergyClassCode: String? = null,

    @ColumnInfo(name = "reaction")
    val reaction: String? = null,

    @ColumnInfo(name = "severity")
    val severity: AllergySeverity,

    @ColumnInfo(name = "status")
    val status: AllergyRecordStatus = AllergyRecordStatus.ACTIVE,

    @ColumnInfo(name = "recorded_by")
    val recordedBy: String,

    @ColumnInfo(name = "recorded_at")
    val recordedAt: Long,

    @Embedded
    val syncMeta: SyncMeta
)
