package com.example.fieldmedic.data.local.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.AlertLevel
import com.example.fieldmedic.data.local.entity.AuditAction
import com.example.fieldmedic.data.local.entity.AuditResult
import com.example.fieldmedic.data.local.entity.Encounter
import com.example.fieldmedic.data.local.entity.EncounterStatus
import com.example.fieldmedic.data.local.entity.Gender
import com.example.fieldmedic.data.local.entity.Patient
import com.example.fieldmedic.data.local.entity.Photo
import com.example.fieldmedic.data.local.entity.PhotoOwnerType
import com.example.fieldmedic.data.local.entity.QueueOperation
import com.example.fieldmedic.data.local.entity.SyncEntityType
import com.example.fieldmedic.data.local.entity.SyncMeta
import com.example.fieldmedic.util.normalizeSearch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryTest {

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
    fun testAuditLogger_chainVerificationAndTamperDetection() = runBlocking {
        // Log 5 entries
        for (i in 1..5) {
            auditLogger.log(
                actorId = "staff1",
                action = AuditAction.VIEW_RECORD,
                entityType = "PATIENT",
                entityId = "p$i",
                patientId = "p$i",
                result = AuditResult.OK
            )
        }

        val initialVerify = auditLogger.verifyChain()
        assertTrue(initialVerify.ok)
        assertEquals(null, initialVerify.brokenAtSeq)

        // Tamper with db directly using raw SQL at seq = 3
        val db = database.openHelper.writableDatabase
        db.execSQL("UPDATE audit_log SET result = 'DENIED' WHERE seq = 3")

        val tamperedVerify = auditLogger.verifyChain()
        assertFalse(tamperedVerify.ok)
        assertEquals(3, tamperedVerify.brokenAtSeq)
    }

    @Test
    fun testSyncEnqueuer_coalescingRules() = runBlocking {
        // 1. CREATE then UPDATE -> 1 CREATE item with merged payload
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "p1",
            patientId = "p1",
            operation = QueueOperation.CREATE,
            changedFieldsJson = """{"name":"A"}""",
            baseValuesJson = "{}",
            baseVersion = 1,
            priority = 1,
            dependsOn = null
        )
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "p1",
            patientId = "p1",
            operation = QueueOperation.UPDATE,
            changedFieldsJson = """{"phone":"123"}""",
            baseValuesJson = "{}",
            baseVersion = 1,
            priority = 1,
            dependsOn = null
        )

        var pendingList = database.syncDao().observeFiltered("PENDING").first()
        assertEquals(1, pendingList.size)
        assertEquals(QueueOperation.CREATE, pendingList[0].operation)
        assertTrue(pendingList[0].payload.contains("name"))
        assertTrue(pendingList[0].payload.contains("phone"))

        // Clear queue
        database.syncDao().delete(pendingList[0].id)

        // 2. CREATE then DELETE -> 0 items (removed)
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "p2",
            patientId = "p2",
            operation = QueueOperation.CREATE,
            changedFieldsJson = """{"name":"B"}""",
            baseValuesJson = "{}",
            baseVersion = 1,
            priority = 1,
            dependsOn = null
        )
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "p2",
            patientId = "p2",
            operation = QueueOperation.DELETE,
            changedFieldsJson = "{}",
            baseValuesJson = "{}",
            baseVersion = 1,
            priority = 1,
            dependsOn = null
        )
        pendingList = database.syncDao().observeFiltered("PENDING").first()
        assertTrue(pendingList.isEmpty())

        // 3. UPDATE then DELETE -> 1 DELETE item
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "p3",
            patientId = "p3",
            operation = QueueOperation.UPDATE,
            changedFieldsJson = """{"name":"C"}""",
            baseValuesJson = "{}",
            baseVersion = 1,
            priority = 1,
            dependsOn = null
        )
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "p3",
            patientId = "p3",
            operation = QueueOperation.DELETE,
            changedFieldsJson = "{}",
            baseValuesJson = "{}",
            baseVersion = 1,
            priority = 1,
            dependsOn = null
        )
        pendingList = database.syncDao().observeFiltered("PENDING").first()
        assertEquals(1, pendingList.size)
        assertEquals(QueueOperation.DELETE, pendingList[0].operation)

        database.syncDao().delete(pendingList[0].id)

        // 4. Two different entities -> 2 items
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "p4",
            patientId = "p4",
            operation = QueueOperation.CREATE,
            changedFieldsJson = "{}",
            baseValuesJson = "{}",
            baseVersion = 1,
            priority = 1,
            dependsOn = null
        )
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "p5",
            patientId = "p5",
            operation = QueueOperation.CREATE,
            changedFieldsJson = "{}",
            baseValuesJson = "{}",
            baseVersion = 1,
            priority = 1,
            dependsOn = null
        )
        pendingList = database.syncDao().observeFiltered("PENDING").first()
        assertEquals(2, pendingList.size)
    }

    @Test
    fun testEncounterWriter_success() = runBlocking {
        // Insert patient first
        val p = Patient(
            id = "p1",
            patientCode = "PAT001",
            stationId = "st1",
            fullName = "Trần Văn T",
            searchName = normalizeSearch("Trần Văn T"),
            birthYear = 1990,
            gender = Gender.MALE,
            ownerStaffId = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        database.patientDao().upsert(p)

        val enc = Encounter(
            id = "enc1",
            patientId = "p1",
            staffId = "staff1",
            stationId = "st1",
            status = EncounterStatus.DRAFT,
            visitAt = 1000L,
            alertLevel = AlertLevel.WARNING, // alert -> priority 0
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        val photo = Photo(
            id = "ph1",
            ownerType = PhotoOwnerType.ENCOUNTER,
            ownerId = "enc1",
            patientId = "p1",
            localPath = "/path",
            width = 10,
            height = 10,
            sizeBytes = 100L,
            checksumSha256 = "123",
            takenAt = 1000L,
            takenBy = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )

        val bundle = EncounterBundle(
            encounter = enc,
            vitals = emptyList(),
            medications = emptyList(),
            referral = null,
            photos = listOf(photo),
            patientIsStillPending = true
        )

        encounterWriter.saveCompletedEncounter(bundle)

        val savedEnc = database.encounterDao().observe("enc1").first()
        assertNotNull(savedEnc)
        assertEquals(EncounterStatus.COMPLETED, savedEnc?.status)

        val queueItems = database.syncDao().observeFiltered("PENDING").first()
        assertEquals(2, queueItems.size) // Encounter + Photo
        val encQueue = queueItems.find { it.entityType == SyncEntityType.ENCOUNTER }
        assertNotNull(encQueue)
        assertEquals(0, encQueue?.priority) // ALERT_ENCOUNTER priority 0
        assertEquals("p1", encQueue?.dependsOn) // depends on patient

        val auditLast = database.auditLogDao().lastEntry()
        assertNotNull(auditLast)
    }

    @Test
    fun testEncounterWriter_rollbackOnFailure() = runBlocking {
        val p = Patient(
            id = "p1",
            patientCode = "PAT001",
            stationId = "st1",
            fullName = "Trần Văn T",
            searchName = normalizeSearch("Trần Văn T"),
            birthYear = 1990,
            gender = Gender.MALE,
            ownerStaffId = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        database.patientDao().upsert(p)

        val enc = Encounter(
            id = "enc2",
            patientId = "non_existent_patient", // violates FK
            staffId = "staff1",
            stationId = "st1",
            status = EncounterStatus.DRAFT,
            visitAt = 1000L,
            alertLevel = AlertLevel.WARNING,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        val bundle = EncounterBundle(
            encounter = enc,
            vitals = emptyList(),
            medications = emptyList(),
            referral = null,
            photos = emptyList(),
            patientIsStillPending = true
        )

        assertThrows(Exception::class.java) {
            runBlocking {
                encounterWriter.saveCompletedEncounter(bundle)
            }
        }

        // Verify nothing was written (transaction rolled back)
        val enc2Check = database.encounterDao().observe("enc2").first()
        assertTrue(enc2Check == null)
        val queueCheck = database.syncDao().observeFiltered("PENDING").first()
        assertTrue(queueCheck.isEmpty())
        val auditCheck = database.auditLogDao().allOrdered()
        assertTrue(auditCheck.isEmpty())
    }
}
