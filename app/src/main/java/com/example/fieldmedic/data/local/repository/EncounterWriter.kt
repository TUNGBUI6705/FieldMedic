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
import com.example.fieldmedic.data.local.entity.QueueOperation
import com.example.fieldmedic.data.local.entity.Referral
import com.example.fieldmedic.data.local.entity.SyncEntityType
import com.example.fieldmedic.data.local.entity.VitalSign
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class EncounterBundle(
    val encounter: Encounter,
    val vitals: List<VitalSign>,
    val medications: List<MedicationGiven>,
    val referral: Referral?,
    val photos: List<Photo>,
    val patientIsStillPending: Boolean
)

class EncounterWriter(
    private val dbProvider: () -> AppDatabase,
    private val syncEnqueuer: SyncEnqueuer,
    private val auditLogger: AuditLogger
) {
    suspend fun saveCompletedEncounter(bundle: EncounterBundle, now: Long = System.currentTimeMillis()) {
        val db = dbProvider()
        db.withTransaction {
            val completedEncounter = bundle.encounter.copy(
                status = EncounterStatus.COMPLETED,
                completedAt = bundle.encounter.completedAt ?: now,
                syncMeta = bundle.encounter.syncMeta.copy(updatedAt = now)
            )

            // 1. Upsert encounter
            db.encounterDao().upsert(completedEncounter)

            // 2. Upsert vitals
            if (bundle.vitals.isNotEmpty()) {
                db.vitalDao().upsertAll(bundle.vitals)
            }

            // 3. Upsert medications
            if (bundle.medications.isNotEmpty()) {
                db.medicationGivenDao().upsertAll(bundle.medications)
            }

            // 4. Upsert referral
            if (bundle.referral != null) {
                db.referralDao().upsert(bundle.referral)
            }

            // 5. Upsert photos
            if (bundle.photos.isNotEmpty()) {
                db.photoDao().upsertAll(bundle.photos)
            }

            // 6. Touch last encounter on patient
            db.patientDao().touchLastEncounter(completedEncounter.patientId, completedEncounter.visitAt)

            // 7. Enqueue encounter
            val encPriority = if (completedEncounter.alertLevel != AlertLevel.NONE || bundle.referral != null) {
                SyncEnqueuer.ALERT_ENCOUNTER
            } else {
                SyncEnqueuer.ENCOUNTER
            }
            val dependsOn = if (bundle.patientIsStillPending) completedEncounter.patientId else null

            val encounterPayload = Json.encodeToString(completedEncounter)
            syncEnqueuer.enqueue(
                entityType = SyncEntityType.ENCOUNTER,
                entityId = completedEncounter.id,
                patientId = completedEncounter.patientId,
                operation = QueueOperation.CREATE,
                changedFieldsJson = encounterPayload,
                baseValuesJson = "{}",
                baseVersion = 1,
                priority = encPriority,
                dependsOn = dependsOn,
                now = now
            )

            // 8. Enqueue photos (depends on encounterId)
            for (photo in bundle.photos) {
                val photoPayload = Json.encodeToString(photo)
                syncEnqueuer.enqueue(
                    entityType = SyncEntityType.PHOTO,
                    entityId = photo.id,
                    patientId = photo.patientId,
                    operation = QueueOperation.CREATE,
                    changedFieldsJson = photoPayload,
                    baseValuesJson = "{}",
                    baseVersion = 1,
                    priority = SyncEnqueuer.PHOTO,
                    dependsOn = completedEncounter.id,
                    now = now
                )
            }

            // 9. Audit log
            auditLogger.log(
                actorId = completedEncounter.staffId,
                action = AuditAction.VIEW_RECORD,
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
