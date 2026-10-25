package com.example.fieldmedic.data.repository

import androidx.room.withTransaction
import com.example.fieldmedic.data.local.auth.SessionManager
import com.example.fieldmedic.data.local.dao.PatientAllergyDao
import com.example.fieldmedic.data.local.dao.PatientConditionDao
import com.example.fieldmedic.data.local.dao.PatientMedicationDao
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.AllergyRecordStatus
import com.example.fieldmedic.data.local.entity.AuditAction
import com.example.fieldmedic.data.local.entity.AuditResult
import com.example.fieldmedic.data.local.entity.ConditionStatus
import com.example.fieldmedic.data.local.entity.MedStatus
import com.example.fieldmedic.data.local.entity.PatientAllergy
import com.example.fieldmedic.data.local.entity.PatientCondition
import com.example.fieldmedic.data.local.entity.PatientMedication
import com.example.fieldmedic.data.local.entity.QueueOperation
import com.example.fieldmedic.data.local.entity.SyncEntityType
import com.example.fieldmedic.data.local.entity.SyncMeta
import com.example.fieldmedic.data.local.entity.SyncStatus
import com.example.fieldmedic.data.local.repository.AuditLogger
import com.example.fieldmedic.data.local.repository.SyncEnqueuer
import com.example.fieldmedic.domain.model.AllergyInput
import com.example.fieldmedic.domain.model.AllergyItem
import com.example.fieldmedic.domain.model.AppError
import com.example.fieldmedic.domain.model.AppResult
import com.example.fieldmedic.domain.model.ConditionInput
import com.example.fieldmedic.domain.model.ConditionItem
import com.example.fieldmedic.domain.model.MedicationInput
import com.example.fieldmedic.domain.model.MedicationItem
import com.example.fieldmedic.domain.repository.ClinicalHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID

class ClinicalHistoryRepositoryImpl(
    private val dbProvider: () -> AppDatabase,
    private val allergyDao: PatientAllergyDao,
    private val conditionDao: PatientConditionDao,
    private val medicationDao: PatientMedicationDao,
    private val syncEnqueuer: SyncEnqueuer,
    private val auditLogger: AuditLogger,
    private val sessionManager: SessionManager
) : ClinicalHistoryRepository {

    override fun observeAllergies(patientId: String): Flow<List<AllergyItem>> {
        return allergyDao.observeActive(patientId).map { list ->
            list.map {
                AllergyItem(
                    id = it.id,
                    patientId = it.patientId,
                    allergenCode = it.allergenCode,
                    allergenName = it.allergenName,
                    allergyClassCode = it.allergyClassCode,
                    reaction = it.reaction,
                    severity = it.severity,
                    status = it.status,
                    recordedBy = it.recordedBy,
                    recordedAt = it.recordedAt
                )
            }
        }
    }

    override fun observeConditions(patientId: String): Flow<List<ConditionItem>> {
        return conditionDao.observeActive(patientId).map { list ->
            list.map {
                ConditionItem(
                    id = it.id,
                    patientId = it.patientId,
                    conditionCode = it.conditionCode,
                    conditionName = it.conditionName,
                    status = it.status,
                    diagnosedYear = it.diagnosedYear,
                    note = it.note
                )
            }
        }
    }

    override fun observeMedications(patientId: String): Flow<List<MedicationItem>> {
        return medicationDao.observeActive(patientId).map { list ->
            list.map {
                MedicationItem(
                    id = it.id,
                    patientId = it.patientId,
                    drugId = it.drugId,
                    drugName = it.drugName,
                    dose = it.dose,
                    frequency = it.frequency,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    status = it.status,
                    source = it.source,
                    note = it.note
                )
            }
        }
    }

    override suspend fun upsertAllergy(patientId: String, input: AllergyInput): AppResult<Unit, AppError> {
        val currentStaff = sessionManager.currentStaff.value
            ?: return AppResult.Error(AppError.Unauthorized("Not logged in"))

        return try {
            val db = dbProvider()
            db.withTransaction {
                val now = System.currentTimeMillis()
                val isNew = input.id == null
                val allergyId = input.id ?: UUID.randomUUID().toString()

                val entity = PatientAllergy(
                    id = allergyId,
                    patientId = patientId,
                    allergenCode = input.allergenCode,
                    allergenName = input.allergenName,
                    allergyClassCode = input.allergyClassCode,
                    reaction = input.reaction,
                    severity = input.severity,
                    status = input.status,
                    recordedBy = currentStaff.id,
                    recordedAt = now,
                    syncMeta = SyncMeta(createdAt = now, updatedAt = now, syncStatus = SyncStatus.PENDING)
                )

                allergyDao.upsert(entity)

                val operation = if (isNew) QueueOperation.CREATE else QueueOperation.UPDATE
                syncEnqueuer.enqueue(
                    entityType = SyncEntityType.PATIENT_ALLERGY,
                    entityId = allergyId,
                    patientId = patientId,
                    operation = operation,
                    changedFieldsJson = "{}",
                    baseValuesJson = "{}",
                    baseVersion = if (isNew) 0 else 1,
                    priority = SyncEnqueuer.PATIENT,
                    dependsOn = null,
                    now = now
                )

                auditLogger.log(
                    actorId = currentStaff.id,
                    action = AuditAction.VIEW_RECORD,
                    entityType = "PATIENT_ALLERGY",
                    entityId = allergyId,
                    patientId = patientId,
                    result = AuditResult.OK,
                    reason = "Upsert allergy"
                )
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e))
        }
    }

    override suspend fun upsertCondition(patientId: String, input: ConditionInput): AppResult<Unit, AppError> {
        val currentStaff = sessionManager.currentStaff.value
            ?: return AppResult.Error(AppError.Unauthorized("Not logged in"))

        return try {
            val db = dbProvider()
            db.withTransaction {
                val now = System.currentTimeMillis()
                val isNew = input.id == null
                val conditionId = input.id ?: UUID.randomUUID().toString()

                val entity = PatientCondition(
                    id = conditionId,
                    patientId = patientId,
                    conditionCode = input.conditionCode,
                    conditionName = input.conditionName,
                    status = input.status,
                    diagnosedYear = input.diagnosedYear,
                    note = input.note,
                    syncMeta = SyncMeta(createdAt = now, updatedAt = now, syncStatus = SyncStatus.PENDING)
                )

                conditionDao.upsert(entity)

                val operation = if (isNew) QueueOperation.CREATE else QueueOperation.UPDATE
                syncEnqueuer.enqueue(
                    entityType = SyncEntityType.PATIENT_CONDITION,
                    entityId = conditionId,
                    patientId = patientId,
                    operation = operation,
                    changedFieldsJson = "{}",
                    baseValuesJson = "{}",
                    baseVersion = if (isNew) 0 else 1,
                    priority = SyncEnqueuer.PATIENT,
                    dependsOn = null,
                    now = now
                )

                auditLogger.log(
                    actorId = currentStaff.id,
                    action = AuditAction.VIEW_RECORD,
                    entityType = "PATIENT_CONDITION",
                    entityId = conditionId,
                    patientId = patientId,
                    result = AuditResult.OK,
                    reason = "Upsert condition"
                )
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e))
        }
    }

    override suspend fun upsertMedication(patientId: String, input: MedicationInput): AppResult<Unit, AppError> {
        val currentStaff = sessionManager.currentStaff.value
            ?: return AppResult.Error(AppError.Unauthorized("Not logged in"))

        return try {
            val db = dbProvider()
            db.withTransaction {
                val now = System.currentTimeMillis()
                val isNew = input.id == null
                val medId = input.id ?: UUID.randomUUID().toString()

                val entity = PatientMedication(
                    id = medId,
                    patientId = patientId,
                    drugId = input.drugId,
                    drugName = input.drugName,
                    dose = input.dose,
                    frequency = input.frequency,
                    startDate = input.startDate,
                    endDate = input.endDate,
                    status = input.status,
                    source = input.source,
                    note = input.note,
                    syncMeta = SyncMeta(createdAt = now, updatedAt = now, syncStatus = SyncStatus.PENDING)
                )

                medicationDao.upsert(entity)

                val operation = if (isNew) QueueOperation.CREATE else QueueOperation.UPDATE
                syncEnqueuer.enqueue(
                    entityType = SyncEntityType.PATIENT_MEDICATION,
                    entityId = medId,
                    patientId = patientId,
                    operation = operation,
                    changedFieldsJson = "{}",
                    baseValuesJson = "{}",
                    baseVersion = if (isNew) 0 else 1,
                    priority = SyncEnqueuer.PATIENT,
                    dependsOn = null,
                    now = now
                )

                auditLogger.log(
                    actorId = currentStaff.id,
                    action = AuditAction.VIEW_RECORD,
                    entityType = "PATIENT_MEDICATION",
                    entityId = medId,
                    patientId = patientId,
                    result = AuditResult.OK,
                    reason = "Upsert medication"
                )
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e))
        }
    }

    override suspend fun softDeleteAllergy(id: String): AppResult<Unit, AppError> {
        val currentStaff = sessionManager.currentStaff.value
            ?: return AppResult.Error(AppError.Unauthorized("Not logged in"))

        return try {
            val db = dbProvider()
            val existing = allergyDao.observeActive("").firstOrNull()?.find { it.id == id }
                ?: return AppResult.Error(AppError.ValidationFailed("Allergy not found"))

            db.withTransaction {
                val now = System.currentTimeMillis()
                val deleted = existing.copy(
                    status = AllergyRecordStatus.RESOLVED,
                    syncMeta = existing.syncMeta.copy(isDeleted = true, updatedAt = now, syncStatus = SyncStatus.PENDING)
                )
                allergyDao.upsert(deleted)

                syncEnqueuer.enqueue(
                    entityType = SyncEntityType.PATIENT_ALLERGY,
                    entityId = id,
                    patientId = existing.patientId,
                    operation = QueueOperation.DELETE,
                    changedFieldsJson = "{}",
                    baseValuesJson = "{}",
                    baseVersion = 1,
                    priority = SyncEnqueuer.PATIENT,
                    dependsOn = null,
                    now = now
                )

                auditLogger.log(
                    actorId = currentStaff.id,
                    action = AuditAction.VIEW_RECORD,
                    entityType = "PATIENT_ALLERGY",
                    entityId = id,
                    patientId = existing.patientId,
                    result = AuditResult.OK,
                    reason = "Soft delete allergy"
                )
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e))
        }
    }

    override suspend fun softDeleteCondition(id: String): AppResult<Unit, AppError> {
        val currentStaff = sessionManager.currentStaff.value
            ?: return AppResult.Error(AppError.Unauthorized("Not logged in"))

        return try {
            val db = dbProvider()
            val existing = conditionDao.observeActive("").firstOrNull()?.find { it.id == id }
                ?: return AppResult.Error(AppError.ValidationFailed("Condition not found"))

            db.withTransaction {
                val now = System.currentTimeMillis()
                val deleted = existing.copy(
                    status = ConditionStatus.RESOLVED,
                    syncMeta = existing.syncMeta.copy(isDeleted = true, updatedAt = now, syncStatus = SyncStatus.PENDING)
                )
                conditionDao.upsert(deleted)

                syncEnqueuer.enqueue(
                    entityType = SyncEntityType.PATIENT_CONDITION,
                    entityId = id,
                    patientId = existing.patientId,
                    operation = QueueOperation.DELETE,
                    changedFieldsJson = "{}",
                    baseValuesJson = "{}",
                    baseVersion = 1,
                    priority = SyncEnqueuer.PATIENT,
                    dependsOn = null,
                    now = now
                )

                auditLogger.log(
                    actorId = currentStaff.id,
                    action = AuditAction.VIEW_RECORD,
                    entityType = "PATIENT_CONDITION",
                    entityId = id,
                    patientId = existing.patientId,
                    result = AuditResult.OK,
                    reason = "Soft delete condition"
                )
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e))
        }
    }

    override suspend fun softDeleteMedication(id: String): AppResult<Unit, AppError> {
        val currentStaff = sessionManager.currentStaff.value
            ?: return AppResult.Error(AppError.Unauthorized("Not logged in"))

        return try {
            val db = dbProvider()
            val existing = medicationDao.observeActive("").firstOrNull()?.find { it.id == id }
                ?: return AppResult.Error(AppError.ValidationFailed("Medication not found"))

            db.withTransaction {
                val now = System.currentTimeMillis()
                val deleted = existing.copy(
                    status = MedStatus.STOPPED,
                    syncMeta = existing.syncMeta.copy(isDeleted = true, updatedAt = now, syncStatus = SyncStatus.PENDING)
                )
                medicationDao.upsert(deleted)

                syncEnqueuer.enqueue(
                    entityType = SyncEntityType.PATIENT_MEDICATION,
                    entityId = id,
                    patientId = existing.patientId,
                    operation = QueueOperation.DELETE,
                    changedFieldsJson = "{}",
                    baseValuesJson = "{}",
                    baseVersion = 1,
                    priority = SyncEnqueuer.PATIENT,
                    dependsOn = null,
                    now = now
                )

                auditLogger.log(
                    actorId = currentStaff.id,
                    action = AuditAction.VIEW_RECORD,
                    entityType = "PATIENT_MEDICATION",
                    entityId = id,
                    patientId = existing.patientId,
                    result = AuditResult.OK,
                    reason = "Soft delete medication"
                )
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e))
        }
    }
}
