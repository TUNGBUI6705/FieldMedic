package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.DrugCatalog

@Dao
interface DrugDao {
    @Upsert
    suspend fun upsertAll(list: List<DrugCatalog>): LongArray

    @Query("SELECT * FROM drug_catalog WHERE is_active = 1 AND search_name LIKE '%' || :q || '%' LIMIT 20")
    suspend fun search(q: String): List<DrugCatalog>

    @Query("SELECT * FROM drug_catalog WHERE id = :id")
    suspend fun getById(id: String): DrugCatalog?
}
