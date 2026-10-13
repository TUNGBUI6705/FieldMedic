package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.RecordInvite
import kotlinx.coroutines.flow.Flow

@Dao
interface InviteDao {
    @Query("SELECT * FROM record_invite WHERE patient_id = :patientId ORDER BY created_at DESC")
    fun observeByPatient(patientId: String): Flow<List<RecordInvite>>

    @Upsert
    suspend fun upsertAll(list: List<RecordInvite>)

    @Query("DELETE FROM record_invite WHERE patient_id = :patientId")
    suspend fun deleteByPatient(patientId: String): Int

    @Transaction
    suspend fun replaceForPatient(patientId: String, list: List<RecordInvite>) {
        deleteByPatient(patientId)
        upsertAll(list)
    }
}
