package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.RecordLock
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordLockDao {
    @Query("SELECT * FROM record_lock WHERE patient_id = :patientId")
    suspend fun get(patientId: String): RecordLock?

    @Query("SELECT * FROM record_lock WHERE patient_id = :patientId")
    fun observe(patientId: String): Flow<RecordLock?>

    @Upsert
    suspend fun upsert(lock: RecordLock)
}
