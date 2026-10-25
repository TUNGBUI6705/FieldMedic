package com.example.fieldmedic.domain.usecase

import androidx.paging.PagingSource
import com.example.fieldmedic.data.local.entity.AbnormalLevel
import com.example.fieldmedic.data.local.entity.AllergenCatalog
import com.example.fieldmedic.data.local.entity.AllergyCheck
import com.example.fieldmedic.data.local.entity.DrugCatalog
import com.example.fieldmedic.data.local.entity.Patient
import com.example.fieldmedic.data.local.entity.Population
import com.example.fieldmedic.data.local.entity.ThresholdRule
import com.example.fieldmedic.data.local.entity.ThresholdSet
import com.example.fieldmedic.data.local.entity.VitalType
import com.example.fieldmedic.data.local.relation.PatientListItem
import com.example.fieldmedic.domain.model.AppResult
import com.example.fieldmedic.domain.repository.CatalogRepository
import com.example.fieldmedic.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCaseUnitTest {

    private class MockCatalogRepository : CatalogRepository {
        override suspend fun getLatestThresholdSet(): ThresholdSet {
            return ThresholdSet(version = 1, effectiveFrom = 1000L, sourceNote = "Test")
        }
        override suspend fun getThresholdRules(setVersion: Int, vitalType: VitalType): List<ThresholdRule> {
            return listOf(
                ThresholdRule(
                    id = "r1",
                    setVersion = 1,
                    vitalType = VitalType.TEMP,
                    population = Population.ADULT,
                    level = AbnormalLevel.WARNING,
                    minValue = null,
                    maxValue = 38.0,
                    labelVi = "Sốt"
                ),
                ThresholdRule(
                    id = "r2",
                    setVersion = 1,
                    vitalType = VitalType.TEMP,
                    population = Population.ADULT,
                    level = AbnormalLevel.CRITICAL,
                    minValue = null,
                    maxValue = 40.0,
                    labelVi = "Sốt cao nguy hiểm"
                )
            )
        }
        override suspend fun searchDrugs(query: String): List<DrugCatalog> = emptyList()
        override suspend fun searchAllergens(query: String): List<AllergenCatalog> = emptyList()
    }

    private class MockPatientRepository : PatientRepository {
        override fun getPatientPaging(query: String, villageId: String?): PagingSource<Int, PatientListItem> {
            throw NotImplementedError()
        }
        override fun observePatient(id: String): Flow<Patient?> {
            throw NotImplementedError()
        }
        override fun observeVillages(stationId: String): Flow<List<com.example.fieldmedic.domain.model.VillageItem>> {
            throw NotImplementedError()
        }
        override suspend fun findDuplicates(fullName: String, birthYear: Int, villageId: String?): List<Patient> = emptyList()
        override suspend fun getActiveAllergyClassCodes(patientId: String): List<String> {
            return listOf("PEN", "NSAID")
        }
        override suspend fun upsertPatient(patient: Patient) {}
    }

    @Test
    fun testCheckVitalThresholds_levels() = runBlocking {
        val useCase = CheckVitalThresholdsUseCase(MockCatalogRepository())

        // 36.8 -> NORMAL
        val resNormal = useCase(vitalType = VitalType.TEMP, value = 36.8)
        assertTrue(resNormal is AppResult.Success)
        assertEquals(AbnormalLevel.NORMAL, (resNormal as AppResult.Success).data)

        // 38.5 -> WARNING
        val resWarn = useCase(vitalType = VitalType.TEMP, value = 38.5)
        assertTrue(resWarn is AppResult.Success)
        assertEquals(AbnormalLevel.WARNING, (resWarn as AppResult.Success).data)

        // 40.5 -> CRITICAL
        val resCrit = useCase(vitalType = VitalType.TEMP, value = 40.5)
        assertTrue(resCrit is AppResult.Success)
        assertEquals(AbnormalLevel.CRITICAL, (resCrit as AppResult.Success).data)
    }

    @Test
    fun testCheckAllergyConflict() = runBlocking {
        val useCase = CheckAllergyConflictUseCase(MockPatientRepository())

        // Non-conflicting
        val resNoConflict = useCase(patientId = "p1", drugAllergyClassCode = "CEPH")
        assertTrue(resNoConflict is AppResult.Success)
        val dataNoConflict = (resNoConflict as AppResult.Success).data
        assertFalse(dataNoConflict.hasConflict)
        assertEquals(AllergyCheck.PASSED, dataNoConflict.checkStatus)

        // Conflicting without override
        val resConflict = useCase(patientId = "p1", drugAllergyClassCode = "PEN", isOverride = false)
        assertTrue(resConflict is AppResult.Success)
        val dataConflict = (resConflict as AppResult.Success).data
        assertTrue(dataConflict.hasConflict)
        assertEquals(AllergyCheck.NOT_CHECKED, dataConflict.checkStatus)

        // Conflicting with override
        val resOverride = useCase(patientId = "p1", drugAllergyClassCode = "PEN", isOverride = true)
        assertTrue(resOverride is AppResult.Success)
        val dataOverride = (resOverride as AppResult.Success).data
        assertTrue(dataOverride.hasConflict)
        assertEquals(AllergyCheck.OVERRIDDEN, dataOverride.checkStatus)
    }
}
