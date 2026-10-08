package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "village",
    indices = [
        Index(value = ["station_id"])
    ]
)
data class Village(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "station_id")
    val stationId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "commune")
    val commune: String? = null,

    @ColumnInfo(name = "district")
    val district: String? = null,

    @ColumnInfo(name = "province")
    val province: String? = null,

    @ColumnInfo(name = "lat")
    val lat: Double? = null,

    @ColumnInfo(name = "lng")
    val lng: Double? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
