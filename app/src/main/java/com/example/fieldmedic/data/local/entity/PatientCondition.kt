package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "patient_condition",
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
data class PatientCondition(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "condition_code")
    val conditionCode: String? = null,

    @ColumnInfo(name = "condition_name")
    val conditionName: String,

    @ColumnInfo(name = "status")
    val status: ConditionStatus = ConditionStatus.ACTIVE,

    @ColumnInfo(name = "diagnosed_year")
    val diagnosedYear: Int? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @Embedded
    val syncMeta: SyncMeta
)
