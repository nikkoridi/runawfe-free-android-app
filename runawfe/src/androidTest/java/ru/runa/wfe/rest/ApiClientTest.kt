package ru.runa.wfe.rest

import android.util.Log
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiClientTest {
    private val scheme = "http://"
    private val badScheme = "ftp://"
    private val notRunaWfeUrl = "www.example.com"
    private val instanceUrl = "10.0.2.2:8080"

    @Before
    fun setUp() {
        // ApiClient writes errors to the log
        mockkStatic(Log::class)
        io.mockk.every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        val apiClintInstance = ApiClient::class.java.getDeclaredField("INSTANCE")
        apiClintInstance.isAccessible = true
        apiClintInstance.set(null, ApiClient)
    }

    @Test
    fun setServerUrl_InvalidUrl() {
        val currentUrl = ApiClient.baseUrl
        val testingCheckResult = ServerCheckResult.Invalid
        ApiClient.setServerUrl(testingCheckResult)
        assertEquals(ApiClient.baseUrl, currentUrl)
    }

    @Test
    fun setServerUrl_NetworkErrorUrl() {
        val currentUrl = ApiClient.baseUrl
        val testingCheckResult = ServerCheckResult.NetworkError
        ApiClient.setServerUrl(testingCheckResult)
        assertEquals(currentUrl, ApiClient.baseUrl)
    }

    @Test
    fun setServerUrl_ValidUrl() {
        val testingCheckResult = ServerCheckResult.Valid(scheme + instanceUrl)
        ApiClient.setServerUrl(testingCheckResult)
        assertTrue(ApiClient.isApiClientInitialized())
    }

    @Test
    fun toOrigin_WrongScheme() {
        val badSchemeUrl = badScheme + instanceUrl
        val baseUrl = ApiClient.toOrigin(badSchemeUrl)
        assertEquals("", baseUrl)
    }

    @Test
    fun toOrigin_LocalhostPort() {
        val localhostPortUrl = "http://10.0.2.2:8080"
        val baseUrl = ApiClient.toOrigin(localhostPortUrl)
        assertEquals(localhostPortUrl, baseUrl)
    }

    @Test
    fun toOrigin_CutAdditional() {
        val longUrl = "$scheme$instanceUrl/?someLong=1&arguments=that&functionShouldCut"
        val baseUrl = ApiClient.toOrigin(longUrl)
        assertEquals(scheme + instanceUrl, baseUrl)
    }

    // Note: this test fails without an internet connection. The ServerCheckResult will be "NetworkError"
    @Test
    fun checkServer_NoScheme() = runTest {
        var testingUrl = instanceUrl
        var checkResult = ApiClient.checkServer(testingUrl)
        assertEquals(ServerCheckResult.Invalid, checkResult)

        testingUrl = notRunaWfeUrl
        checkResult = ApiClient.checkServer(testingUrl)
        assertEquals(ServerCheckResult.Invalid, checkResult)
    }

    @Test
    fun checkServer_WrongScheme() = runTest {
        val testingUrl = badScheme + instanceUrl
        val checkResult = ApiClient.checkServer(testingUrl)
        assertEquals(ServerCheckResult.Invalid, checkResult)
    }

    // Note: this test fails without an internet connection. The ServerCheckResult will be "NetworkError"
    @Test
    fun checkServer_NotRunaUrl() = runTest {
        val testingUrl = scheme + notRunaWfeUrl
        val checkResult = ApiClient.checkServer(testingUrl)
        assertEquals(ServerCheckResult.Invalid, checkResult)
    }

    // Note: this test fails without an internet connection. The ServerCheckResult will be "NetworkError"
    @Test
    fun checkServer_CorrectUrl() = runTest {
        val testingUrl = scheme + instanceUrl
        val checkResult = ApiClient.checkServer(testingUrl)
        assertEquals(ServerCheckResult.Valid(testingUrl), checkResult)
    }
}