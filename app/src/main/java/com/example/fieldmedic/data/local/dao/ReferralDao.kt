package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.Referral
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferralDao {

    @Upsert
    suspend fun upsert(referral: Referral)

    @Query("SELECT * FROM referral WHERE encounter_id = :encounterId AND is_deleted = 0")
    fun forEncounter(encounterId: String): Flow<List<Referral>>

    @Query("SELECT * FROM referral WHERE patient_id = :patientId AND is_deleted = 0 ORDER BY issued_at DESC")
    fun observeByPatient(patientId: String): Flow<List<Referral>>
}
