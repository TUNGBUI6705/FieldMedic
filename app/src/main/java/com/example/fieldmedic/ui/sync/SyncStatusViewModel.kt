package com.example.fieldmedic.ui.sync

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fieldmedic.data.local.db.DatabaseHolder
import com.example.fieldmedic.data.local.view.SyncSummary
import com.example.fieldmedic.data.remote.worker.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SyncStatusViewModel(
    private val databaseHolder: DatabaseHolder
) : ViewModel() {

    private val _syncSummary = MutableStateFlow<SyncSummary?>(null)
    val syncSummary: StateFlow<SyncSummary?> = _syncSummary.asStateFlow()

    fun startObserving() {
        viewModelScope.launch {
            try {
                val db = databaseHolder.requireDb()
                db.syncDao().observeSummary().collect { summary ->
                    _syncSummary.value = summary
                }
            } catch (_: Exception) {
                _syncSummary.value = null
            }
        }
    }

    fun triggerManualSync(context: Context) {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(context).enqueue(syncRequest)
    }
}
