package com.example.fieldmedic.data.local.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class CryptoUnitTest {

    private class MockKeyProvider : StationKeyProvider {
        private val fKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        private val hKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        override fun fieldKey(): ByteArray = fKey
        override fun hmacKey(): ByteArray = hKey
    }

    @Test
    fun testFieldCrypto_encryptDecryptAndBlindIndex() {
        val provider = MockKeyProvider()
        val fieldCrypto = FieldCrypto(provider)

        val nationalId = "001092012345"
        val encrypted1 = fieldCrypto.encryptNationalId(nationalId)
        val encrypted2 = fieldCrypto.encryptNationalId(nationalId)

        // Decrypt matches original
        assertEquals(nationalId, fieldCrypto.decryptNationalId(encrypted1))
        assertEquals(nationalId, fieldCrypto.decryptNationalId(encrypted2))

        // Two encryptions of same national ID produce different ciphertexts (random IV)
        assertFalse(encrypted1.contentEquals(encrypted2))

        // Blind index is deterministic
        val blind1 = fieldCrypto.blindIndex(nationalId)
        val blind2 = fieldCrypto.blindIndex("  001092012345  ") // with spaces
        val blindDiff = fieldCrypto.blindIndex("001092012346")
        assertEquals(blind1, blind2)
        assertNotEquals(blind1, blindDiff)

        // Last 4
        assertEquals("2345", fieldCrypto.last4(nationalId))
        assertEquals("123", fieldCrypto.last4("123"))
    }

    @Test
    fun testRecordCrypto_lifecycleAndSecurity() {
        val recordCrypto = RecordCrypto()
        val patientId = "pat_001"
        val password = "StrongPassword123!".toCharArray()
        val staffId = "staff_1"
        val now = System.currentTimeMillis()

        // 1. Create lock
        val lock = recordCrypto.createLock(patientId, password, staffId, now)
        assertNotNull(lock)
        assertTrue(recordCrypto.isUnlocked(patientId))

        // 2. Encrypt field
        val plain = "Medical history notes"
        val encrypted = recordCrypto.encryptField(patientId, "encounter", "row_1", "note", plain, lock.dekVersion)
        assertTrue(encrypted.startsWith("ENC1:"))

        // 3. Lock now
        recordCrypto.lockNow(patientId)
        assertFalse(recordCrypto.isUnlocked(patientId))

        // 4. Decrypt field while locked -> throws RecordLockedException
        assertThrows(RecordLockedException::class.java) {
            recordCrypto.decryptField(patientId, "encounter", "row_1", "note", encrypted)
        }

        // 5. Unlock with wrong password -> returns false
        val wrongPassword = "WrongPassword999!".toCharArray()
        assertFalse(recordCrypto.unlock(lock, wrongPassword))
        assertFalse(recordCrypto.isUnlocked(patientId))

        // 6. Unlock with correct password -> returns true
        val correctPassword = "StrongPassword123!".toCharArray()
        assertTrue(recordCrypto.unlock(lock, correctPassword))
        assertTrue(recordCrypto.isUnlocked(patientId))

        // 7. Decrypt field successfully
        val decrypted = recordCrypto.decryptField(patientId, "encounter", "row_1", "note", encrypted)
        assertEquals(plain, decrypted)

        // 8. Decrypt with different rowId (different AAD) -> throws error (AEADBadTagException)
        assertThrows(Exception::class.java) {
            recordCrypto.decryptField(patientId, "encounter", "row_2", "note", encrypted)
        }

        // 9. Change password
        val newPassword1 = "NewSecurePassword456!".toCharArray()
        val updatedLock = recordCrypto.changePassword(lock, "StrongPassword123!".toCharArray(), newPassword1, now + 1000L)
        assertNotNull(updatedLock)

        // Old password no longer works on updated lock
        recordCrypto.lockNow(patientId)
        assertFalse(recordCrypto.unlock(updatedLock!!, "StrongPassword123!".toCharArray()))

        // New password works and decrypts old encrypted data
        val newPassword2 = "NewSecurePassword456!".toCharArray()
        assertTrue(recordCrypto.unlock(updatedLock, newPassword2))
        assertEquals(plain, recordCrypto.decryptField(patientId, "encounter", "row_1", "note", encrypted))

        // 10. Unencrypted raw text returned as is
        val rawText = "Plaintext without prefix"
        assertEquals(rawText, recordCrypto.decryptField(patientId, "encounter", "row_1", "note", rawText))

        // 11. Every encryptField call produces different ciphertext (random IV)
        val enc1 = recordCrypto.encryptField(patientId, "encounter", "row_1", "note", plain, lock.dekVersion)
        val enc2 = recordCrypto.encryptField(patientId, "encounter", "row_1", "note", plain, lock.dekVersion)
        assertNotEquals(enc1, enc2)
    }
}
