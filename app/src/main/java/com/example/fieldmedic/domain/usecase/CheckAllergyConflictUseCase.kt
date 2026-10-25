package com.example.fieldmedic.domain.usecase

import com.example.fieldmedic.data.local.entity.AllergyCheck
import com.example.fieldmedic.domain.model.AppError
import com.example.fieldmedic.domain.model.AppResult
import com.example.fieldmedic.domain.repository.PatientRepository

data class AllergyConflictResult(
    val hasConflict: Boolean,
    val conflictingClassCode: String?,
    val checkStatus: AllergyCheck,
)

class CheckAllergyConflictUseCase(
    private val patientRepository: PatientRepository
) {

    suspend operator fun invoke(
        patientId: String,
        drugAllergyClassCode: String?,
        isOverride: Boolean = false
    ): AppResult<AllergyConflictResult, AppError> {
        return try {
            if (drugAllergyClassCode.isNullOrBlank()) {
                return AppResult.Success(
                    AllergyConflictResult(
                        hasConflict = false,
                        conflictingClassCode = null,
                        checkStatus = AllergyCheck.PASSED
                    )
                )
            }

            val activeClassCodes = patientRepository.getActiveAllergyClassCodes(patientId)
            val hasConflict = activeClassCodes.contains(drugAllergyClassCode)

            val checkStatus = when {
                !hasConflict -> AllergyCheck.PASSED
                isOverride -> AllergyCheck.OVERRIDDEN
                else -> AllergyCheck.NOT_CHECKED
            }

            AppResult.Success(
                AllergyConflictResult(
                    hasConflict = hasConflict,
                    conflictingClassCode = if (hasConflict) drugAllergyClassCode else null,
                    checkStatus = checkStatus
                )
            )
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e))
        }
    }
}
