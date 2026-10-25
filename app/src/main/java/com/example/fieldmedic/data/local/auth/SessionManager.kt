package com.example.fieldmedic.data.local.auth

import com.example.fieldmedic.data.local.entity.Staff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager {

    companion object {
        const val SESSION_TIMEOUT_MS = 15 * 60 * 1000L
    }

    private val _currentStaff = MutableStateFlow<Staff?>(null)
    val currentStaff: StateFlow<Staff?> = _currentStaff.asStateFlow()

    private var lastActivityTs: Long = System.currentTimeMillis()

    fun startSession(staff: Staff, now: Long = System.currentTimeMillis()) {
        _currentStaff.value = staff
        lastActivityTs = now
    }

    fun touchActivity(now: Long = System.currentTimeMillis()) {
        if (_currentStaff.value != null) {
            lastActivityTs = now
        }
    }

    fun isSessionExpired(now: Long = System.currentTimeMillis()): Boolean {
        if (_currentStaff.value == null) return true
        return (now - lastActivityTs) > SESSION_TIMEOUT_MS
    }

    fun clearSession() {
        _currentStaff.value = null
        lastActivityTs = 0L
    }
}
