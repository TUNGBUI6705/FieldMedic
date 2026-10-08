package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff",
    indices = [
        Index(value = ["station_id"])
    ]
)
data class Staff(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "station_id")
    val stationId: String? = null,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "role")
    val role: StaffRole,

    @ColumnInfo(name = "phone")
    val phone: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "is_external")
    val isExternal: Boolean = false,

    @ColumnInfo(name = "last_sync_at")
    val lastSyncAt: Long? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
