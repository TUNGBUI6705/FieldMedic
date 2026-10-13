package com.example.fieldmedic.data.local.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class KeyGateError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    class Cancelled : KeyGateError("User cancelled")
    class LockedOut : KeyGateError("Biometric locked out")
    class NoCredentialEnrolled : KeyGateError("No credential enrolled")
    class KeyInvalidated : KeyGateError("Key permanently invalidated")
    data class Unknown(override val cause: Throwable) : KeyGateError(cause.message, cause)
}

class DbKeyManager(private val context: Context) {

    companion object {
        const val KEY_ALIAS = "fm_db_wrap"

        // Đặt false để người dùng thêm vân tay mới không làm mất khóa và mất dữ liệu chưa đồng bộ.
        // Đây là đánh đổi ưu tiên tính sẵn sàng dữ liệu (data availability) của ứng dụng offline-first
        // thay vì thu hồi khóa khi có biometric mới (revocation on enrollment change).
        private const val INVALIDATE_ON_BIOMETRIC_ENROLLMENT = true
    }

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    // noBackupFilesDir được sử dụng để lưu khóa mã hóa vì thư mục này tự động bị loại khỏi
    // cơ chế cloud backup (Google Drive backup), ngăn chặn việc file khóa bị đồng bộ lên đám mây
    // một cách không an toàn.
    private val keyFile: File
        get() = File(File(context.noBackupFilesDir, "keys"), "db_key.bin")

    fun isInitialized(): Boolean = keyFile.exists()

    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun getOrCreateKey(): SecretKey {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // 0 = xác thực mỗi lần dùng (authentication required for every use) để đảm bảo
            // mọi thao tác mở khóa CSDL đều phải được người dùng xác nhận bằng sinh trắc học/PIN.
            .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
            .setInvalidatedByBiometricEnrollment(INVALIDATE_ON_BIOMETRIC_ENROLLMENT)

        try {
            builder.setIsStrongBoxBacked(true)
            keyGenerator.init(builder.build())
            return keyGenerator.generateKey()
        } catch (_: Exception) {
            // Nếu StrongBox không khả dụng trên thiết bị, thử tạo khóa chuẩn trong TEE (Trusted Execution Environment)
            builder.setIsStrongBoxBacked(false)
            keyGenerator.init(builder.build())
            return keyGenerator.generateKey()
        }
    }

    suspend fun setup(activity: FragmentActivity): ByteArray {
        val rawKey = ByteArray(32)
        SecureRandom().nextBytes(rawKey)

        val cipher = try {
            Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}").apply {
                init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            }
        } catch (_: KeyPermanentlyInvalidatedException) {
            keyStore.deleteEntry(KEY_ALIAS)
            Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}").apply {
                init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            }
        }

        authenticateAndExecute(activity, BiometricPrompt.CryptoObject(cipher), "Thiết lập bảo mật CSDL", "Xác thực để thiết lập khóa mã hóa CSDL") { encryptedCipher ->
            val iv = encryptedCipher.iv
            val ciphertext = encryptedCipher.doFinal(rawKey)

            keyFile.parentFile?.mkdirs()
            keyFile.outputStream().use { fos ->
                fos.write(iv) // 12 bytes IV
                fos.write(ciphertext)
            }
        }

        return rawKey
    }

    suspend fun unlock(activity: FragmentActivity): ByteArray {
        if (!keyFile.exists()) {
            throw IllegalStateException("Key file not initialized")
        }

        val bytes = keyFile.readBytes()
        val iv = bytes.copyOfRange(0, 12)
        val ciphertext = bytes.copyOfRange(12, bytes.size)

        val secretKey = try {
            getOrCreateKey()
        } catch (_: KeyPermanentlyInvalidatedException) {
            throw KeyGateError.KeyInvalidated()
        }

        val cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}").apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        }

        var rawKey = ByteArray(0)
        authenticateAndExecute(activity, BiometricPrompt.CryptoObject(cipher), "Mở khóa CSDL", "Xác thực để mở khóa cơ sở dữ liệu") { decryptedCipher ->
            rawKey = decryptedCipher.doFinal(ciphertext)
        }

        return rawKey
    }

    private suspend fun authenticateAndExecute(
        activity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject,
        title: String,
        subtitle: String,
        action: (Cipher) -> Unit
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                try {
                    val cipher = result.cryptoObject?.cipher ?: throw IllegalStateException("CryptoObject cipher is null")
                    action(cipher)
                    if (continuation.isActive) continuation.resume(Unit)
                } catch (e: Throwable) {
                    if (continuation.isActive) continuation.resumeWithException(KeyGateError.Unknown(e))
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                val error = when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> KeyGateError.Cancelled()
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> KeyGateError.LockedOut()
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> KeyGateError.NoCredentialEnrolled()
                    else -> KeyGateError.Unknown(Exception("Biometric error [$errorCode]: $errString"))
                }
                if (continuation.isActive) continuation.resumeWithException(error)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo, cryptoObject)

        continuation.invokeOnCancellation {
            try {
                biometricPrompt.cancelAuthentication()
            } catch (_: Exception) {}
        }
    }
}
