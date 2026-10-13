package com.example.fieldmedic.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.Patient
import com.example.fieldmedic.data.local.relation.PatientListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Upsert
    suspend fun upsert(patient: Patient): Long

    @Upsert
    suspend fun upsertAll(list: List<Patient>): LongArray

    @Query("SELECT * FROM patient WHERE id = :id AND is_deleted = 0")
    fun observe(id: String): Flow<Patient?>

    @Query(
        """
        SELECT p.id, p.patient_code AS patientCode, p.full_name AS fullName, p.birth_year AS birthYear, p.dob, p.gender,
               v.name AS villageName, p.last_encounter_at AS lastEncounterAt, p.sync_status AS syncStatus,
               p.allergy_status AS allergyStatus, p.is_provisional AS isProvisional, p.lock_enabled AS lockEnabled,
               p.avatar_photo_id AS avatarPhotoId
        FROM patient p LEFT JOIN village v ON v.id = p.village_id
        WHERE p.is_deleted = 0
          AND (:q = '' OR p.search_name LIKE '%' || :q || '%' OR p.phone LIKE '%' || :q || '%' OR p.patient_code LIKE :q || '%')
          AND (:villageId IS NULL OR p.village_id = :villageId)
        ORDER BY p.last_encounter_at DESC, p.full_name
        """
    )
    fun pagingList(q: String, villageId: String?): PagingSource<Int, PatientListItem>

    @Query(
        """
        SELECT * FROM patient
        WHERE is_deleted = 0
          AND full_name = :name
          AND birth_year = :year
          AND (:villageId IS NULL OR village_id = :villageId)
        """
    )
    suspend fun findDuplicates(name: String, year: Int, villageId: String?): List<Patient>

    @Query("SELECT * FROM patient WHERE national_id_hash = :hash AND is_deleted = 0")
    suspend fun findByNationalIdHash(hash: String): List<Patient>

    @Query("UPDATE patient SET last_encounter_at = MAX(COALESCE(last_encounter_at, 0), :at) WHERE id = :id")
    suspend fun touchLastEncounter(id: String, at: Long): Int

    @Query("UPDATE patient SET lock_enabled = :enabled WHERE id = :id")
    suspend fun setLockEnabled(id: String, enabled: Boolean): Int
}
