package com.example.fieldmedic.data.local.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.QueueOperation
import com.example.fieldmedic.data.local.entity.SyncEntityType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncEnqueuerTest {

    private lateinit var database: AppDatabase
    private lateinit var syncEnqueuer: SyncEnqueuer

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        syncEnqueuer = SyncEnqueuer { database }
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun Q01_enqueue_create_withNonZeroBaseVersion_isRejected(): Unit = runBlocking {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                syncEnqueuer.enqueue(
                    entityType = SyncEntityType.PATIENT,
                    entityId = "p1",
                    patientId = "p1",
                    operation = QueueOperation.CREATE,
                    changedFieldsJson = "{}",
                    baseValuesJson = "{}",
                    baseVersion = 1,
                    priority = 1,
                    dependsOn = null
                )
            }
        }

        val queueItems = database.syncDao().observeFiltered("PENDING").first()
        assertTrue(queueItems.isEmpty())

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

        val successQueueItems = database.syncDao().observeFiltered("PENDING").first()
        assertEquals(1, successQueueItems.size)
    }

    @Test
    fun Q02_coalesce_updateUpdate_keepsOldestBaseValues(): Unit = runBlocking {
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "e1",
            patientId = "p1",
            operation = QueueOperation.UPDATE,
            changedFieldsJson = """{"phone":"0902"}""",
            baseValuesJson = """{"phone":"0901"}""",
            baseVersion = 5,
            priority = 1,
            dependsOn = null
        )
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "e1",
            patientId = "p1",
            operation = QueueOperation.UPDATE,
            changedFieldsJson = """{"phone":"0903","address":"B"}""",
            baseValuesJson = """{"phone":"0902","address":"A"}""",
            baseVersion = 6,
            priority = 1,
            dependsOn = null
        )

        val sqliteDb = database.openHelper.readableDatabase
        val cursor = sqliteDb.query("SELECT payload, base_values, base_version FROM sync_queue WHERE entity_id = 'e1'")
        assertTrue(cursor.moveToFirst())
        val payload = cursor.getString(0)
        val baseValues = cursor.getString(1)
        val baseVersion = cursor.getInt(2)
        cursor.close()

        assertEquals("""{"phone":"0903","address":"B"}""", payload)
        assertEquals("""{"phone":"0901","address":"A"}""", baseValues)
        assertEquals(5, baseVersion)
    }

    @Test
    fun Q03_coalesce_neverMergesIntoSendingItem(): Unit = runBlocking {
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "e1",
            patientId = "p1",
            operation = QueueOperation.UPDATE,
            changedFieldsJson = """{"phone":"x"}""",
            baseValuesJson = """{"phone":"a"}""",
            baseVersion = 1,
            priority = 1,
            dependsOn = null
        )

        val pendingList = database.syncDao().observeFiltered("PENDING").first()
        val id1 = pendingList[0].id
        database.syncDao().markInFlight(id1)

        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "e1",
            patientId = "p1",
            operation = QueueOperation.UPDATE,
            changedFieldsJson = """{"address":"y"}""",
            baseValuesJson = """{"address":"b"}""",
            baseVersion = 1,
            priority = 1,
            dependsOn = null
        )

        val allItems = database.syncDao().observeFiltered(null).first()
        assertEquals(2, allItems.size)
        val sendingItem = allItems.find { it.status.name == "SENDING" || it.status.name == "IN_FLIGHT" }
        val pendingItem = allItems.find { it.status.name == "PENDING" }
        assertNotNull(sendingItem)
        assertNotNull(pendingItem)
        assertEquals("""{"phone":"x"}""", sendingItem?.payload)
        assertEquals("""{"address":"y"}""", pendingItem?.payload)
        assertEquals("""{"address":"b"}""", pendingItem?.baseValues)

        // Subsequent update to e1 should merge into the PENDING item
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "e1",
            patientId = "p1",
            operation = QueueOperation.UPDATE,
            changedFieldsJson = """{"address":"z"}""",
            baseValuesJson = """{"address":"y"}""",
            baseVersion = 1,
            priority = 1,
            dependsOn = null
        )

        val allItemsAfter = database.syncDao().observeFiltered(null).first()
        assertEquals(2, allItemsAfter.size)
        val pendingAfter = allItemsAfter.find { it.status.name == "PENDING" }
        assertEquals("""{"address":"z"}""", pendingAfter?.payload)
        assertEquals("""{"address":"b"}""", pendingAfter?.baseValues)
    }

    @Test
    fun Q04_coalesce_updateThenDelete_keepsOldestBase(): Unit = runBlocking {
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "e1",
            patientId = "p1",
            operation = QueueOperation.UPDATE,
            changedFieldsJson = """{"phone":"x"}""",
            baseValuesJson = """{"phone":"a"}""",
            baseVersion = 3,
            priority = 1,
            dependsOn = null
        )
        syncEnqueuer.enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = "e1",
            patientId = "p1",
            operation = QueueOperation.DELETE,
            changedFieldsJson = "{}",
            baseValuesJson = "{}",
            baseVersion = 3,
            priority = 1,
            dependsOn = null
        )

        val sqliteDb = database.openHelper.readableDatabase
        val cursor = sqliteDb.query("SELECT operation, base_values, base_version FROM sync_queue WHERE entity_id = 'e1'")
        assertTrue(cursor.moveToFirst())
        val operation = cursor.getString(0)
        val baseValues = cursor.getString(1)
        val baseVersion = cursor.getInt(2)
        cursor.close()

        assertEquals("DELETE", operation)
        assertEquals("""{"phone":"a"}""", baseValues)
        assertEquals(3, baseVersion)
    }
}
