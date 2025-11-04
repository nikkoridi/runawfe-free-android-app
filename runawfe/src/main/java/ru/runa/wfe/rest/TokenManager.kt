package ru.runa.wfe.rest

import ru.runa.wfe.rest.dto.WfeCredentials
import ru.runa.wfe.ui.login.LoginResult

// TODO: where to store it on device
object TokenManager {
    var token: String? = ""

    fun clear() {
        token = ""
    }

    fun isNotEmpty(): Boolean {
        return !token.isNullOrBlank()
    }

    suspend fun login(credentials: WfeCredentials): LoginResult {
        return try {
            val response = ApiClient.authService.basic(credentials)
            if (response.isSuccessful) {
                token = response.body().toString()
                LoginResult.Success
            }
            else {
                when (response.code()) {
                    401 -> LoginResult.Error("Unauthorized")
                    404 -> LoginResult.Error("Not found")
                    else -> LoginResult.Error("Undefined response error")
                }
            }
        } catch (exception: Exception) {
            LoginResult.Error("Unknown error: ${exception.localizedMessage}")
        } finally {
            credentials.login = ""
            credentials.password = ""
        }
    }
}