package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "error_log",
    indices = [
        Index(value = ["created_at"]),
    ]
)
data class ErrorLog(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "level")
    val level: LogLevel,

    @ColumnInfo(name = "tag")
    val tag: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "stack")
    val stack: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
