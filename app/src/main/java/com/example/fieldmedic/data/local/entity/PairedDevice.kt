package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "paired_device",
    indices = [
        Index(value = ["mac_address"], unique = true),
    ]
)
data class PairedDevice(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "mac_address")
    val macAddress: String,

    @ColumnInfo(name = "type")
    val type: DeviceType,

    @ColumnInfo(name = "last_connected_at")
    val lastConnectedAt: Long? = null,
)
