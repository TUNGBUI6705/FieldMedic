package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.AlertLevel
import com.example.fieldmedic.data.local.entity.Encounter
import kotlinx.coroutines.flow.Flow

@Dao
interface EncounterDao {

    @Upsert
    suspend fun upsert(encounter: Encounter)

    @Query("SELECT * FROM encounter WHERE id = :id AND is_deleted = 0")
    fun observe(id: String): Flow<Encounter?>

    @Query("SELECT * FROM encounter WHERE patient_id = :patientId AND is_deleted = 0 ORDER BY visit_at DESC")
    fun observeHistory(patientId: String): Flow<List<Encounter>>

    @Query("SELECT * FROM encounter WHERE patient_id = :patientId AND status = 'DRAFT' AND is_deleted = 0 ORDER BY visit_at DESC LIMIT 1")
    suspend fun getDraft(patientId: String): Encounter?

    @Query(
        """
        UPDATE encounter
        SET chief_complaint = :chiefComplaint,
            symptom_note = :symptomNote,
            diagnosis = :diagnosis,
            treatment_note = :treatmentNote,
            draft_step = :draftStep,
            updated_at = :updatedAt
        WHERE id = :id AND status = 'DRAFT'
        """
    )
    suspend fun updateDraft(
        id: String,
        chiefComplaint: String?,
        symptomNote: String?,
        diagnosis: String?,
        treatmentNote: String?,
        draftStep: Int,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE encounter
        SET status = 'COMPLETED',
            completed_at = :completedAt,
            alert_level = :alertLevel,
            has_danger_sign = :hasDangerSign,
            referral_needed = :referralNeeded,
            updated_at = :updatedAt
        WHERE id = :id AND status = 'DRAFT'
        """
    )
    suspend fun complete(
        id: String,
        completedAt: Long,
        alertLevel: AlertLevel,
        hasDangerSign: Boolean,
        referralNeeded: Boolean,
        updatedAt: Long
    ): Int

    @Query("SELECT COUNT(*) FROM encounter WHERE is_deleted = 0 AND status = 'COMPLETED' AND visit_at BETWEEN :start AND :end")
    fun countCompletedBetween(start: Long, end: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM encounter WHERE is_deleted = 0 AND status = 'COMPLETED' AND alert_level != 'NONE' AND visit_at BETWEEN :start AND :end")
    fun countAlertBetween(start: Long, end: Long): Flow<Int>
}
