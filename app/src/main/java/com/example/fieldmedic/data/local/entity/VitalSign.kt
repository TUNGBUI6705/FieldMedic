package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vital_sign",
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
        Index(value = ["reading_id"]),
        Index(value = ["patient_id", "type", "measured_at"])
    ]
)
data class VitalSign(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "encounter_id")
    val encounterId: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "reading_id")
    val readingId: String? = null,

    @ColumnInfo(name = "type")
    val type: VitalType,

    @ColumnInfo(name = "value")
    val value: Double,

    @ColumnInfo(name = "unit")
    val unit: String,

    @ColumnInfo(name = "source")
    val source: VitalSource,

    @ColumnInfo(name = "device_id")
    val deviceId: String? = null,

    @ColumnInfo(name = "original_value")
    val originalValue: Double? = null,

    @ColumnInfo(name = "is_overridden")
    val isOverridden: Boolean = false,

    @ColumnInfo(name = "context")
    val context: VitalContext? = null,

    @ColumnInfo(name = "plausibility_confirmed")
    val plausibilityConfirmed: Boolean = false,

    @ColumnInfo(name = "abnormal_level")
    val abnormalLevel: AbnormalLevel = AbnormalLevel.NORMAL,

    @ColumnInfo(name = "threshold_set_version")
    val thresholdSetVersion: Int,

    @ColumnInfo(name = "measured_at")
    val measuredAt: Long,

    @Embedded
    val syncMeta: SyncMeta
)
