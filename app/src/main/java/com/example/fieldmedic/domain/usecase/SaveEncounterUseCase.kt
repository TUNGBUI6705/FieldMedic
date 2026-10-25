package com.example.fieldmedic.domain.usecase

import com.example.fieldmedic.data.local.entity.AbnormalLevel
import com.example.fieldmedic.data.local.entity.AlertLevel
import com.example.fieldmedic.data.local.repository.EncounterBundle
import com.example.fieldmedic.data.local.repository.EncounterWriteException
import com.example.fieldmedic.domain.model.AppError
import com.example.fieldmedic.domain.model.AppResult
import com.example.fieldmedic.domain.repository.EncounterRepository
import com.example.fieldmedic.domain.usecase.CheckAllergyConflictUseCase
import com.example.fieldmedic.domain.usecase.CheckVitalThresholdsUseCase

class SaveEncounterUseCase(
    private val encounterRepository: EncounterRepository,
    private val checkVitalThresholdsUseCase: CheckVitalThresholdsUseCase,
    private val checkAllergyConflictUseCase: CheckAllergyConflictUseCase,
) {

    suspend operator fun invoke(bundle: EncounterBundle): AppResult<Unit, AppError> {
        return try {
            var maxAbnormal = AbnormalLevel.NORMAL
            val evaluatedVitals = bundle.vitals.map { vital ->
                val result = checkVitalThresholdsUseCase(
                    vitalType = vital.type,
                    value = vital.value
                )
                val level = result.getOrNull() ?: AbnormalLevel.NORMAL
                if (level == AbnormalLevel.CRITICAL) {
                    maxAbnormal = AbnormalLevel.CRITICAL
                } else if ((level == AbnormalLevel.WARNING) && (maxAbnormal != AbnormalLevel.CRITICAL)) {
                    maxAbnormal = AbnormalLevel.WARNING
                }
                vital.copy(abnormalLevel = level)
            }

            val encounterAlertLevel = when (maxAbnormal) {
                AbnormalLevel.CRITICAL -> AlertLevel.CRITICAL
                AbnormalLevel.WARNING -> AlertLevel.WARNING
                else -> AlertLevel.NONE
            }

            val evaluatedMeds = bundle.medications.map { med ->
                val checkResult = checkAllergyConflictUseCase(
                    patientId = bundle.encounter.patientId,
                    drugAllergyClassCode = null,
                    isOverride = med.allergyOverrideReason != null
                )
                val conflictResult = checkResult.getOrNull()
                if (conflictResult != null) {
                    med.copy(allergyCheck = conflictResult.checkStatus)
                } else {
                    med
                }
            }

            val updatedEncounter = bundle.encounter.copy(
                alertLevel = encounterAlertLevel,
                hasDangerSign = bundle.encounter.hasDangerSign || (encounterAlertLevel == AlertLevel.CRITICAL)
            )

            val updatedBundle = bundle.copy(
                encounter = updatedEncounter,
                vitals = evaluatedVitals,
                medications = evaluatedMeds
            )

            encounterRepository.saveCompletedEncounter(updatedBundle)
            AppResult.Success(Unit)
        } catch (e: EncounterWriteException.AlreadyCompleted) {
            AppResult.Error(AppError.ValidationFailed(e.message ?: "Encounter already completed"))
        } catch (e: EncounterWriteException.InconsistentBundle) {
            AppResult.Error(AppError.ValidationFailed(e.reason))
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e))
        }
    }
}
