package com.example.fieldmedic.data.local.relation

import com.example.fieldmedic.data.local.entity.AllergyStatus
import com.example.fieldmedic.data.local.entity.Gender
import com.example.fieldmedic.data.local.entity.SyncStatus

data class PatientListItem(
    val id: String,
    val patientCode: String,
    val fullName: String,
    val birthYear: Int,
    val dob: String?,
    val gender: Gender,
    val villageName: String?,
    val lastEncounterAt: Long?,
    val syncStatus: SyncStatus,
    val allergyStatus: AllergyStatus,
    val isProvisional: Boolean,
    val lockEnabled: Boolean,
    val avatarPhotoId: String?
)
