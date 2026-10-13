package com.example.fieldmedic.data.local.crypto

import com.example.fieldmedic.data.local.entity.RecordLock
import com.example.fieldmedic.data.local.entity.SyncMeta
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class KdfParams(
    val algo: String,
    val iterations: Int
) {
    fun serialize(): String = Json.encodeToString(this)

    companion object {
        fun parse(json: String): KdfParams = Json.decodeFromString(json)
    }
}

class RecordLockedException(val patientId: String) : Exception("Record for patient $patientId is locked")

class RecordCrypto {

    private val dekCache = ConcurrentHashMap<String, ByteArray>()

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        // ĐỐI CHIẾU KHUYẾN NGHỊ HIỆN HÀNH CỦA OWASP PASSWORD STORAGE CHEAT SHEET:
        // OWASP khuyến nghị tối thiểu 600,000 vòng lặp cho PBKDF2-HMAC-SHA256.
        const val DEFAULT_ITERATIONS = 600_000
    }

    private fun deriveKek(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    fun createLock(patientId: String, password: CharArray, staffId: String, now: Long): RecordLock {
        val dek = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iterations = DEFAULT_ITERATIONS
        val kek = deriveKek(password, salt, iterations)

        val dekVersion = 1
        val aad = "dek|$patientId|$dekVersion"

        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(kek, "AES"), GCMParameterSpec(128, iv))
            updateAAD(aad.toByteArray(Charsets.UTF_8))
        }
        val wrappedDekCiphertext = cipher.doFinal(dek)
        val wrappedDek = iv + wrappedDekCiphertext

        // Securely wipe sensitive arrays
        password.fill(' ')
        kek.fill(0)

        dekCache[patientId] = dek

        return RecordLock(
            patientId = patientId,
            enabled = true,
            kdfAlgo = "PBKDF2_SHA256",
            kdfParamsJson = KdfParams("PBKDF2_SHA256", iterations).serialize(),
            kdfSalt = salt,
            wrappedDek = wrappedDek,
            dekVersion = dekVersion,
            createdBy = staffId,
            passwordChangedAt = now,
            syncMeta = SyncMeta(createdAt = now, updatedAt = now)
        )
    }

    fun unlock(lock: RecordLock, password: CharArray): Boolean {
        val kdfParams = KdfParams.parse(lock.kdfParamsJson)
        val kek = deriveKek(password, salt = lock.kdfSalt, iterations = kdfParams.iterations)
        val aad = "dek|${lock.patientId}|${lock.dekVersion}"

        val wrappedDek = lock.wrappedDek
        val iv = wrappedDek.copyOfRange(0, 12)
        val ciphertext = wrappedDek.copyOfRange(12, wrappedDek.size)

        val dek = try {
            val cipher = Cipher.getInstance(ALGORITHM).apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(kek, "AES"), GCMParameterSpec(128, iv))
                updateAAD(aad.toByteArray(Charsets.UTF_8))
            }
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            null
        } finally {
            password.fill(' ')
            kek.fill(0)
        }

        if (dek != null) {
            dekCache[lock.patientId] = dek
            return true
        }
        return false
    }

    fun changePassword(lock: RecordLock, oldPassword: CharArray, newPassword: CharArray, now: Long): RecordLock? {
        val kdfParams = KdfParams.parse(lock.kdfParamsJson)
        val oldKek = deriveKek(oldPassword, salt = lock.kdfSalt, iterations = kdfParams.iterations)
        val aad = "dek|${lock.patientId}|${lock.dekVersion}"

        val wrappedDek = lock.wrappedDek
        val iv = wrappedDek.copyOfRange(0, 12)
        val ciphertext = wrappedDek.copyOfRange(12, wrappedDek.size)

        val dek = try {
            val cipher = Cipher.getInstance(ALGORITHM).apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(oldKek, "AES"), GCMParameterSpec(128, iv))
                updateAAD(aad.toByteArray(Charsets.UTF_8))
            }
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            null
        } finally {
            oldPassword.fill(' ')
            oldKek.fill(0)
        }

        if (dek == null) {
            newPassword.fill(' ')
            return null
        }

        val newSalt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iterations = DEFAULT_ITERATIONS
        val newKek = deriveKek(newPassword, newSalt, iterations)

        val newIv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val newCipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(newKek, "AES"), GCMParameterSpec(128, newIv))
            updateAAD(aad.toByteArray(Charsets.UTF_8))
        }
        val newWrappedDekCiphertext = newCipher.doFinal(dek)
        val newWrappedDek = newIv + newWrappedDekCiphertext

        newPassword.fill(' ')
        newKek.fill(0)

        dekCache[lock.patientId] = dek

        return lock.copy(
            kdfSalt = newSalt,
            wrappedDek = newWrappedDek,
            kdfParamsJson = KdfParams("PBKDF2_SHA256", iterations).serialize(),
            passwordChangedAt = now,
            syncMeta = lock.syncMeta.copy(updatedAt = now)
        )
    }

    fun encryptField(patientId: String, table: String, rowId: String, column: String, plain: String, dekVersion: Int): String {
        val dek = dekCache[patientId] ?: throw RecordLockedException(patientId)
        val aad = "$table|$patientId|$dekVersion|$rowId|$column"

        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(dek, "AES"), GCMParameterSpec(128, iv))
            updateAAD(aad.toByteArray(Charsets.UTF_8))
        }
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = iv + ciphertext
        val base64 = Base64.getEncoder().encodeToString(combined)
        return "ENC1:$dekVersion:$base64"
    }

    fun decryptField(patientId: String, table: String, rowId: String, column: String, stored: String): String {
        if (!stored.startsWith("ENC1:")) {
            return stored
        }

        val parts = stored.split(":")
        if (parts.size != 3) {
            return stored
        }

        val dekVersion = parts[1].toIntOrNull() ?: 1
        val base64Data = parts[2]

        val dek = dekCache[patientId] ?: throw RecordLockedException(patientId)
        val aad = "$table|$patientId|$dekVersion|$rowId|$column"

        val decoded = Base64.getDecoder().decode(base64Data)
        val iv = decoded.copyOfRange(0, 12)
        val ciphertext = decoded.copyOfRange(12, decoded.size)

        val cipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(dek, "AES"), GCMParameterSpec(128, iv))
            updateAAD(aad.toByteArray(Charsets.UTF_8))
        }
        val plainBytes = cipher.doFinal(ciphertext)
        return String(plainBytes, Charsets.UTF_8)
    }

    fun isUnlocked(patientId: String): Boolean = dekCache.containsKey(patientId)

    fun lockNow(patientId: String) {
        dekCache.remove(patientId)?.fill(0)
    }

    fun lockAll() {
        dekCache.values.forEach { it.fill(0) }
        dekCache.clear()
    }
}
