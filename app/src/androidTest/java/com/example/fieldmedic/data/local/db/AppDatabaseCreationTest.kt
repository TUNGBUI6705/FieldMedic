package com.example.fieldmedic.data.local.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseCreationTest {

    private lateinit var database: AppDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testDatabaseCreation_containsAll31TablesAnd1View() {
        // Force DB creation
        val db = database.openHelper.writableDatabase

        val tableCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT IN ('android_metadata', 'room_master_table', 'sqlite_sequence')"
        )

        val tableNames = mutableSetOf<String>()
        while (tableCursor.moveToNext()) {
            tableNames.add(tableCursor.getString(0))
        }
        tableCursor.close()

        val expectedTables = setOf(
            "station",
            "village",
            "allergen_catalog",
            "drug_catalog",
            "threshold_set",
            "threshold_rule",
            "staff",
            "patient",
            "patient_allergy",
            "patient_condition",
            "patient_medication",
            "appointment",
            "encounter",
            "vital_sign",
            "medication_given",
            "referral",
            "encounter_addendum",
            "photo",
            "sync_queue",
            "sync_conflict",
            "sync_cursor",
            "record_comment",
            "record_comment_revision",
            "record_member",
            "record_invite",
            "record_lock",
            "record_lock_state",
            "comment_read_state",
            "audit_log",
            "paired_device",
            "error_log",
        )

        assertEquals(expectedTables, tableNames)

        val viewCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='view'"
        )

        val viewNames = mutableSetOf<String>()
        while (viewCursor.moveToNext()) {
            viewNames.add(viewCursor.getString(0))
        }
        viewCursor.close()

        val expectedViews = setOf("v_sync_summary")

        assertEquals(expectedViews, viewNames)
    }
}
