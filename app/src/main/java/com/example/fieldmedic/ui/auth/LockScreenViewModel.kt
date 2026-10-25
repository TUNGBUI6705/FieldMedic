package com.example.fieldmedic.ui.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fieldmedic.data.local.auth.SessionManager
import com.example.fieldmedic.data.local.crypto.DbKeyManager
import com.example.fieldmedic.data.local.db.DatabaseHolder
import com.example.fieldmedic.data.remote.auth.FirebaseAuthTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LockScreenUiState {
    data class Locked(
        val isInitialized: Boolean,
        val canBiometric: Boolean,
        val errorMessage: String? = null
    ) : LockScreenUiState

    data class Unlocked(
        val staffName: String
    ) : LockScreenUiState
}

class LockScreenViewModel(
    private val dbKeyManager: DbKeyManager,
    private val databaseHolder: DatabaseHolder,
    private val sessionManager: SessionManager,
    private val authTransport: FirebaseAuthTransport? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<LockScreenUiState>(
        LockScreenUiState.Locked(
            isInitialized = dbKeyManager.isInitialized(),
            canBiometric = dbKeyManager.canAuthenticate()
        )
    )
    val uiState: StateFlow<LockScreenUiState> = _uiState.asStateFlow()

    fun checkState() {
        val isInit = dbKeyManager.isInitialized()
        val canBio = dbKeyManager.canAuthenticate()
        val currentStaff = sessionManager.currentStaff.value

        if (sessionManager.isSessionExpired()) {
            _uiState.value = LockScreenUiState.Locked(
                isInitialized = isInit,
                canBiometric = canBio,
                errorMessage = if (currentStaff != null) "Phiên làm việc đã hết hạn. Vui lòng mở khóa lại." else null
            )
        } else if (currentStaff != null) {
            _uiState.value = LockScreenUiState.Unlocked(
                staffName = currentStaff.fullName
            )
        } else {
            _uiState.value = LockScreenUiState.Locked(
                isInitialized = isInit,
                canBiometric = canBio
            )
        }
    }

    fun setupKeyAndUnlock(activity: FragmentActivity) {
        viewModelScope.launch {
            try {
                val rawKey = dbKeyManager.setup(activity)
                databaseHolder.open(rawKey)
                rawKey.fill(0)

                val staffName = sessionManager.currentStaff.value?.fullName ?: "Cán bộ y tế"
                _uiState.value = LockScreenUiState.Unlocked(staffName = staffName)
            } catch (e: Exception) {
                _uiState.value = LockScreenUiState.Locked(
                    isInitialized = dbKeyManager.isInitialized(),
                    canBiometric = dbKeyManager.canAuthenticate(),
                    errorMessage = e.localizedMessage ?: e.toString()
                )
            }
        }
    }

    fun unlockWithBiometric(activity: FragmentActivity) {
        viewModelScope.launch {
            try {
                val rawKey = dbKeyManager.unlock(activity)
                databaseHolder.open(rawKey)
                rawKey.fill(0)

                val staffName = sessionManager.currentStaff.value?.fullName ?: "Cán bộ y tế"
                _uiState.value = LockScreenUiState.Unlocked(staffName = staffName)
            } catch (e: Exception) {
                _uiState.value = LockScreenUiState.Locked(
                    isInitialized = dbKeyManager.isInitialized(),
                    canBiometric = dbKeyManager.canAuthenticate(),
                    errorMessage = e.localizedMessage ?: e.toString()
                )
            }
        }
    }

    fun loginWithEmailPassword(activity: FragmentActivity, email: String, pass: String) {
        if (authTransport == null) return
        viewModelScope.launch {
            try {
                val staff = authTransport.loginAndSyncStaff(email, pass)
                if (staff != null) {
                    sessionManager.startSession(staff)
                    if (dbKeyManager.isInitialized()) {
                        unlockWithBiometric(activity)
                    } else {
                        setupKeyAndUnlock(activity)
                    }
                } else {
                    _uiState.value = LockScreenUiState.Locked(
                        isInitialized = dbKeyManager.isInitialized(),
                        canBiometric = dbKeyManager.canAuthenticate(),
                        errorMessage = "Đăng nhập không thành công. Kiểm tra lại thông tin."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = LockScreenUiState.Locked(
                    isInitialized = dbKeyManager.isInitialized(),
                    canBiometric = dbKeyManager.canAuthenticate(),
                    errorMessage = e.localizedMessage ?: e.toString()
                )
            }
        }
    }

    fun lockApp() {
        databaseHolder.closeAndWipe()
        sessionManager.clearSession()
        _uiState.value = LockScreenUiState.Locked(
            isInitialized = dbKeyManager.isInitialized(),
            canBiometric = dbKeyManager.canAuthenticate()
        )
    }
}
