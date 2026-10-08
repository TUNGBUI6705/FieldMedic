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
    fun testDatabaseCreation_containsAll7Tables() {
        // Force DB creation
        val db = database.openHelper.writableDatabase

        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT IN ('android_metadata', 'room_master_table', 'sqlite_sequence')"
        )

        val tableNames = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            tableNames.add(cursor.getString(0))
        }
        cursor.close()

        val expectedTables = setOf(
            "station",
            "village",
            "allergen_catalog",
            "drug_catalog",
            "threshold_set",
            "threshold_rule",
            "staff"
        )

        assertEquals(expectedTables, tableNames)
    }
}
