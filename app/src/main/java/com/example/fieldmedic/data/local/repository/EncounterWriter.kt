package com.example.fieldmedic.data.local.repository

import androidx.room.withTransaction
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.AlertLevel
import com.example.fieldmedic.data.local.entity.AuditAction
import com.example.fieldmedic.data.local.entity.AuditResult
import com.example.fieldmedic.data.local.entity.Encounter
import com.example.fieldmedic.data.local.entity.EncounterStatus
import com.example.fieldmedic.data.local.entity.MedicationGiven
import com.example.fieldmedic.data.local.entity.Photo
import com.example.fieldmedic.data.local.entity.PhotoOwnerType
import com.example.fieldmedic.data.local.entity.QueueOperation
import com.example.fieldmedic.data.local.entity.Referral
import com.example.fieldmedic.data.local.entity.SyncEntityType
import com.example.fieldmedic.data.local.entity.SyncStatus
import com.example.fieldmedic.data.local.entity.VitalSign
import com.example.fieldmedic.data.remote.wire.EncounterWire
import com.example.fieldmedic.data.remote.wire.PhotoWire

data class EncounterBundle(
    val encounter: Encounter,
    val vitals: List<VitalSign>,
    val medications: List<MedicationGiven>,
    val referral: Referral?,
    val photos: List<Photo>
)

class EncounterWriter(
    private val dbProvider: () -> AppDatabase,
    private val syncEnqueuer: SyncEnqueuer,
    private val auditLogger: AuditLogger
) {

    private fun validateBundle(bundle: EncounterBundle) {
        val encounter = bundle.encounter
        for (v in bundle.vitals) {
            if (v.encounterId != encounter.id || v.patientId != encounter.patientId) {
                throw EncounterWriteException.InconsistentBundle("vital encounterId or patientId mismatch")
            }
        }
        for (m in bundle.medications) {
            if (m.encounterId != encounter.id || m.patientId != encounter.patientId) {
                throw EncounterWriteException.InconsistentBundle("medication encounterId or patientId mismatch")
            }
        }
        if (bundle.referral != null) {
            if (bundle.referral.encounterId != encounter.id || bundle.referral.patientId != encounter.patientId) {
                throw EncounterWriteException.InconsistentBundle("referral encounterId or patientId mismatch")
            }
        }
        for (p in bundle.photos) {
            if (p.patientId != encounter.patientId) {
                throw EncounterWriteException.InconsistentBundle("photo patientId mismatch")
            }
            if (p.ownerType == PhotoOwnerType.ENCOUNTER && p.ownerId != encounter.id) {
                throw EncounterWriteException.InconsistentBundle("photo ownerId mismatch")
            }
        }
    }

    suspend fun saveCompletedEncounter(bundle: EncounterBundle, now: Long = System.currentTimeMillis()) {
        validateBundle(bundle)

        val db = dbProvider()
        db.withTransaction {
            val existing = db.encounterDao().getById(bundle.encounter.id)
            if (existing != null && existing.status == EncounterStatus.COMPLETED) {
                throw EncounterWriteException.AlreadyCompleted(bundle.encounter.id)
            }

            val completedEncounter = bundle.encounter.copy(
                status = EncounterStatus.COMPLETED,
                completedAt = bundle.encounter.completedAt ?: now,
                syncMeta = bundle.encounter.syncMeta.copy(
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            )

            // 1. Upsert encounter
            db.encounterDao().upsert(completedEncounter)

            // 2. Upsert vitals
            val vitalsToSave = bundle.vitals.map {
                it.copy(syncMeta = it.syncMeta.copy(updatedAt = now, syncStatus = SyncStatus.PENDING))
            }
            if (vitalsToSave.isNotEmpty()) {
                db.vitalDao().upsertAll(vitalsToSave)
            }

            // 3. Upsert medications
            val medsToSave = bundle.medications.map {
                it.copy(syncMeta = it.syncMeta.copy(updatedAt = now, syncStatus = SyncStatus.PENDING))
            }
            if (medsToSave.isNotEmpty()) {
                db.medicationGivenDao().upsertAll(medsToSave)
            }

            // 4. Upsert referral
            val referralToSave = bundle.referral?.copy(
                syncMeta = bundle.referral.syncMeta.copy(updatedAt = now, syncStatus = SyncStatus.PENDING)
            )
            if (referralToSave != null) {
                db.referralDao().upsert(referralToSave)
            }

            // 5. Upsert photos
            val photosToSave = bundle.photos.map {
                it.copy(syncMeta = it.syncMeta.copy(updatedAt = now, syncStatus = SyncStatus.PENDING))
            }
            if (photosToSave.isNotEmpty()) {
                db.photoDao().upsertAll(photosToSave)
            }

            // 6. Touch last encounter on patient
            db.patientDao().touchLastEncounter(completedEncounter.patientId, completedEncounter.visitAt)

            // 7. Enqueue encounter
            val encPriority = if (completedEncounter.alertLevel != AlertLevel.NONE || referralToSave != null) {
                SyncEnqueuer.ALERT_ENCOUNTER
            } else {
                SyncEnqueuer.ENCOUNTER
            }
            val patientPendingCount = db.syncDao().countForEntity(SyncEntityType.PATIENT, completedEncounter.patientId)
            val dependsOn = if (patientPendingCount > 0) completedEncounter.patientId else null

            val encounterPayload = EncounterWire.toPayload(completedEncounter, vitalsToSave, medsToSave, referralToSave).toString()
            syncEnqueuer.enqueue(
                entityType = SyncEntityType.ENCOUNTER,
                entityId = completedEncounter.id,
                patientId = completedEncounter.patientId,
                operation = QueueOperation.CREATE,
                changedFieldsJson = encounterPayload,
                baseValuesJson = "{}",
                baseVersion = 0,
                priority = encPriority,
                dependsOn = dependsOn,
                now = now
            )

            // 8. Enqueue photos (depends on encounterId)
            for (photo in photosToSave) {
                val photoPayload = PhotoWire.toPayload(photo).toString()
                syncEnqueuer.enqueue(
                    entityType = SyncEntityType.PHOTO,
                    entityId = photo.id,
                    patientId = photo.patientId,
                    operation = QueueOperation.CREATE,
                    changedFieldsJson = photoPayload,
                    baseValuesJson = "{}",
                    baseVersion = 0,
                    priority = SyncEnqueuer.PHOTO,
                    dependsOn = completedEncounter.id,
                    now = now
                )
            }

            // 9. Audit log
            auditLogger.log(
                actorId = completedEncounter.staffId,
                action = AuditAction.COMPLETE_ENCOUNTER,
                entityType = "ENCOUNTER",
                entityId = completedEncounter.id,
                patientId = completedEncounter.patientId,
                result = AuditResult.OK,
                reason = "Completed encounter saved",
                metadata = mapOf("encounterId" to completedEncounter.id)
            )
        }
    }
}
