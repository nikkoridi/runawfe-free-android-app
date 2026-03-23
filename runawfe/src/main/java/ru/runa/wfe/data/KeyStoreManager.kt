package ru.runa.wfe.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeyStoreManager {
    private val keyStore by lazy {
        KeyStore.getInstance(PROVIDER).apply { load(null) }
    }

    val charset by lazy {
        Charsets.UTF_8
    }

    private fun checkKey() {
        if (!keyStore.containsAlias(ALIAS)) {
            generateKey()
        }
    }

    private fun generateKey() {
        val generator: KeyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            PROVIDER
        )
        val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setKeySize(KEY_SIZE)
            .build()
        generator.init(parameterSpec)
        generator.generateKey()
    }

    // TODO: Add exception handling
    private fun getSecretKey(): SecretKey {
        checkKey()
        val key = (keyStore.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry)
            .secretKey
        return key
    }

    fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv: ByteArray = cipher.iv
        val ciphertext = cipher.doFinal(data.toByteArray(charset))
        return android.util.Base64.encodeToString(
            iv + ciphertext,
            android.util.Base64.NO_WRAP
        )
    }

    fun decrypt(data: String): String {
        val base64Decoded = android.util.Base64.decode(data, android.util.Base64.NO_WRAP)
        val iv = base64Decoded.take(IV_SIZE).toByteArray()
        val textWithTag = base64Decoded.drop(IV_SIZE).toByteArray()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        if (iv.size != IV_SIZE) {
            Log.e(
                this.javaClass.simpleName,
                "Decrypt error: iv size should be $IV_SIZE, got ${iv.size}"
            )
        } else {
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(GCM_TAG_SIZE * 8, iv))
            return cipher.doFinal(textWithTag).toString(charset)
        }
        return ""
    }

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PROVIDER = "AndroidKeyStore"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 12
    private const val GCM_TAG_SIZE = 16
    const val ALIAS = "secureKey"
}