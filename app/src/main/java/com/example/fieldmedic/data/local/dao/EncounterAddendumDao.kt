package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.EncounterAddendum
import kotlinx.coroutines.flow.Flow

@Dao
interface EncounterAddendumDao {

    @Upsert
    suspend fun insert(addendum: EncounterAddendum)

    @Query("SELECT * FROM encounter_addendum WHERE id = :id AND is_deleted = 0")
    fun observe(id: String): Flow<EncounterAddendum?>

    @Query("SELECT * FROM encounter_addendum WHERE encounter_id = :encounterId AND is_deleted = 0 ORDER BY created_at DESC")
    fun forEncounter(encounterId: String): Flow<List<EncounterAddendum>>
}
