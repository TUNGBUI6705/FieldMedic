package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "encounter",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["patient_id", "visit_at"]),
        Index(value = ["status"]),
        Index(value = ["alert_level", "visit_at"]),
        Index(value = ["staff_id", "visit_at"])
    ]
)
data class Encounter(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "staff_id")
    val staffId: String,

    @ColumnInfo(name = "station_id")
    val stationId: String,

    @ColumnInfo(name = "status")
    val status: EncounterStatus = EncounterStatus.DRAFT,

    @ColumnInfo(name = "draft_step")
    val draftStep: Int = 1,

    @ColumnInfo(name = "visit_at")
    val visitAt: Long,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "is_quick_exam")
    val isQuickExam: Boolean = false,

    @ColumnInfo(name = "chief_complaint")
    val chiefComplaint: String? = null,

    @ColumnInfo(name = "symptom_note")
    val symptomNote: String? = null,

    @ColumnInfo(name = "diagnosis")
    val diagnosis: String? = null,

    @ColumnInfo(name = "treatment_note")
    val treatmentNote: String? = null,

    @ColumnInfo(name = "onset_at")
    val onsetAt: Long? = null,

    @ColumnInfo(name = "onset_text")
    val onsetText: String? = null,

    @ColumnInfo(name = "symptoms_json")
    val symptomsJson: String = "[]",

    @ColumnInfo(name = "danger_signs_json")
    val dangerSignsJson: String = "[]",

    @ColumnInfo(name = "pregnancy_status")
    val pregnancyStatus: PregnancyStatus = PregnancyStatus.NOT_APPLICABLE,

    @ColumnInfo(name = "gestational_weeks")
    val gestationalWeeks: Int? = null,

    @ColumnInfo(name = "diagnosis_code")
    val diagnosisCode: String? = null,

    @ColumnInfo(name = "referral_needed")
    val referralNeeded: Boolean = false,

    @ColumnInfo(name = "follow_up_date")
    val followUpDate: String? = null,

    @ColumnInfo(name = "alert_level")
    val alertLevel: AlertLevel = AlertLevel.NONE,

    @ColumnInfo(name = "threshold_set_version")
    val thresholdSetVersion: Int? = null,

    @ColumnInfo(name = "has_danger_sign")
    val hasDangerSign: Boolean = false,

    @ColumnInfo(name = "photo_consent")
    val photoConsent: Boolean = false,

    @ColumnInfo(name = "photo_consent_by")
    val photoConsentBy: ConsentBy? = null,

    @ColumnInfo(name = "photo_consent_at")
    val photoConsentAt: Long? = null,

    @ColumnInfo(name = "lat")
    val lat: Double? = null,

    @ColumnInfo(name = "lng")
    val lng: Double? = null,

    @ColumnInfo(name = "device_id")
    val deviceId: String? = null,

    @ColumnInfo(name = "dek_version")
    val dekVersion: Int? = null,

    @Embedded
    val syncMeta: SyncMeta
)
