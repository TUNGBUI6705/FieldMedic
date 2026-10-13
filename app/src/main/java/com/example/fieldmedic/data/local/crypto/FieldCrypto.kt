package com.example.fieldmedic.data.local.crypto

import android.content.Context
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface StationKeyProvider {
    fun fieldKey(): ByteArray
    fun hmacKey(): ByteArray
}

// THAY BẰNG khóa trạm lấy từ server sau khi đăng nhập;
// bản này khiến CCCD không giải mã được trên thiết bị khác vì khóa được sinh ngẫu nhiên cục bộ lần đầu.
class DevStationKeyProvider(context: Context) : StationKeyProvider {
    private val keysFile = File(File(context.noBackupFilesDir, "keys"), "station_keys.bin")
    private val fieldKeyBytes: ByteArray
    private val hmacKeyBytes: ByteArray

    init {
        keysFile.parentFile?.mkdirs()
        if (keysFile.exists()) {
            val allBytes = keysFile.readBytes()
            fieldKeyBytes = allBytes.copyOfRange(0, 32)
            hmacKeyBytes = allBytes.copyOfRange(32, 64)
        } else {
            fieldKeyBytes = ByteArray(32).apply { SecureRandom().nextBytes(this) }
            hmacKeyBytes = ByteArray(32).apply { SecureRandom().nextBytes(this) }
            val combined = fieldKeyBytes + hmacKeyBytes
            keysFile.writeBytes(combined)
        }
    }

    override fun fieldKey(): ByteArray = fieldKeyBytes
    override fun hmacKey(): ByteArray = hmacKeyBytes
}

class FieldCrypto(private val keys: StationKeyProvider) {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val AAD_NATIONAL_ID = "national_id"
    }

    fun encryptNationalId(plain: String): ByteArray {
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(keys.fieldKey(), "AES"), GCMParameterSpec(128, iv))
            updateAAD(AAD_NATIONAL_ID.toByteArray(Charsets.UTF_8))
        }
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return iv + ciphertext
    }

    fun decryptNationalId(data: ByteArray): String {
        val iv = data.copyOfRange(0, 12)
        val ciphertext = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(keys.fieldKey(), "AES"), GCMParameterSpec(128, iv))
            updateAAD(AAD_NATIONAL_ID.toByteArray(Charsets.UTF_8))
        }
        val plainBytes = cipher.doFinal(ciphertext)
        return String(plainBytes, Charsets.UTF_8)
    }

    fun blindIndex(nationalId: String): String {
        val normalized = nationalId.replace(Regex("\\s+"), "")
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(keys.hmacKey(), "HmacSHA256"))
        }
        val hashBytes = mac.doFinal(normalized.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun last4(nationalId: String): String {
        val normalized = nationalId.replace(Regex("\\s+"), "")
        return if (normalized.length >= 4) {
            normalized.takeLast(4)
        } else {
            normalized
        }
    }
}
