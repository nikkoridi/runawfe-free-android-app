package ru.runa.wfe.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.integration.android.AndroidKeystoreKmsClient
import java.security.KeyStore
import javax.crypto.KeyGenerator

class KeyStoreManager(context: Context) {
    private val keyStore by lazy {
        KeyStore.getInstance(PROVIDER).apply { load(null) }
    }

    val charset by lazy {
        Charsets.UTF_8
    }

    fun initTinkConfig() {
        AeadConfig.register()
    }

    private fun checkKey() {
        AndroidKeystoreKmsClient.getOrGenerateNewAeadKey(MASTERKEY_URI)
        if (!keyStore.containsAlias(ALIAS)) {
            generateKey()
        }
    }

    private val aeadKeysetHandle by lazy {
        AndroidKeysetManager.Builder()
            .withSharedPref(context, "keyset", "keyset_preference")
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri(MASTERKEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
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

    fun encrypt(data: String): String {
        checkKey()
        val ciphertext = aeadKeysetHandle.encrypt(data.toByteArray(charset), ByteArray(0))
        return android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)
    }

    fun decrypt(data: String): String {
        checkKey()
        val ciphertext = android.util.Base64.decode(data, android.util.Base64.NO_WRAP)
        return aeadKeysetHandle.decrypt(ciphertext, ByteArray(0)).toString(charset)
    }

    companion object {
        private const val PROVIDER = "AndroidKeyStore"
        private const val KEY_SIZE = 256
        const val ALIAS = "secureKey"
        const val MASTERKEY_URI = "android-keystore://$ALIAS"
    }
}