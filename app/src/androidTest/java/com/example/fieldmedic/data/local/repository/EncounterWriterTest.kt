package com.example.fieldmedic.data.local.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.EncounterStatus
import com.example.fieldmedic.data.local.entity.QueueOperation
import com.example.fieldmedic.data.local.entity.SyncEntityType
import com.example.fieldmedic.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncounterWriterTest {

    private lateinit var database: AppDatabase
    private lateinit var auditLogger: AuditLogger
    private lateinit var syncEnqueuer: SyncEnqueuer
    private lateinit var encounterWriter: EncounterWriter

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        auditLogger = AuditLogger({ database }, "dev-device", "1.0")
        syncEnqueuer = SyncEnqueuer { database }
        encounterWriter = EncounterWriter({ database }, syncEnqueuer, auditLogger)
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun W01_save_writesAllRows(): Unit = runBlocking {
        val patient = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(patient)

        val enc = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val vitals = listOf(EncounterFixtures.fixtureVital(id = "v1", encounterId = "enc1", patientId = "p1"), EncounterFixtures.fixtureVital(id = "v2", encounterId = "enc1", patientId = "p1"))
        val meds = listOf(EncounterFixtures.fixtureMedication(id = "m1", encounterId = "enc1", patientId = "p1"))
        val ref = EncounterFixtures.fixtureReferral(id = "ref1", encounterId = "enc1", patientId = "p1")
        val photos = listOf(EncounterFixtures.fixturePhoto(id = "ph1", encounterId = "enc1", patientId = "p1"))

        val bundle = EncounterFixtures.fixtureBundle(
            encounter = enc,
            vitals = vitals,
            medications = meds,
            referral = ref,
            photos = photos
        )

        encounterWriter.saveCompletedEncounter(bundle)

        assertEquals(1, EncounterFixtures.count(database, "encounter", "id = 'enc1'"))
        assertEquals(2, EncounterFixtures.count(database, "vital_sign", "encounter_id = 'enc1'"))
        assertEquals(1, EncounterFixtures.count(database, "medication_given", "encounter_id = 'enc1'"))
        assertEquals(1, EncounterFixtures.count(database, "referral", "encounter_id = 'enc1'"))
        assertEquals(1, EncounterFixtures.count(database, "photo", "encounter_id = 'enc1'"))

        val pSaved = database.patientDao().observe("p1").first()
        assertNotNull(pSaved?.lastEncounterAt)

        val queueItems = database.syncDao().observeFiltered("PENDING").first()
        assertEquals(2, queueItems.size) // encounter + photo
    }

    @Test
    fun W02_save_includesChildrenInEncounterPayload(): Unit = runBlocking {
        val patient = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(patient)

        val enc = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val vitals = listOf(EncounterFixtures.fixtureVital(id = "v1", encounterId = "enc1", patientId = "p1"))
        val meds = listOf(EncounterFixtures.fixtureMedication(id = "m1", encounterId = "enc1", patientId = "p1"))
        val ref = EncounterFixtures.fixtureReferral(id = "ref1", encounterId = "enc1", patientId = "p1")

        val bundle = EncounterFixtures.fixtureBundle(
            encounter = enc,
            vitals = vitals,
            medications = meds,
            referral = ref,
            photos = emptyList()
        )

        encounterWriter.saveCompletedEncounter(bundle)

        val queueItems = database.syncDao().observeFiltered("PENDING").first()
        val encQueue = queueItems.find { it.entityType == SyncEntityType.ENCOUNTER }
        assertNotNull(encQueue)

        val jsonObj = Json.parseToJsonElement(encQueue!!.payload).jsonObject
        assertTrue(jsonObj.containsKey("vitals"))
        assertEquals(1, jsonObj["vitals"]!!.jsonArray.size)
        assertTrue(jsonObj.containsKey("medications"))
        assertEquals(1, jsonObj["medications"]!!.jsonArray.size)
        assertTrue(jsonObj.containsKey("referral"))
        assertNotNull(jsonObj["referral"])
    }

    @Test
    fun W03_save_payloadsHaveNoLocalMeta(): Unit = runBlocking {
        val patient = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(patient)

        val enc = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val ref = EncounterFixtures.fixtureReferral(id = "ref1", encounterId = "enc1", patientId = "p1")
        val photo = EncounterFixtures.fixturePhoto(id = "ph1", encounterId = "enc1", patientId = "p1")

        val bundle = EncounterFixtures.fixtureBundle(
            encounter = enc,
            referral = ref,
            photos = listOf(photo)
        )

        encounterWriter.saveCompletedEncounter(bundle)

        val queueItems = database.syncDao().observeFiltered("PENDING").first()
        val forbiddenKeys = setOf(
            "syncMeta", "syncStatus", "serverUpdatedAt", "localPath", "version",
            "createdAt", "updatedAt", "updatedBy", "isDeleted", "lastEncounterAt",
            "uploadStatus", "uploadSession", "bytesUploaded", "remotePath", "dekVersion", "isFileEncrypted"
        )

        fun checkKeys(element: kotlinx.serialization.json.JsonElement) {
            when (element) {
                is kotlinx.serialization.json.JsonObject -> {
                    for ((key, value) in element) {
                        assertTrue("Payload contains forbidden key: $key", !forbiddenKeys.contains(key))
                        checkKeys(value)
                    }
                }
                is kotlinx.serialization.json.JsonArray -> {
                    for (item in element) {
                        checkKeys(item)
                    }
                }
                else -> {}
            }
        }

        for (item in queueItems) {
            val jsonObj = Json.parseToJsonElement(item.payload).jsonObject
            checkKeys(jsonObj)
        }
    }

    @Test
    fun W04_save_forcesPendingStatus(): Unit = runBlocking {
        val patient = EncounterFixtures.fixturePatient(id = "p1", syncStatus = SyncStatus.SYNCED)
        database.patientDao().upsert(patient)

        val enc = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1", syncStatus = SyncStatus.SYNCED)
        val v = EncounterFixtures.fixtureVital(id = "v1", encounterId = "enc1", patientId = "p1", syncStatus = SyncStatus.SYNCED)
        val m = EncounterFixtures.fixtureMedication(id = "m1", encounterId = "enc1", patientId = "p1", syncStatus = SyncStatus.SYNCED)
        val r = EncounterFixtures.fixtureReferral(id = "ref1", encounterId = "enc1", patientId = "p1", syncStatus = SyncStatus.SYNCED)
        val ph = EncounterFixtures.fixturePhoto(id = "ph1", encounterId = "enc1", patientId = "p1", syncStatus = SyncStatus.SYNCED)

        val bundle = EncounterFixtures.fixtureBundle(
            encounter = enc,
            vitals = listOf(v),
            medications = listOf(m),
            referral = r,
            photos = listOf(ph)
        )

        encounterWriter.saveCompletedEncounter(bundle)

        val savedEnc = database.encounterDao().observe("enc1").first()
        assertEquals(SyncStatus.PENDING, savedEnc?.syncMeta?.syncStatus)
    }

    @Test
    fun W05_save_auditActionIsCompleteEncounter(): Unit = runBlocking {
        val patient = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(patient)
        val enc = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val bundle = EncounterFixtures.fixtureBundle(encounter = enc, photos = emptyList())

        encounterWriter.saveCompletedEncounter(bundle)

        val lastAudit = database.auditLogDao().lastEntry()
        assertNotNull(lastAudit)
        assertEquals("COMPLETE_ENCOUNTER", lastAudit?.action?.name)
        assertEquals("ENCOUNTER", lastAudit?.entityType)
        assertEquals("enc1", lastAudit?.entityId)
        assertEquals("p1", lastAudit?.patientId)
    }

    @Test
    fun W06_save_createItemsHaveBaseVersionZero(): Unit = runBlocking {
        val patient = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(patient)
        val enc = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val photo = EncounterFixtures.fixturePhoto(id = "ph1", encounterId = "enc1", patientId = "p1")
        val bundle = EncounterFixtures.fixtureBundle(encounter = enc, photos = listOf(photo))

        encounterWriter.saveCompletedEncounter(bundle)

        val queueItems = database.syncDao().observeFiltered("PENDING").first()
        for (item in queueItems) {
            assertEquals(QueueOperation.CREATE, item.operation)
            assertEquals(0, item.baseVersion)
        }
    }

    @Test
    fun W07_save_twiceSameEncounter_isRejected(): Unit = runBlocking {
        val patient = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(patient)
        val enc = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val bundle = EncounterFixtures.fixtureBundle(encounter = enc, photos = emptyList())

        encounterWriter.saveCompletedEncounter(bundle)

        val queueSize1 = database.syncDao().observeFiltered("PENDING").first().size
        val enc1 = database.encounterDao().observe("enc1").first()
        val auditSize1 = database.auditLogDao().allOrdered().size
        val vitalSize1 = EncounterFixtures.count(database, "vital_sign")

        assertThrows(EncounterWriteException.AlreadyCompleted::class.java) {
            runBlocking {
                encounterWriter.saveCompletedEncounter(bundle)
            }
        }

        val queueSize2 = database.syncDao().observeFiltered("PENDING").first().size
        val enc2 = database.encounterDao().observe("enc1").first()
        val auditSize2 = database.auditLogDao().allOrdered().size
        val vitalSize2 = EncounterFixtures.count(database, "vital_sign")

        assertEquals(queueSize1, queueSize2)
        assertEquals(enc1?.completedAt, enc2?.completedAt)
        assertEquals(auditSize1, auditSize2)
        assertEquals(vitalSize1, vitalSize2)
    }

    @Test
    fun W08_save_overExistingDraft_succeeds(): Unit = runBlocking {
        val patient = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(patient)

        val draftEnc = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1", status = EncounterStatus.DRAFT)
        database.encounterDao().upsert(draftEnc)

        val bundle = EncounterFixtures.fixtureBundle(encounter = draftEnc, photos = emptyList())
        encounterWriter.saveCompletedEncounter(bundle)

        val saved = database.encounterDao().observe("enc1").first()
        assertNotNull(saved)
        assertEquals(EncounterStatus.COMPLETED, saved?.status)

        val queueItems = database.syncDao().observeFiltered("PENDING").first()
        assertEquals(1, queueItems.size)
        assertEquals(QueueOperation.CREATE, queueItems[0].operation)
    }

    @Test
    fun W09a_save_rejectsInconsistentBundle_vitalEncounterMismatch(): Unit = runBlocking {
        val p1 = EncounterFixtures.fixturePatient(id = "p1")
        val p2 = EncounterFixtures.fixturePatient(id = "p2")
        database.patientDao().upsertAll(listOf(p1, p2))

        val enc1 = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val encOther = EncounterFixtures.fixtureEncounter(id = "encOther", patientId = "p2")
        database.encounterDao().upsert(encOther)

        val vitalWrong = EncounterFixtures.fixtureVital(encounterId = "encOther", patientId = "p1")
        val bundle = EncounterFixtures.fixtureBundle(encounter = enc1, vitals = listOf(vitalWrong), photos = emptyList())

        assertThrows(EncounterWriteException.InconsistentBundle::class.java) {
            runBlocking { encounterWriter.saveCompletedEncounter(bundle) }
        }
        assertEquals(0, EncounterFixtures.count(database, "encounter", "id = 'enc1'"))
    }

    @Test
    fun W09b_save_rejectsInconsistentBundle_vitalPatientMismatch(): Unit = runBlocking {
        val p1 = EncounterFixtures.fixturePatient(id = "p1")
        val p2 = EncounterFixtures.fixturePatient(id = "p2")
        database.patientDao().upsertAll(listOf(p1, p2))

        val enc1 = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val vitalWrong = EncounterFixtures.fixtureVital(encounterId = "enc1", patientId = "p2")
        val bundle = EncounterFixtures.fixtureBundle(encounter = enc1, vitals = listOf(vitalWrong), photos = emptyList())

        assertThrows(EncounterWriteException.InconsistentBundle::class.java) {
            runBlocking { encounterWriter.saveCompletedEncounter(bundle) }
        }
    }

    @Test
    fun W09c_save_rejectsInconsistentBundle_medicationEncounterMismatch(): Unit = runBlocking {
        val p1 = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(p1)

        val enc1 = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val encOther = EncounterFixtures.fixtureEncounter(id = "encOther", patientId = "p1")
        database.encounterDao().upsert(encOther)

        val medWrong = EncounterFixtures.fixtureMedication(encounterId = "encOther", patientId = "p1")
        val bundle = EncounterFixtures.fixtureBundle(encounter = enc1, medications = listOf(medWrong), photos = emptyList())

        assertThrows(EncounterWriteException.InconsistentBundle::class.java) {
            runBlocking { encounterWriter.saveCompletedEncounter(bundle) }
        }
    }

    @Test
    fun W09d_save_rejectsInconsistentBundle_referralPatientMismatch(): Unit = runBlocking {
        val p1 = EncounterFixtures.fixturePatient(id = "p1")
        val p2 = EncounterFixtures.fixturePatient(id = "p2")
        database.patientDao().upsertAll(listOf(p1, p2))

        val enc1 = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val refWrong = EncounterFixtures.fixtureReferral(encounterId = "enc1", patientId = "p2")
        val bundle = EncounterFixtures.fixtureBundle(encounter = enc1, referral = refWrong, photos = emptyList())

        assertThrows(EncounterWriteException.InconsistentBundle::class.java) {
            runBlocking { encounterWriter.saveCompletedEncounter(bundle) }
        }
    }

    @Test
    fun W09e_save_rejectsInconsistentBundle_photoPatientMismatch(): Unit = runBlocking {
        val p1 = EncounterFixtures.fixturePatient(id = "p1")
        val p2 = EncounterFixtures.fixturePatient(id = "p2")
        database.patientDao().upsertAll(listOf(p1, p2))

        val enc1 = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val photoWrong = EncounterFixtures.fixturePhoto(encounterId = "enc1", patientId = "p2")
        val bundle = EncounterFixtures.fixtureBundle(encounter = enc1, photos = listOf(photoWrong))

        assertThrows(EncounterWriteException.InconsistentBundle::class.java) {
            runBlocking { encounterWriter.saveCompletedEncounter(bundle) }
        }
    }

    @Test
    fun W09f_save_rejectsInconsistentBundle_photoOwnerMismatch(): Unit = runBlocking {
        val p1 = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(p1)

        val enc1 = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val encOther = EncounterFixtures.fixtureEncounter(id = "encOther", patientId = "p1")
        database.encounterDao().upsert(encOther)

        val photoWrong = EncounterFixtures.fixturePhoto(encounterId = "encOther", patientId = "p1")
        val bundle = EncounterFixtures.fixtureBundle(encounter = enc1, photos = listOf(photoWrong))

        assertThrows(EncounterWriteException.InconsistentBundle::class.java) {
            runBlocking { encounterWriter.saveCompletedEncounter(bundle) }
        }
    }

    @Test
    fun W10a_save_dependsOn_patientQueuedPending(): Unit = runBlocking {
        val p1 = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(p1)

        // Enqueue patient pending
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "p1",
            patientId = "p1",
            operation = QueueOperation.CREATE,
            changedFieldsJson = "{}",
            baseValuesJson = "{}",
            baseVersion = 0,
            priority = 1,
            dependsOn = null
        )

        val enc = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val bundle = EncounterFixtures.fixtureBundle(encounter = enc, photos = emptyList())

        encounterWriter.saveCompletedEncounter(bundle)

        val queueItems = database.syncDao().observeFiltered("PENDING").first()
        val encQueue = queueItems.find { it.entityType == SyncEntityType.ENCOUNTER }
        assertEquals("p1", encQueue?.dependsOn)
    }

    @Test
    fun W10b_save_dependsOn_patientNotQueued(): Unit = runBlocking {
        val p1 = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(p1)

        val enc = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val bundle = EncounterFixtures.fixtureBundle(encounter = enc, photos = emptyList())

        encounterWriter.saveCompletedEncounter(bundle)

        val queueItems = database.syncDao().observeFiltered("PENDING").first()
        val encQueue = queueItems.find { it.entityType == SyncEntityType.ENCOUNTER }
        assertEquals(null, encQueue?.dependsOn)
    }

    @Test
    fun W10c_save_dependsOn_patientItemIsSending(): Unit = runBlocking {
        val p1 = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(p1)

        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "p1",
            patientId = "p1",
            operation = QueueOperation.CREATE,
            changedFieldsJson = "{}",
            baseValuesJson = "{}",
            baseVersion = 0,
            priority = 1,
            dependsOn = null
        )
        val pendingList = database.syncDao().observeFiltered("PENDING").first()
        database.syncDao().markInFlight(pendingList[0].id)

        val enc = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val bundle = EncounterFixtures.fixtureBundle(encounter = enc, photos = emptyList())

        encounterWriter.saveCompletedEncounter(bundle)

        val queueItems = database.syncDao().observeFiltered("PENDING").first()
        val encQueue = queueItems.find { it.entityType == SyncEntityType.ENCOUNTER }
        assertEquals("p1", encQueue?.dependsOn)
    }

    @Test
    fun W11_save_rollsBackEverything_whenAuditFails(): Unit = runBlocking {
        val p1 = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(p1)

        val closedDb = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        closedDb.close()

        val badAuditLogger = AuditLogger({ closedDb }, "dev-device", "1.0")
        val badWriter = EncounterWriter({ database }, syncEnqueuer, badAuditLogger)

        val enc = EncounterFixtures.fixtureEncounter(id = "enc1", patientId = "p1")
        val bundle = EncounterFixtures.fixtureBundle(encounter = enc, photos = emptyList())

        assertThrows(RuntimeException::class.java) {
            runBlocking {
                badWriter.saveCompletedEncounter(bundle)
            }
        }

        assertEquals(0, EncounterFixtures.count(database, "encounter"))
        assertEquals(0, EncounterFixtures.count(database, "vital_sign"))
        assertEquals(0, EncounterFixtures.count(database, "sync_queue"))
        assertEquals(0, EncounterFixtures.count(database, "audit_log"))
    }

    @Test
    fun testEncounterWriter_rollbackOnFailure(): Unit = runBlocking {
        val p1 = EncounterFixtures.fixturePatient(id = "p1")
        database.patientDao().upsert(p1)

        val enc = EncounterFixtures.fixtureEncounter(
            id = "enc2",
            patientId = "non_existent_patient",
            syncStatus = SyncStatus.SYNCED
        )
        val bundle = EncounterFixtures.fixtureBundle(
            encounter = enc,
            photos = emptyList()
        )

        assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
            runBlocking {
                encounterWriter.saveCompletedEncounter(bundle)
            }
        }

        val enc2Check = database.encounterDao().observe("enc2").first()
        assertTrue(enc2Check == null)
        val queueCheck = database.syncDao().observeFiltered("PENDING").first()
        assertTrue(queueCheck.isEmpty())
        val auditCheck = database.auditLogDao().allOrdered()
        assertTrue(auditCheck.isEmpty())
    }
}
