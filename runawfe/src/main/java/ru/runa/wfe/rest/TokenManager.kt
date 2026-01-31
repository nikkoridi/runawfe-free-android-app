package ru.runa.wfe.rest

import android.util.Log
import com.google.gson.JsonParser
import ru.runa.wfe.PreferencesManager
import ru.runa.wfe.R
import ru.runa.wfe.rest.dto.WfeCredentials
import ru.runa.wfe.ui.login.LoginResult
import java.time.Instant

object TokenManager {
    private var token: String = ""

    fun getToken(): String {
        return token
    }

    fun clearToken() {
        token = ""
    }

    private fun getTokenPayloadString(token: String): String {
        val tokenParts = token.split(".")
        if (tokenParts.size >= 2)
            return String(java.util.Base64.getUrlDecoder().decode(tokenParts[1]), Charsets.UTF_8)
        return ""
    }

    private fun checkExpiration(payload: String): Boolean {
        val jsonPayload = JsonParser().parse(payload).asJsonObject
        val expiration = jsonPayload.get("exp")
        if (expiration != null) {
            val currentTime = Instant.now().epochSecond
            val expirationTime = expiration.toString().toLongOrNull()
            if (expirationTime != null && expirationTime > currentTime) {
                return true
            }
        }
        return false
    }

    private fun checkToken(token: String = getToken()): Boolean {
        val payload = getTokenPayloadString(token)
        return payload.isNotEmpty() && checkExpiration(payload)
    }

    suspend fun loadToken(preferencesManager: PreferencesManager): Boolean {
        try {
            val token = preferencesManager
                .getSecureValue(PreferencesManager.TOKEN, String::class.java)
            val check = checkToken(token)
            return check
        } catch (ex: Exception) {
            Log.e(this.javaClass.simpleName, ex.message.toString())
        }
        return false
    }

    suspend fun requestToken(credentials: WfeCredentials): LoginResult {
        return try {
            val response = ApiClient.authService.basic(credentials)
            if (response.isSuccessful) {
                val token = response.body().toString()
                if (checkToken(token)) {
                    this.token = token
                    return LoginResult(true)
                }
                else {
                    return LoginResult(false, R.string.token_error)
                }
            }
            else {
                when (response.code()) {
                    401 -> LoginResult(false, R.string.auth_error)
                    404 -> LoginResult(false, R.string.not_found_login)
                    else -> {
                        Log.e(this.javaClass.simpleName, "Undefined response error")
                        LoginResult(false, R.string.undefined_error)
                    }
                }
            }
        } catch (exception: Exception) {
            Log.e("TokenManager", "Unknown error: ${exception.localizedMessage}")
            LoginResult(false, R.string.undefined_error)
        } finally {
            credentials.login = ""
            credentials.password = ""
        }
    }
}