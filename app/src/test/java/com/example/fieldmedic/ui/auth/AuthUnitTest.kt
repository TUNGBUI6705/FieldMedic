package com.example.fieldmedic.ui.auth

import com.example.fieldmedic.data.local.auth.SessionManager
import com.example.fieldmedic.data.local.entity.Staff
import com.example.fieldmedic.data.local.entity.StaffRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUnitTest {

    private fun createSampleStaff(): Staff {
        return Staff(
            id = "staff1",
            stationId = "st1",
            fullName = "Bác sĩ Nguyễn Văn A",
            role = StaffRole.NURSE,
            updatedAt = 1000L
        )
    }

    @Test
    fun testSessionManager_lifecycleAndTimeout() {
        val sessionManager = SessionManager()
        val staff = createSampleStaff()
        val now = 1_000_000L

        // Initial state
        assertNull(sessionManager.currentStaff.value)
        assertTrue(sessionManager.isSessionExpired(now))

        // Start session
        sessionManager.startSession(staff, now)
        assertEquals("staff1", sessionManager.currentStaff.value?.id)
        assertFalse(sessionManager.isSessionExpired(now))

        // Within 15 minutes (e.g. 10 minutes later) -> not expired
        val tenMinsLater = now + (10 * 60 * 1000L)
        assertFalse(sessionManager.isSessionExpired(tenMinsLater))

        // Touch activity at 10 minutes
        sessionManager.touchActivity(tenMinsLater)

        // 20 minutes from start (10 mins from touch) -> still valid
        val twentyMinsFromStart = now + (20 * 60 * 1000L)
        assertFalse(sessionManager.isSessionExpired(twentyMinsFromStart))

        // 30 minutes from touch (20 mins after touch) -> expired
        val thirtyMinsFromStart = now + (30 * 60 * 1000L)
        assertTrue(sessionManager.isSessionExpired(thirtyMinsFromStart))

        // Clear session
        sessionManager.clearSession()
        assertNull(sessionManager.currentStaff.value)
        assertTrue(sessionManager.isSessionExpired(now))
    }
}
