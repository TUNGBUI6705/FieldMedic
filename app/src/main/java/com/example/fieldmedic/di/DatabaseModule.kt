package com.example.fieldmedic.di

import android.content.Context
import com.example.fieldmedic.data.local.crypto.DbKeyManager
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.db.DatabaseHolder

class DatabaseModule(private val context: Context) {

    val databaseHolder: DatabaseHolder by lazy {
        DatabaseHolder(context)
    }

    val dbKeyManager: DbKeyManager by lazy {
        DbKeyManager(context)
    }

    fun getDatabase(): AppDatabase {
        return databaseHolder.requireDb()
    }
}
