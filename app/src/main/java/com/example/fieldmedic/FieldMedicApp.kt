package com.example.fieldmedic

import android.app.Application
import com.example.fieldmedic.data.remote.worker.SyncWorker

class FieldMedicApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            SyncWorker.enqueuePeriodicSync(this)
        } catch (_: Exception) {}
    }
}
