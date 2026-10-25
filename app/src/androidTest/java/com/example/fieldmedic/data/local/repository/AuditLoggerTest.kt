package com.example.fieldmedic.data.local.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.AuditAction
import com.example.fieldmedic.data.local.entity.AuditResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuditLoggerTest {

    private lateinit var database: AppDatabase
    private lateinit var auditLogger: AuditLogger

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        auditLogger = AuditLogger({ database }, "dev-device", "1.0")
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun A01_auditLogger_concurrentLogs_keepChainIntact(): Unit = runBlocking {
        for (run in 1..5) {
            database.clearAllTables()

            coroutineScope {
                (1..50).map { i ->
                    async(Dispatchers.Default) {
                        auditLogger.log(
                            actorId = "staff1",
                            action = AuditAction.VIEW_RECORD,
                            entityType = "PATIENT",
                            entityId = "p$i",
                            patientId = "p$i",
                            result = AuditResult.OK
                        )
                    }
                }.awaitAll()
            }

            val allLogs = database.auditLogDao().allOrdered()
            assertEquals(50, allLogs.size)

            val seqs = allLogs.map { it.seq.toInt() }.sorted()
            assertEquals((1..50).toList(), seqs)

            val verifyResult = auditLogger.verifyChain()
            assertTrue("Run $run failed: ${verifyResult.brokenAtSeq}", verifyResult.ok)
        }
    }

    @Test
    fun A02_auditHash_coversMetadata(): Unit = runBlocking {
        auditLogger.log(
            actorId = "staff1",
            action = AuditAction.VIEW_RECORD,
            entityType = "PATIENT",
            entityId = "p1",
            patientId = "p1",
            result = AuditResult.OK,
            metadata = mapOf("k" to "v")
        )

        val db = database.openHelper.writableDatabase
        db.execSQL("UPDATE audit_log SET metadata_json = '{\"k\":\"x\"}' WHERE seq = 1")

        val verifyResult = auditLogger.verifyChain()
        assertFalse(verifyResult.ok)
        assertEquals(1, verifyResult.brokenAtSeq)
    }

    @Test
    fun A03a_auditHash_coversDeviceId(): Unit = runBlocking {
        for (i in 1..3) {
            auditLogger.log(
                actorId = "staff1",
                action = AuditAction.VIEW_RECORD,
                entityType = "PATIENT",
                entityId = "p$i",
                patientId = "p$i"
            )
        }

        val db = database.openHelper.writableDatabase
        db.execSQL("UPDATE audit_log SET device_id = 'tampered-device' WHERE seq = 2")

        val verifyResult = auditLogger.verifyChain()
        assertFalse(verifyResult.ok)
        assertEquals(2, verifyResult.brokenAtSeq)
    }

    @Test
    fun A03b_auditHash_coversAppVersion(): Unit = runBlocking {
        for (i in 1..3) {
            auditLogger.log(
                actorId = "staff1",
                action = AuditAction.VIEW_RECORD,
                entityType = "PATIENT",
                entityId = "p$i",
                patientId = "p$i"
            )
        }

        val db = database.openHelper.writableDatabase
        db.execSQL("UPDATE audit_log SET app_version = '9.9' WHERE seq = 2")

        val verifyResult = auditLogger.verifyChain()
        assertFalse(verifyResult.ok)
        assertEquals(2, verifyResult.brokenAtSeq)
    }

    @Test
    fun A04_auditChain_detectsDeletedRow(): Unit = runBlocking {
        for (i in 1..5) {
            auditLogger.log(
                actorId = "staff1",
                action = AuditAction.VIEW_RECORD,
                entityType = "PATIENT",
                entityId = "p$i",
                patientId = "p$i"
            )
        }

        val db = database.openHelper.writableDatabase
        db.execSQL("DELETE FROM audit_log WHERE seq = 3")

        val verifyResult = auditLogger.verifyChain()
        assertFalse(verifyResult.ok)
    }
}
