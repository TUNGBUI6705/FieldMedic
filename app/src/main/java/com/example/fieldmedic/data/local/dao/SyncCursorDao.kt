package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.SyncCursor

@Dao
interface SyncCursorDao {
    @Query("SELECT * FROM sync_cursor WHERE collection = :collection")
    suspend fun get(collection: String): SyncCursor?

    @Upsert
    suspend fun upsert(cursor: SyncCursor)
}
