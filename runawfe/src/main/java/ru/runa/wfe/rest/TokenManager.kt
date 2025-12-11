package ru.runa.wfe.rest

import android.util.Log
import com.google.android.material.snackbar.Snackbar
import ru.runa.wfe.R
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
                return  LoginResult(true)
            }
            else {
                when (response.code()) {
                    401 -> LoginResult(false, R.string.auth_error)
                    404 -> LoginResult(false, R.string.not_found_login)
                    else -> {
                        Log.e("TokenManager", "Undefined response error")
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