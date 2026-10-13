package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.VitalSign
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalDao {

    @Upsert
    suspend fun upsert(vital: VitalSign)

    @Upsert
    suspend fun upsertAll(vitals: List<VitalSign>)

    @Query("SELECT * FROM vital_sign WHERE patient_id = :patientId AND is_deleted = 0 AND type IN (:types) ORDER BY measured_at ASC")
    fun observeSeries(patientId: String, types: List<String>): Flow<List<VitalSign>>

    @Query("SELECT * FROM vital_sign WHERE encounter_id = :encounterId AND is_deleted = 0 ORDER BY measured_at ASC")
    fun forEncounter(encounterId: String): Flow<List<VitalSign>>
}
