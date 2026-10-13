package com.example.fieldmedic.data.local.db

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface DbState {
    object Locked : DbState
    object Open : DbState
}

class DatabaseHolder(private val context: Context) {

    private val _state = MutableStateFlow<DbState>(DbState.Locked)
    val state: StateFlow<DbState> = _state.asStateFlow()

    private var databaseInstance: AppDatabase? = null

    @Synchronized
    fun open(passphrase: ByteArray): AppDatabase {
        databaseInstance?.let { existing ->
            if (_state.value is DbState.Open) {
                return existing
            }
        }

        System.loadLibrary("sqlcipher")
        disableSqlCipherLogsInRelease()

        val passphraseCopy = passphrase.clone()
        try {
            val factory = SupportOpenHelperFactory(passphraseCopy)
            val db = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
                .openHelperFactory(factory)
                .build()

            // Verify passphrase by opening writable database
            db.openHelper.writableDatabase

            databaseInstance = db
            _state.value = DbState.Open
            return db
        } finally {
            passphraseCopy.fill(0)
            passphrase.fill(0)
        }
    }

    private fun disableSqlCipherLogsInRelease() {
        val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebug) {
            // Chỉ tắt log lớp Java của thư viện, không tắt log JNI/lõi SQLCipher.
            net.zetetic.database. Logger.setTarget(net.zetetic.database.NoopTarget())
        }
    }

    fun requireDb(): AppDatabase {
        val db = databaseInstance
        if (db == null || _state.value !is DbState.Open) {
            throw DatabaseLockedException()
        }
        return db
    }

    @Synchronized
    fun closeAndWipe() {
        try {
            databaseInstance?.close()
        } catch (_: Exception) {}
        databaseInstance = null
        _state.value = DbState.Locked
    }
}
