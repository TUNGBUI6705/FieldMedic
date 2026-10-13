package com.example.fieldmedic.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.AllergyRecordStatus
import com.example.fieldmedic.data.local.entity.AllergySeverity
import com.example.fieldmedic.data.local.entity.Gender
import com.example.fieldmedic.data.local.entity.Patient
import com.example.fieldmedic.data.local.entity.PatientAllergy
import com.example.fieldmedic.data.local.entity.SyncMeta
import com.example.fieldmedic.data.local.entity.Village
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
class PatientAndCatalogDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var patientDao: PatientDao
    private lateinit var allergyDao: PatientAllergyDao
    private lateinit var villageDao: VillageDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        patientDao = database.patientDao()
        allergyDao = database.patientAllergyDao()
        villageDao = database.villageDao()

        runBlocking {
            // Fix: Insert required Village records to satisfy foreign key constraints for tests using villageId
            villageDao.upsertAll(
                listOf(
                    Village(id = "v1", stationId = "st1", name = "Thôn 1"),
                    Village(id = "v2", stationId = "st1", name = "Thôn 2")
                )
            )
        }
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testPagingList_searchByName_found() = runBlocking {
        val name = "Nguyễn Văn A"
        val patient = Patient(
            id = "p1",
            patientCode = "PAT001",
            stationId = "st1",
            fullName = name,
            searchName = normalizeSearch(name),
            birthYear = 1990,
            gender = Gender.MALE,
            ownerStaffId = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        patientDao.upsert(patient)

        val pagingSourceFound = patientDao.pagingList(q = "nguyen", villageId = null)
        val resultFound = pagingSourceFound.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page
        assertEquals(1, resultFound.data.size)
        assertEquals("p1", resultFound.data[0].id)
    }

    @Test
    fun testPagingList_searchByName_notFound() = runBlocking {
        val name = "Nguyễn Văn A"
        val patient = Patient(
            id = "p1",
            patientCode = "PAT001",
            stationId = "st1",
            fullName = name,
            searchName = normalizeSearch(name),
            birthYear = 1990,
            gender = Gender.MALE,
            ownerStaffId = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        patientDao.upsert(patient)

        val pagingSourceNotFound = patientDao.pagingList(q = "xyz", villageId = null)
        val resultNotFound = pagingSourceNotFound.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page
        assertTrue(resultNotFound.data.isEmpty())
    }

    @Test
    fun testFindDuplicates_sameVillage() = runBlocking {
        val p1 = Patient(
            id = "p1",
            patientCode = "PAT001",
            stationId = "st1",
            fullName = "Trần Thị B",
            searchName = normalizeSearch("Trần Thị B"),
            birthYear = 1985,
            gender = Gender.FEMALE,
            villageId = "v1",
            ownerStaffId = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        patientDao.upsert(p1)

        val dupsSameVillage = patientDao.findDuplicates("Trần Thị B", 1985, "v1")
        assertEquals(1, dupsSameVillage.size)
        assertEquals("p1", dupsSameVillage[0].id)
    }

    @Test
    fun testFindDuplicates_otherVillage() = runBlocking {
        val p1 = Patient(
            id = "p1",
            patientCode = "PAT001",
            stationId = "st1",
            fullName = "Trần Thị B",
            searchName = normalizeSearch("Trần Thị B"),
            birthYear = 1985,
            gender = Gender.FEMALE,
            villageId = "v1",
            ownerStaffId = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        patientDao.upsert(p1)

        val dupsOtherVillage = patientDao.findDuplicates("Trần Thị B", 1985, "v2")
        assertTrue(dupsOtherVillage.isEmpty())
    }

    @Test
    fun testUpsertPatient_doesNotDeleteChildAllergies() = runBlocking {
        val p = Patient(
            id = "p1",
            patientCode = "PAT001",
            stationId = "st1",
            fullName = "Lê Văn C",
            searchName = normalizeSearch("Lê Văn C"),
            birthYear = 2000,
            gender = Gender.MALE,
            phone = "0901234567",
            ownerStaffId = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        patientDao.upsert(p)

        val allergy = PatientAllergy(
            id = "a1",
            patientId = "p1",
            allergenName = "Penicillin",
            allergyClassCode = "PEN",
            severity = AllergySeverity.SEVERE,
            recordedBy = "staff1",
            recordedAt = 1000L,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        allergyDao.upsert(allergy)

        // Upsert patient again with new phone
        val updatedPatient = p.copy(phone = "0987654321")
        patientDao.upsert(updatedPatient)

        val activeAllergies = allergyDao.observeActive("p1").first()
        assertEquals(1, activeAllergies.size)
        assertEquals("a1", activeAllergies[0].id)
    }

    @Test
    fun testActiveClassCodes_returnsOnlyActiveNonDeleted() = runBlocking {
        val p = Patient(
            id = "p1",
            patientCode = "PAT001",
            stationId = "st1",
            fullName = "Phạm Văn D",
            searchName = normalizeSearch("Phạm Văn D"),
            birthYear = 1995,
            gender = Gender.MALE,
            ownerStaffId = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        patientDao.upsert(p)

        val a1 = PatientAllergy(
            id = "a1",
            patientId = "p1",
            allergenName = "Amoxicillin",
            allergyClassCode = "PEN",
            severity = AllergySeverity.MODERATE,
            status = AllergyRecordStatus.ACTIVE,
            recordedBy = "staff1",
            recordedAt = 1000L,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L, isDeleted = false)
        )
        val a2 = PatientAllergy(
            id = "a2",
            patientId = "p1",
            allergenName = "Aspirin",
            allergyClassCode = "NSAID",
            severity = AllergySeverity.MILD,
            status = AllergyRecordStatus.RESOLVED,
            recordedBy = "staff1",
            recordedAt = 1000L,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L, isDeleted = false)
        )
        val a3 = PatientAllergy(
            id = "a3",
            patientId = "p1",
            allergenName = "Ibuprofen",
            allergyClassCode = "NSAID",
            severity = AllergySeverity.MILD,
            status = AllergyRecordStatus.ACTIVE,
            recordedBy = "staff1",
            recordedAt = 1000L,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L, isDeleted = true)
        )
        allergyDao.upsertAll(listOf(a1, a2, a3))

        val classCodes = allergyDao.activeClassCodes("p1")
        assertEquals(listOf("PEN"), classCodes)
    }

    @Test
    fun testTouchLastEncounter_doesNotDecreaseValue() = runBlocking {
        val p = Patient(
            id = "p1",
            patientCode = "PAT001",
            stationId = "st1",
            fullName = "Hoàng Văn E",
            searchName = normalizeSearch("Hoàng Văn E"),
            birthYear = 1988,
            gender = Gender.MALE,
            ownerStaffId = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        patientDao.upsert(p)

        patientDao.touchLastEncounter("p1", 5000L)
        val pAfterFirstTouch = patientDao.observe("p1").first()
        assertEquals(5000L, pAfterFirstTouch?.lastEncounterAt)

        patientDao.touchLastEncounter("p1", 3000L)
        val pAfterSecondTouch = patientDao.observe("p1").first()
        assertEquals(5000L, pAfterSecondTouch?.lastEncounterAt)
    }
}
