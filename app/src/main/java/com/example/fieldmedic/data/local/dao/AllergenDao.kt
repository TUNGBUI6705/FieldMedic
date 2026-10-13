package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.AllergenCatalog

@Dao
interface AllergenDao {
    @Upsert
    suspend fun upsertAll(list: List<AllergenCatalog>): LongArray

    @Query("SELECT * FROM allergen_catalog WHERE search_name LIKE '%' || :q || '%' LIMIT 20")
    suspend fun search(q: String): List<AllergenCatalog>
}
