package com.example.fieldmedic.domain.repository

import com.example.fieldmedic.data.local.entity.AllergenCatalog
import com.example.fieldmedic.data.local.entity.DrugCatalog
import com.example.fieldmedic.data.local.entity.ThresholdRule
import com.example.fieldmedic.data.local.entity.ThresholdSet
import com.example.fieldmedic.data.local.entity.VitalType

interface CatalogRepository {
    suspend fun getLatestThresholdSet(): ThresholdSet?
    suspend fun getThresholdRules(setVersion: Int, vitalType: VitalType): List<ThresholdRule>
    suspend fun searchDrugs(query: String): List<DrugCatalog>
    suspend fun searchAllergens(query: String): List<AllergenCatalog>
}
