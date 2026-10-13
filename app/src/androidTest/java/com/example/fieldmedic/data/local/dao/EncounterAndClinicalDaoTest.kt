package com.example.fieldmedic.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.AlertLevel
import com.example.fieldmedic.data.local.entity.Encounter
import com.example.fieldmedic.data.local.entity.EncounterStatus
import com.example.fieldmedic.data.local.entity.Gender
import com.example.fieldmedic.data.local.entity.MedicationGiven
import com.example.fieldmedic.data.local.entity.Patient
import com.example.fieldmedic.data.local.entity.Photo
import com.example.fieldmedic.data.local.entity.PhotoOwnerType
import com.example.fieldmedic.data.local.entity.PhotoUploadStatus
import com.example.fieldmedic.data.local.entity.SyncMeta
import com.example.fieldmedic.data.local.entity.VitalSign
import com.example.fieldmedic.data.local.entity.VitalSource
import com.example.fieldmedic.data.local.entity.VitalType
import com.example.fieldmedic.util.normalizeSearch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncounterAndClinicalDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var patientDao: PatientDao
    private lateinit var encounterDao: EncounterDao
    private lateinit var vitalDao: VitalDao
    private lateinit var medicationDao: MedicationGivenDao
    private lateinit var photoDao: PhotoDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        patientDao = database.patientDao()
        encounterDao = database.encounterDao()
        vitalDao = database.vitalDao()
        medicationDao = database.medicationGivenDao()
        photoDao = database.photoDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    private suspend fun insertSamplePatient(id: String = "p1") {
        val p = Patient(
            id = id,
            patientCode = "PAT001",
            stationId = "st1",
            fullName = "Nguyễn Văn X",
            searchName = normalizeSearch("Nguyễn Văn X"),
            birthYear = 1990,
            gender = Gender.MALE,
            ownerStaffId = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        patientDao.upsert(p)
    }

    @Test
    fun testEncounterDraftImmutability() = runBlocking {
        insertSamplePatient()
        val enc = Encounter(
            id = "enc1",
            patientId = "p1",
            staffId = "staff1",
            stationId = "st1",
            status = EncounterStatus.DRAFT,
            visitAt = 1000L,
            chiefComplaint = "Sốt",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        encounterDao.upsert(enc)

        // Complete first time -> expect 1 row updated
        val firstComplete = encounterDao.complete("enc1", 2000L, AlertLevel.WARNING, true, false, 2000L)
        assertEquals(1, firstComplete)

        // Complete second time -> expect 0 rows updated (no longer DRAFT)
        val secondComplete = encounterDao.complete("enc1", 2500L, AlertLevel.CRITICAL, true, true, 2500L)
        assertEquals(0, secondComplete)

        // Update draft after completed -> expect 0 rows updated and content unchanged
        val updateAfterComplete = encounterDao.updateDraft("enc1", "Ho", null, null, null, 2, 3000L)
        assertEquals(0, updateAfterComplete)

        val updatedEnc = encounterDao.observe("enc1").first()
        assertNotNull(updatedEnc)
        assertEquals("Sốt", updatedEnc?.chiefComplaint)
        assertEquals(EncounterStatus.COMPLETED, updatedEnc?.status)
    }

    @Test
    fun testVitalDao_observeSeries() = runBlocking {
        insertSamplePatient()
        val enc = Encounter(
            id = "enc1",
            patientId = "p1",
            staffId = "staff1",
            stationId = "st1",
            visitAt = 1000L,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        encounterDao.upsert(enc)

        val v1 = VitalSign(
            id = "v1",
            encounterId = "enc1",
            patientId = "p1",
            type = VitalType.TEMP,
            value = 38.5,
            unit = "°C",
            source = VitalSource.MANUAL,
            thresholdSetVersion = 1,
            measuredAt = 1000L,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        val v2 = VitalSign(
            id = "v2",
            encounterId = "enc1",
            patientId = "p1",
            type = VitalType.PULSE,
            value = 90.0,
            unit = "bpm",
            source = VitalSource.MANUAL,
            thresholdSetVersion = 1,
            measuredAt = 1100L,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        val v3 = VitalSign(
            id = "v3",
            encounterId = "enc1",
            patientId = "p1",
            type = VitalType.TEMP,
            value = 37.8,
            unit = "°C",
            source = VitalSource.MANUAL,
            thresholdSetVersion = 1,
            measuredAt = 1200L,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        vitalDao.upsertAll(listOf(v1, v2, v3))

        val series = vitalDao.observeSeries("p1", listOf(VitalType.TEMP.name)).first()
        assertEquals(2, series.size)
        assertEquals("v1", series[0].id) // measuredAt 1000L
        assertEquals("v3", series[1].id) // measuredAt 1200L
    }

    @Test
    fun testCascadeDeletion_onPatientDelete() = runBlocking {
        insertSamplePatient()
        val enc = Encounter(
            id = "enc1",
            patientId = "p1",
            staffId = "staff1",
            stationId = "st1",
            visitAt = 1000L,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        encounterDao.upsert(enc)

        val v = VitalSign(
            id = "v1",
            encounterId = "enc1",
            patientId = "p1",
            type = VitalType.TEMP,
            value = 37.0,
            unit = "°C",
            source = VitalSource.MANUAL,
            thresholdSetVersion = 1,
            measuredAt = 1000L,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        vitalDao.upsert(v)

        val med = MedicationGiven(
            id = "m1",
            encounterId = "enc1",
            patientId = "p1",
            drugName = "Paracetamol",
            dose = "500mg",
            frequency = "q6h",
            days = 3,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        medicationDao.upsert(med)

        val photo = Photo(
            id = "ph1",
            ownerType = PhotoOwnerType.ENCOUNTER,
            ownerId = "enc1",
            patientId = "p1",
            localPath = "/path/to/img",
            width = 100,
            height = 100,
            sizeBytes = 1024L,
            checksumSha256 = "abc",
            takenAt = 1000L,
            takenBy = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        photoDao.upsert(photo)

        // Real delete patient
        val db = database.openHelper.writableDatabase
        db.execSQL("DELETE FROM patient WHERE id = 'p1'")

        // Verify cascade delete
        val encCheck = encounterDao.observe("enc1").first()
        assertTrue(encCheck == null)

        val pending = photoDao.pendingUploads()
        assertTrue(pending.isEmpty())
    }

    @Test
    fun testCounters_completedAndAlert() = runBlocking {
        insertSamplePatient()
        val e1 = Encounter(
            id = "e1",
            patientId = "p1",
            staffId = "staff1",
            stationId = "st1",
            status = EncounterStatus.COMPLETED,
            visitAt = 1000L,
            alertLevel = AlertLevel.NONE,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        val e2 = Encounter(
            id = "e2",
            patientId = "p1",
            staffId = "staff1",
            stationId = "st1",
            status = EncounterStatus.COMPLETED,
            visitAt = 1500L,
            alertLevel = AlertLevel.WARNING,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        val e3 = Encounter(
            id = "e3",
            patientId = "p1",
            staffId = "staff1",
            stationId = "st1",
            status = EncounterStatus.COMPLETED,
            visitAt = 2000L,
            alertLevel = AlertLevel.CRITICAL,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        encounterDao.upsert(e1)
        encounterDao.upsert(e2)
        encounterDao.upsert(e3)

        val completedCount = encounterDao.countCompletedBetween(500L, 2500L).first()
        assertEquals(3, completedCount)

        val alertCount = encounterDao.countAlertBetween(500L, 2500L).first()
        assertEquals(2, alertCount)
    }

    @Test
    fun testPhotoDao_pendingUploads() = runBlocking {
        insertSamplePatient()
        val pLocal = Photo(
            id = "phL",
            ownerType = PhotoOwnerType.PATIENT_AVATAR,
            ownerId = "p1",
            patientId = "p1",
            localPath = "/local",
            width = 10,
            height = 10,
            sizeBytes = 100L,
            checksumSha256 = "1",
            uploadStatus = PhotoUploadStatus.LOCAL,
            takenAt = 1000L,
            takenBy = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        val pFailed = Photo(
            id = "phF",
            ownerType = PhotoOwnerType.PATIENT_AVATAR,
            ownerId = "p1",
            patientId = "p1",
            localPath = "/local2",
            width = 10,
            height = 10,
            sizeBytes = 100L,
            checksumSha256 = "2",
            uploadStatus = PhotoUploadStatus.FAILED,
            takenAt = 1000L,
            takenBy = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        val pUploaded = Photo(
            id = "phU",
            ownerType = PhotoOwnerType.PATIENT_AVATAR,
            ownerId = "p1",
            patientId = "p1",
            localPath = "/local3",
            remotePath = "/remote3",
            width = 10,
            height = 10,
            sizeBytes = 100L,
            checksumSha256 = "3",
            uploadStatus = PhotoUploadStatus.UPLOADED,
            takenAt = 1000L,
            takenBy = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )

        photoDao.upsertAll(listOf(pLocal, pFailed, pUploaded))

        val pending = photoDao.pendingUploads()
        assertEquals(2, pending.size)
        assertTrue(pending.any { it.id == "phL" })
        assertTrue(pending.any { it.id == "phF" })
        assertTrue(pending.none { it.id == "phU" })
    }
}
