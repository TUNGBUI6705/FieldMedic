package com.example.fieldmedic.domain.usecase

import com.example.fieldmedic.data.local.entity.AbnormalLevel
import com.example.fieldmedic.data.local.entity.Population
import com.example.fieldmedic.data.local.entity.ThresholdRule
import com.example.fieldmedic.data.local.entity.VitalType
import com.example.fieldmedic.domain.model.AppError
import com.example.fieldmedic.domain.model.AppResult
import com.example.fieldmedic.domain.repository.CatalogRepository

class CheckVitalThresholdsUseCase(
    private val catalogRepository: CatalogRepository
) {

    suspend operator fun invoke(
        vitalType: VitalType,
        value: Double,
        population: Population = Population.ADULT,
        ageDays: Int? = null
    ): AppResult<AbnormalLevel, AppError> {
        return try {
            val latestSet = catalogRepository.getLatestThresholdSet()
                ?: return AppResult.Success(AbnormalLevel.NORMAL)

            val rules = catalogRepository.getThresholdRules(latestSet.version, vitalType)
                .filter { it.population == population }
                .filter { rule ->
                    val minAgeOk = rule.ageMinDays == null || (ageDays != null && ageDays >= rule.ageMinDays)
                    val maxAgeOk = rule.ageMaxDays == null || (ageDays != null && ageDays <= rule.ageMaxDays)
                    minAgeOk && maxAgeOk
                }

            var highestLevel = AbnormalLevel.NORMAL

            for (rule in rules) {
                if (isViolation(value, rule)) {
                    if (rule.level == AbnormalLevel.CRITICAL) {
                        highestLevel = AbnormalLevel.CRITICAL
                        break
                    } else if (rule.level == AbnormalLevel.WARNING) {
                        highestLevel = AbnormalLevel.WARNING
                    }
                }
            }

            AppResult.Success(highestLevel)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e))
        }
    }

    private fun isViolation(value: Double, rule: ThresholdRule): Boolean {
        if (rule.minValue != null) {
            val minViolated = if (rule.minInclusive) value < rule.minValue else value <= rule.minValue
            if (minViolated) return true
        }
        if (rule.maxValue != null) {
            val maxViolated = if (rule.maxInclusive) value > rule.maxValue else value >= rule.maxValue
            if (maxViolated) return true
        }
        return false
    }
}
