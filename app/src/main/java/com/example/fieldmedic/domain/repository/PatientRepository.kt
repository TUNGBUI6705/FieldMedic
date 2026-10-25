package com.example.fieldmedic.domain.repository

import androidx.paging.PagingSource
import com.example.fieldmedic.data.local.entity.Patient
import com.example.fieldmedic.data.local.relation.PatientListItem
import com.example.fieldmedic.domain.model.VillageItem
import kotlinx.coroutines.flow.Flow

interface PatientRepository {
    fun getPatientPaging(query: String, villageId: String?): PagingSource<Int, PatientListItem>
    fun observePatient(id: String): Flow<Patient?>
    fun observeVillages(stationId: String): Flow<List<VillageItem>>
    suspend fun findDuplicates(fullName: String, birthYear: Int, villageId: String?): List<Patient>
    suspend fun getActiveAllergyClassCodes(patientId: String): List<String>
    suspend fun upsertPatient(patient: Patient)
}
