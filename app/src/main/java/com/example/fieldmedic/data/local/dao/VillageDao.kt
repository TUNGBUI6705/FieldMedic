package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.Village
import kotlinx.coroutines.flow.Flow

@Dao
interface VillageDao {
    @Upsert
    suspend fun upsertAll(list: List<Village>): LongArray

    @Query("SELECT * FROM village WHERE station_id = :stationId AND is_active = 1 ORDER BY name ASC")
    fun observeActiveByStation(stationId: String): Flow<List<Village>>
}
