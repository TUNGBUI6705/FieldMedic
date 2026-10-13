package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.MedicationGiven
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationGivenDao {

    @Upsert
    suspend fun upsert(medication: MedicationGiven)

    @Upsert
    suspend fun upsertAll(medications: List<MedicationGiven>)

    @Query("SELECT * FROM medication_given WHERE encounter_id = :encounterId AND is_deleted = 0")
    fun forEncounter(encounterId: String): Flow<List<MedicationGiven>>
}
