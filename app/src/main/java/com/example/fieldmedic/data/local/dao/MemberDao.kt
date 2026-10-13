package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.RecordMember
import com.example.fieldmedic.data.local.entity.RecordRole
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Upsert
    suspend fun upsertAll(list: List<RecordMember>)

    @Query("SELECT * FROM record_member WHERE patient_id = :patientId AND is_deleted = 0")
    fun observeByPatient(patientId: String): Flow<List<RecordMember>>

    @Query(
        """
        SELECT role FROM record_member
        WHERE patient_id = :patientId
          AND staff_id = :staffId
          AND status = 'ACTIVE'
          AND is_deleted = 0
          AND (expires_at IS NULL OR expires_at > :now)
        LIMIT 1
        """
    )
    suspend fun activeRole(patientId: String, staffId: String, now: Long): RecordRole?
}
