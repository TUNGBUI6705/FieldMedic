package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.ErrorLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ErrorLogDao {
    @Upsert
    suspend fun insert(errorLog: ErrorLog)

    @Query("SELECT * FROM error_log ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ErrorLog>>

    @Query("DELETE FROM error_log WHERE created_at < :olderThan")
    suspend fun prune(olderThan: Long): Int
}
