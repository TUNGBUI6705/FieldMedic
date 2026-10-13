package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.PatientCondition
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientConditionDao {
    @Upsert
    suspend fun upsert(condition: PatientCondition): Long

    @Upsert
    suspend fun upsertAll(list: List<PatientCondition>): LongArray

    @Query("SELECT * FROM patient_condition WHERE patient_id = :patientId AND status = 'ACTIVE' AND is_deleted = 0")
    fun observeActive(patientId: String): Flow<List<PatientCondition>>
}
