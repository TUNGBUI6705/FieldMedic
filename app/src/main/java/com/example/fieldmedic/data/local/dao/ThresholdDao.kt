package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.ThresholdRule
import com.example.fieldmedic.data.local.entity.ThresholdSet

@Dao
interface ThresholdDao {
    @Upsert
    suspend fun upsertSets(sets: List<ThresholdSet>): LongArray

    @Upsert
    suspend fun upsertRules(rules: List<ThresholdRule>): LongArray

    @Query("SELECT * FROM threshold_set ORDER BY version DESC LIMIT 1")
    suspend fun latestSet(): ThresholdSet?

    @Query("SELECT * FROM threshold_rule WHERE set_version = :setVersion AND vital_type = :vitalType")
    suspend fun rules(setVersion: Int, vitalType: String): List<ThresholdRule>
}
