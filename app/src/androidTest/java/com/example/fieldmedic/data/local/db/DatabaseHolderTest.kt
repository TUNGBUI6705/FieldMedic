package com.example.fieldmedic.data.local.db

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fieldmedic.data.local.entity.Gender
import com.example.fieldmedic.data.local.entity.Patient
import com.example.fieldmedic.data.local.entity.SyncMeta
import com.example.fieldmedic.util.normalizeSearch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DatabaseHolderTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var holder: DatabaseHolder

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dbFile = context.getDatabasePath(AppDatabase.NAME)
        if (dbFile.exists()) {
            dbFile.delete()
        }
        holder = DatabaseHolder(context)
    }

    @After
    fun tearDown() {
        try {
            holder.closeAndWipe()
        } catch (_: Exception) {}
        if (dbFile.exists()) {
            dbFile.delete()
        }
    }

    @Test
    fun testOpenWithPassphrase_insertsAndReopens() {
        runBlocking {
            val passphraseA = "PassphraseA123!".toByteArray(Charsets.UTF_8)
            val db = holder.open(passphraseA)

            val patient = Patient(
                id = "p1",
                patientCode = "PAT001",
                stationId = "st1",
                fullName = "Nguyễn Văn A",
                searchName = normalizeSearch("Nguyễn Văn A"),
                birthYear = 1990,
                gender = Gender.MALE,
                ownerStaffId = "staff1",
                syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
            )
            db.patientDao().upsert(patient)

            // Close and wipe holder
            holder.closeAndWipe()

            // Reopen with same passphrase A
            val passphraseA2 = "PassphraseA123!".toByteArray(Charsets.UTF_8)
            val dbReopened = holder.open(passphraseA2)
            val retrieved = dbReopened.patientDao().observe("p1").first()

            assertNotNull(retrieved)
            assertEquals("Nguyễn Văn A", retrieved?.fullName)
        }
    }

    @Test
    fun testOpenWithWrongPassphrase_throwsException() {
        val passphraseA = "PassphraseA123!".toByteArray(Charsets.UTF_8)
        runBlocking {
            val db = holder.open(passphraseA)
            db.patientDao().upsert(
                Patient(
                    id = "p1",
                    patientCode = "PAT001",
                    stationId = "st1",
                    fullName = "Test",
                    searchName = "test",
                    birthYear = 1990,
                    gender = Gender.MALE,
                    ownerStaffId = "staff1",
                    syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
                )
            )
        }
        holder.closeAndWipe()

        // Open with wrong passphrase B
        val passphraseB = "WrongPassphrase999!".toByteArray(Charsets.UTF_8)
        assertThrows(Exception::class.java) {
            holder.open(passphraseB)
        }
    }

    @Test
    fun testDatabaseFileIsEncrypted_notPlaintextSQLite() {
        runBlocking {
            val passphraseA = "EncryptedPass123!".toByteArray(Charsets.UTF_8)
            val db = holder.open(passphraseA)
            db.patientDao().upsert(
                Patient(
                    id = "p1",
                    patientCode = "PAT001",
                    stationId = "st1",
                    fullName = "Test",
                    searchName = "test",
                    birthYear = 1990,
                    gender = Gender.MALE,
                    ownerStaffId = "staff1",
                    syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
                )
            )
            holder.closeAndWipe()

            assertTrue(dbFile.exists())

            // Read first 16 bytes of dbFile
            val bytes = ByteArray(16)
            dbFile.inputStream().use { it.read(bytes) }
            val headerString = String(bytes, Charsets.US_ASCII)

            // Must NOT equal standard SQLite header "SQLite format 3\0"
            val standardHeader = "SQLite format 3\u0000"
            assertNotEquals(standardHeader, headerString)
        }
    }

    @Test
    fun testStandardSQLiteDatabase_failsToOpenEncryptedFile() {
        runBlocking {
            val passphraseA = "EncryptedPass123!".toByteArray(Charsets.UTF_8)
            val db = holder.open(passphraseA)
            db.patientDao().upsert(
                Patient(
                    id = "p1",
                    patientCode = "PAT001",
                    stationId = "st1",
                    fullName = "Test",
                    searchName = "test",
                    birthYear = 1990,
                    gender = Gender.MALE,
                    ownerStaffId = "staff1",
                    syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
                )
            )
            holder.closeAndWipe()

            // Try opening with standard android.database.sqlite.SQLiteDatabase
            assertThrows(Exception::class.java) {
                val standardDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                standardDb.rawQuery("SELECT * FROM sqlite_master", null).use { it.moveToFirst() }
                standardDb.close()
            }
        }
    }

    @Test
    fun testRequireDb_throwsWhenNotOpenOrClosed() {
        assertThrows(DatabaseLockedException::class.java) {
            holder.requireDb()
        }

        val passphraseA = "Pass123!".toByteArray(Charsets.UTF_8)
        holder.open(passphraseA)
        holder.closeAndWipe()

        assertThrows(DatabaseLockedException::class.java) {
            holder.requireDb()
        }
    }
}
