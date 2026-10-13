package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.RecordLockState

@Dao
interface RecordLockStateDao {
    @Query("SELECT * FROM record_lock_state WHERE patient_id = :patientId")
    suspend fun get(patientId: String): RecordLockState?

    @Upsert
    suspend fun upsert(state: RecordLockState)

    @Query("UPDATE record_lock_state SET failed_attempts = failed_attempts + 1, locked_until = :lockedUntil WHERE patient_id = :patientId")
    suspend fun incrementFailed(patientId: String, lockedUntil: Long?): Int

    @Query("UPDATE record_lock_state SET failed_attempts = 0, locked_until = NULL, last_unlock_at = :at WHERE patient_id = :patientId")
    suspend fun reset(patientId: String, at: Long): Int
}
