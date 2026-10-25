package com.example.fieldmedic.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fieldmedic.data.local.auth.SessionManager
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.AllergySeverity
import com.example.fieldmedic.data.local.entity.Gender
import com.example.fieldmedic.data.local.entity.Patient
import com.example.fieldmedic.data.local.entity.Staff
import com.example.fieldmedic.data.local.entity.StaffRole
import com.example.fieldmedic.data.local.entity.SyncMeta
import com.example.fieldmedic.data.local.repository.AuditLogger
import com.example.fieldmedic.data.local.repository.EncounterFixtures
import com.example.fieldmedic.data.local.repository.SyncEnqueuer
import com.example.fieldmedic.domain.model.AllergyInput
import com.example.fieldmedic.domain.model.AppResult
import com.example.fieldmedic.domain.repository.ClinicalHistoryRepository
import com.example.fieldmedic.domain.repository.CommentRepository
import com.example.fieldmedic.util.normalizeSearch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClinicalAndCommentRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var syncEnqueuer: SyncEnqueuer
    private lateinit var auditLogger: AuditLogger
    private lateinit var clinicalRepo: ClinicalHistoryRepository
    private lateinit var commentRepo: CommentRepository

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        sessionManager = SessionManager()
        syncEnqueuer = SyncEnqueuer { database }
        auditLogger = AuditLogger({ database }, "dev-device", "1.0")

        clinicalRepo = ClinicalHistoryRepositoryImpl(
            dbProvider = { database },
            allergyDao = database.patientAllergyDao(),
            conditionDao = database.patientConditionDao(),
            medicationDao = database.patientMedicationDao(),
            syncEnqueuer = syncEnqueuer,
            auditLogger = auditLogger,
            sessionManager = sessionManager
        )

        commentRepo = CommentRepositoryImpl(
            dbProvider = { database },
            commentDao = database.commentDao(),
            readStateDao = database.commentReadStateDao(),
            syncEnqueuer = syncEnqueuer,
            auditLogger = auditLogger,
            sessionManager = sessionManager
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertSamplePatient(id: String = "p1") {
        val p = Patient(
            id = id,
            patientCode = "PAT001",
            stationId = "st1",
            fullName = "Nguyễn Văn Test",
            searchName = normalizeSearch("Nguyễn Văn Test"),
            birthYear = 1990,
            gender = Gender.MALE,
            ownerStaffId = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        database.patientDao().upsert(p)
    }

    private fun loginStaff() {
        val staff = Staff(
            id = "staff1",
            stationId = "st1",
            fullName = "Bác sĩ Test",
            role = StaffRole.NURSE,
            updatedAt = 1000L
        )
        sessionManager.startSession(staff)
    }

    @Test
    fun testUpsertAllergy_notLoggedIn_returnsError() {
        runBlocking {
            insertSamplePatient("p1")
            val input = AllergyInput(allergenName = "Aspirin", severity = AllergySeverity.MILD)

            val result = clinicalRepo.upsertAllergy("p1", input)
            assertTrue(result is AppResult.Error)
        }
    }

    @Test
    fun testUpsertAllergy_happyPath_enqueuesSyncAndAudit() {
        runBlocking {
            insertSamplePatient("p1")
            loginStaff()

            val input = AllergyInput(allergenName = "Penicillin", allergyClassCode = "PEN", severity = AllergySeverity.SEVERE)
            val result = clinicalRepo.upsertAllergy("p1", input)

            assertTrue(result is AppResult.Success)

            val allergies = clinicalRepo.observeAllergies("p1").first()
            assertEquals(1, allergies.size)
            assertEquals("Penicillin", allergies[0].allergenName)

            // Verify sync queue and audit
            assertEquals(1, EncounterFixtures.count(database, "sync_queue"))
            assertEquals(1, EncounterFixtures.count(database, "audit_log"))
        }
    }

    @Test
    fun testAddComment_happyPathAndValidation() {
        runBlocking {
            insertSamplePatient("p1")
            loginStaff()

            // Empty comment text -> validation error
            val invalidResult = commentRepo.addComment("p1", "   ")
            assertTrue(invalidResult is AppResult.Error)

            // Valid comment -> success
            val validResult = commentRepo.addComment("p1", "Theo dõi tình trạng sức khỏe")
            assertTrue(validResult is AppResult.Success)

            val thread = commentRepo.observeThread("p1").first()
            assertEquals(1, thread.size)
            assertEquals("Theo dõi tình trạng sức khỏe", thread[0].body)

            // Verify sync queue and audit log
            assertEquals(1, EncounterFixtures.count(database, "sync_queue"))
            assertEquals(1, EncounterFixtures.count(database, "audit_log"))
        }
    }

    @Test
    fun testMarkThreadRead_happyPath() {
        runBlocking {
            insertSamplePatient("p1")
            loginStaff()

            val result = commentRepo.markThreadRead("p1")
            assertTrue(result is AppResult.Success)

            val unread = commentRepo.observeUnreadCount("p1").first()
            assertEquals(0, unread)
        }
    }
}
