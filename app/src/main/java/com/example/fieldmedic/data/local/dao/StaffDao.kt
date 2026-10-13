package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.Staff
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Upsert
    suspend fun upsertAll(list: List<Staff>): LongArray

    @Query("SELECT * FROM staff WHERE id = :id")
    fun observe(id: String): Flow<Staff?>

    @Query("SELECT * FROM staff WHERE station_id = :stationId AND is_active = 1")
    fun observeByStation(stationId: String): Flow<List<Staff>>
}
