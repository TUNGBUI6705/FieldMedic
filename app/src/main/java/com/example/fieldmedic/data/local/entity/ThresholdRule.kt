package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "threshold_rule",
    foreignKeys = [
        ForeignKey(
            entity = ThresholdSet::class,
            parentColumns = ["version"],
            childColumns = ["set_version"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["set_version", "vital_type", "population"])
    ]
)
data class ThresholdRule(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "set_version")
    val setVersion: Int,

    @ColumnInfo(name = "vital_type")
    val vitalType: VitalType,

    @ColumnInfo(name = "population")
    val population: Population,

    @ColumnInfo(name = "age_min_days")
    val ageMinDays: Int? = null,

    @ColumnInfo(name = "age_max_days")
    val ageMaxDays: Int? = null,

    @ColumnInfo(name = "level")
    val level: AbnormalLevel,

    @ColumnInfo(name = "min_value")
    val minValue: Double? = null,

    @ColumnInfo(name = "max_value")
    val maxValue: Double? = null,

    @ColumnInfo(name = "min_inclusive")
    val minInclusive: Boolean = true,

    @ColumnInfo(name = "max_inclusive")
    val maxInclusive: Boolean = true,

    @ColumnInfo(name = "label_vi")
    val labelVi: String
)
