package com.example.fieldmedic.data.repository

import com.example.fieldmedic.data.local.dao.AllergenDao
import com.example.fieldmedic.data.local.dao.DrugDao
import com.example.fieldmedic.data.local.dao.ThresholdDao
import com.example.fieldmedic.data.local.entity.AllergenCatalog
import com.example.fieldmedic.data.local.entity.DrugCatalog
import com.example.fieldmedic.data.local.entity.ThresholdRule
import com.example.fieldmedic.data.local.entity.ThresholdSet
import com.example.fieldmedic.data.local.entity.VitalType
import com.example.fieldmedic.domain.repository.CatalogRepository

class CatalogRepositoryImpl(
    private val thresholdDao: ThresholdDao,
    private val drugDao: DrugDao,
    private val allergenDao: AllergenDao
) : CatalogRepository {

    override suspend fun getLatestThresholdSet(): ThresholdSet? {
        return thresholdDao.latestSet()
    }

    override suspend fun getThresholdRules(setVersion: Int, vitalType: VitalType): List<ThresholdRule> {
        return thresholdDao.rules(setVersion, vitalType.name)
    }

    override suspend fun searchDrugs(query: String): List<DrugCatalog> {
        return drugDao.search(query)
    }

    override suspend fun searchAllergens(query: String): List<AllergenCatalog> {
        return allergenDao.search(query)
    }
}
