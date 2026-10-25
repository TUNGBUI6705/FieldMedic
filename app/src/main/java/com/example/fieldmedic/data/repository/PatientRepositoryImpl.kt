package com.example.fieldmedic.data.repository

import androidx.paging.PagingSource
import com.example.fieldmedic.data.local.dao.PatientAllergyDao
import com.example.fieldmedic.data.local.dao.PatientDao
import com.example.fieldmedic.data.local.dao.VillageDao
import com.example.fieldmedic.data.local.entity.Patient
import com.example.fieldmedic.data.local.relation.PatientListItem
import com.example.fieldmedic.domain.model.VillageItem
import com.example.fieldmedic.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PatientRepositoryImpl(
    private val patientDao: PatientDao,
    private val allergyDao: PatientAllergyDao,
    private val villageDao: VillageDao
) : PatientRepository {

    override fun getPatientPaging(query: String, villageId: String?): PagingSource<Int, PatientListItem> {
        return patientDao.pagingList(query, villageId)
    }

    override fun observePatient(id: String): Flow<Patient?> {
        return patientDao.observe(id)
    }

    override fun observeVillages(stationId: String): Flow<List<VillageItem>> {
        return villageDao.observeActiveByStation(stationId).map { villages ->
            villages.map { VillageItem(id = it.id, name = it.name) }
        }
    }

    override suspend fun findDuplicates(fullName: String, birthYear: Int, villageId: String?): List<Patient> {
        return patientDao.findDuplicates(fullName, birthYear, villageId)
    }

    override suspend fun getActiveAllergyClassCodes(patientId: String): List<String> {
        return allergyDao.activeClassCodes(patientId)
    }

    override suspend fun upsertPatient(patient: Patient) {
        patientDao.upsert(patient)
    }
}
