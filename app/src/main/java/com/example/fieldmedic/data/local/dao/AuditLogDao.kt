package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.AuditLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Upsert
    suspend fun insert(log: AuditLog)

    @Query("SELECT * FROM audit_log ORDER BY seq DESC LIMIT 1")
    suspend fun lastEntry(): AuditLog?

    @Query("SELECT * FROM audit_log ORDER BY seq ASC")
    suspend fun allOrdered(): List<AuditLog>

    @Query("SELECT * FROM audit_log WHERE patient_id = :patientId ORDER BY seq ASC")
    fun forPatient(patientId: String): Flow<List<AuditLog>>
}
