package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_given",
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
data class MedicationGiven(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "encounter_id")
    val encounterId: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "drug_id")
    val drugId: String? = null,

    @ColumnInfo(name = "drug_name")
    val drugName: String,

    @ColumnInfo(name = "dose")
    val dose: String,

    @ColumnInfo(name = "frequency")
    val frequency: String,

    @ColumnInfo(name = "days")
    val days: Int,

    @ColumnInfo(name = "quantity")
    val quantity: String? = null,

    @ColumnInfo(name = "instruction")
    val instruction: String? = null,

    @ColumnInfo(name = "allergy_check")
    val allergyCheck: AllergyCheck = AllergyCheck.NOT_CHECKED,

    @ColumnInfo(name = "allergy_override_reason")
    val allergyOverrideReason: String? = null,

    @ColumnInfo(name = "pregnancy_check")
    val pregnancyCheck: PregnancyCheck = PregnancyCheck.NOT_APPLICABLE,

    @ColumnInfo(name = "duplicate_ack")
    val duplicateAck: Boolean = false,

    @Embedded
    val syncMeta: SyncMeta
)
