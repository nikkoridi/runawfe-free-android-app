package ru.runa.wfe.data

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Before
import org.junit.Test

class PreferencesManagerTest {
    private lateinit var preferencesManager: PreferencesManager
    private val testKey = stringPreferencesKey("testKey")
    private val defaultValue = "default"
    private val value = "testValue"

    @Before
    fun setUp() {
        preferencesManager = PreferencesManager.getInstance(
            ApplicationProvider.getApplicationContext()
        )
    }

    @After
    fun tearDown() = runTest {
        preferencesManager.clearPreferences()
    }

    @Test
    fun setKeyAndGetValue() = runTest {
        preferencesManager.setKey(testKey, value)
        val getValue = preferencesManager.getValue(testKey, defaultValue)
        assertEquals(value, getValue)
    }

    @Test
    fun getUnsetValue() {
        val wrongKey = stringPreferencesKey("wrongKey")
        val getValue = preferencesManager.getValue(wrongKey, defaultValue)
        assertEquals(defaultValue, getValue)
    }

    @Test
    fun hasKeyTrue() = runTest {
        preferencesManager.setKey(testKey, value)
        val checkKey = preferencesManager.hasKey(testKey)
        assertEquals(true, checkKey)
    }

    @Test
    fun hasKeyFalse() = runTest {
        val wrongKey = stringPreferencesKey("wrongKey")
        val checkKey = preferencesManager.hasKey(wrongKey)
        assertEquals(false, checkKey)
    }

    @Test
    fun setKeyAndGetSecureValue() = runTest {
        val secretText = "secret"
        preferencesManager.setSecureKey(testKey, secretText)

        val secretAsNormalKey = preferencesManager.getValue(testKey, defaultValue)
        assertNotSame(secretText, secretAsNormalKey)

        val readSecret = preferencesManager.getSecureValue(testKey, String::class.java)
        assertEquals(secretText, readSecret)
    }


    @Test
    fun deleteKeyValueAndGetDefault() = runTest {
        preferencesManager.setKey(testKey, value)
        preferencesManager.deleteKeyValue(testKey)
        val deletedValue = preferencesManager.getValue(testKey, defaultValue)
        assertEquals(defaultValue, deletedValue)
    }

    @Test
    fun clearPreferences() = runTest {
        val key1 = stringPreferencesKey("Key1")
        val key2 = stringPreferencesKey("Key2")
        val secretKey = stringPreferencesKey("secretKey")
        preferencesManager.setKey(key1, value)
        preferencesManager.setKey(key2, value)
        preferencesManager.setSecureKey(secretKey, "secret")

        preferencesManager.clearPreferences()

        assertEquals(false, preferencesManager.hasKey(key1))
        assertEquals(false, preferencesManager.hasKey(key2))
        assertEquals(false, preferencesManager.hasKey(secretKey))

        assertEquals(defaultValue, preferencesManager.getValue(key1, defaultValue))
        assertEquals(defaultValue, preferencesManager.getValue(key2, defaultValue), defaultValue)
        assertEquals(null, preferencesManager.getSecureValue(secretKey, String::class.java))
    }
}