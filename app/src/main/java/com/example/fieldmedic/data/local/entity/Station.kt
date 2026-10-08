package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "station")
data class Station(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "commune")
    val commune: String? = null,

    @ColumnInfo(name = "district")
    val district: String? = null,

    @ColumnInfo(name = "province")
    val province: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
