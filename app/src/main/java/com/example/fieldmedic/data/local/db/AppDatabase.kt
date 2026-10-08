package com.example.fieldmedic.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.fieldmedic.data.local.entity.AllergenCatalog
import com.example.fieldmedic.data.local.entity.DrugCatalog
import com.example.fieldmedic.data.local.entity.Staff
import com.example.fieldmedic.data.local.entity.Station
import com.example.fieldmedic.data.local.entity.ThresholdRule
import com.example.fieldmedic.data.local.entity.ThresholdSet
import com.example.fieldmedic.data.local.entity.Village

@Database(
    entities = [
        Station::class,
        Village::class,
        AllergenCatalog::class,
        DrugCatalog::class,
        ThresholdSet::class,
        ThresholdRule::class,
        Staff::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        const val NAME = "fieldmedic.db"
    }
}
