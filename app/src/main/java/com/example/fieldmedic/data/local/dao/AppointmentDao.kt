package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.Appointment
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {

    @Upsert
    suspend fun upsert(appointment: Appointment)

    @Upsert
    suspend fun upsertAll(appointments: List<Appointment>)

    @Query("SELECT * FROM appointment WHERE is_deleted = 0 AND status = 'SCHEDULED' AND scheduled_at BETWEEN :start AND :end ORDER BY scheduled_at ASC")
    fun observeBetween(start: Long, end: Long): Flow<List<Appointment>>

    @Query("SELECT COUNT(*) FROM appointment WHERE is_deleted = 0 AND status = 'SCHEDULED' AND scheduled_at BETWEEN :now AND :endOfDay")
    fun countRemaining(now: Long, endOfDay: Long): Flow<Int>
}
