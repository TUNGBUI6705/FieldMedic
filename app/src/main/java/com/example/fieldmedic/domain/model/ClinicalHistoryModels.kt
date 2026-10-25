package com.example.fieldmedic.domain.model

import com.example.fieldmedic.data.local.entity.AllergyRecordStatus
import com.example.fieldmedic.data.local.entity.AllergySeverity
import com.example.fieldmedic.data.local.entity.ConditionStatus
import com.example.fieldmedic.data.local.entity.MedSource
import com.example.fieldmedic.data.local.entity.MedStatus

data class AllergyItem(
    val id: String,
    val patientId: String,
    val allergenCode: String?,
    val allergenName: String,
    val allergyClassCode: String?,
    val reaction: String?,
    val severity: AllergySeverity,
    val status: AllergyRecordStatus,
    val recordedBy: String,
    val recordedAt: Long
)

data class AllergyInput(
    val id: String? = null,
    val allergenCode: String? = null,
    val allergenName: String,
    val allergyClassCode: String? = null,
    val reaction: String? = null,
    val severity: AllergySeverity,
    val status: AllergyRecordStatus = AllergyRecordStatus.ACTIVE
)

data class ConditionItem(
    val id: String,
    val patientId: String,
    val conditionCode: String?,
    val conditionName: String,
    val status: ConditionStatus,
    val diagnosedYear: Int?,
    val note: String?
)

data class ConditionInput(
    val id: String? = null,
    val conditionCode: String? = null,
    val conditionName: String,
    val status: ConditionStatus = ConditionStatus.ACTIVE,
    val diagnosedYear: Int? = null,
    val note: String? = null
)

data class MedicationItem(
    val id: String,
    val patientId: String,
    val drugId: String?,
    val drugName: String,
    val dose: String?,
    val frequency: String?,
    val startDate: String?,
    val endDate: String?,
    val status: MedStatus,
    val source: MedSource,
    val note: String?
)

data class MedicationInput(
    val id: String? = null,
    val drugId: String? = null,
    val drugName: String,
    val dose: String? = null,
    val frequency: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: MedStatus = MedStatus.ACTIVE,
    val source: MedSource = MedSource.REPORTED,
    val note: String? = null
)
