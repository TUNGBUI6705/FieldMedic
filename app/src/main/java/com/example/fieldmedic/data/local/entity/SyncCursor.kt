package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_cursor")
data class SyncCursor(
    @PrimaryKey
    @ColumnInfo(name = "collection")
    val collection: String,

    @ColumnInfo(name = "last_pull_server_ts")
    val lastPullServerTs: Long = 0L,

    @ColumnInfo(name = "last_success_at")
    val lastSuccessAt: Long? = null,
)
