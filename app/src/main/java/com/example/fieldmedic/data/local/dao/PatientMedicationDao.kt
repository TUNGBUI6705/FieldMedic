package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.PatientMedication
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientMedicationDao {
    @Upsert
    suspend fun upsert(medication: PatientMedication): Long

    @Upsert
    suspend fun upsertAll(list: List<PatientMedication>): LongArray

    @Query("SELECT * FROM patient_medication WHERE patient_id = :patientId AND status = 'ACTIVE' AND is_deleted = 0")
    fun observeActive(patientId: String): Flow<List<PatientMedication>>
}
