package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.PairedDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface PairedDeviceDao {
    @Upsert
    suspend fun upsert(device: PairedDevice)

    @Upsert
    suspend fun upsertAll(list: List<PairedDevice>)

    @Query("SELECT * FROM paired_device")
    fun observeAll(): Flow<List<PairedDevice>>

    @Query("SELECT * FROM paired_device WHERE mac_address = :mac")
    suspend fun getByMac(mac: String): PairedDevice?

    @Query("DELETE FROM paired_device WHERE id = :id")
    suspend fun delete(id: String): Int
}
