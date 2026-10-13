package com.example.fieldmedic.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.util.DebugSeeder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class PatientListBenchmarkTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        // Seed 2,000 patients for test benchmarking
        DebugSeeder.seed(database, patientsCount = 2_000, encountersPerPatient = 2)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun benchmarkPagingListFirstPage() = runBlocking {
        val patientDao = database.patientDao()

        val timeEmpty = measureTimeMillis {
            val pagingSource = patientDao.pagingList(q = "", villageId = null)
            val result = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
            ) as PagingSource.LoadResult.Page
            assertTrue(result.data.isNotEmpty())
        }
        println("BENCHMARK: pagingList(q = \"\") first page loaded in ${timeEmpty}ms")
        assertTrue("First page load time for q = \"\" should be under 100ms (took ${timeEmpty}ms)", timeEmpty < 100)

        val timeNguyen = measureTimeMillis {
            val pagingSource = patientDao.pagingList(q = "nguyen", villageId = null)
            val result = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
            ) as PagingSource.LoadResult.Page
            assertTrue(result.data.size >= 0)
        }
        println("BENCHMARK: pagingList(q = \"nguyen\") first page loaded in ${timeNguyen}ms")
        assertTrue("First page load time for q = \"nguyen\" should be under 100ms (took ${timeNguyen}ms)", timeNguyen < 100)
    }
}
