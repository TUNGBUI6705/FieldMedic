package com.example.fieldmedic.domain.repository

import com.example.fieldmedic.domain.model.AllergyInput
import com.example.fieldmedic.domain.model.AllergyItem
import com.example.fieldmedic.domain.model.AppError
import com.example.fieldmedic.domain.model.AppResult
import com.example.fieldmedic.domain.model.ConditionInput
import com.example.fieldmedic.domain.model.ConditionItem
import com.example.fieldmedic.domain.model.MedicationInput
import com.example.fieldmedic.domain.model.MedicationItem
import kotlinx.coroutines.flow.Flow

interface ClinicalHistoryRepository {
    fun observeAllergies(patientId: String): Flow<List<AllergyItem>>
    fun observeConditions(patientId: String): Flow<List<ConditionItem>>
    fun observeMedications(patientId: String): Flow<List<MedicationItem>>

    suspend fun upsertAllergy(patientId: String, input: AllergyInput): AppResult<Unit, AppError>
    suspend fun upsertCondition(patientId: String, input: ConditionInput): AppResult<Unit, AppError>
    suspend fun upsertMedication(patientId: String, input: MedicationInput): AppResult<Unit, AppError>

    suspend fun softDeleteAllergy(id: String): AppResult<Unit, AppError>
    suspend fun softDeleteCondition(id: String): AppResult<Unit, AppError>
    suspend fun softDeleteMedication(id: String): AppResult<Unit, AppError>
}
