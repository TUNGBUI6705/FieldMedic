package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.PatientAllergy
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientAllergyDao {
    @Upsert
    suspend fun upsert(allergy: PatientAllergy): Long

    @Upsert
    suspend fun upsertAll(list: List<PatientAllergy>): LongArray

    @Query("SELECT * FROM patient_allergy WHERE patient_id = :patientId AND status = 'ACTIVE' AND is_deleted = 0")
    fun observeActive(patientId: String): Flow<List<PatientAllergy>>

    @Query("SELECT DISTINCT allergy_class_code FROM patient_allergy WHERE patient_id = :patientId AND status = 'ACTIVE' AND is_deleted = 0 AND allergy_class_code IS NOT NULL")
    suspend fun activeClassCodes(patientId: String): List<String>
}
