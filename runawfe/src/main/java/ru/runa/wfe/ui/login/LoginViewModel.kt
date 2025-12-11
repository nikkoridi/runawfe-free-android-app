package ru.runa.wfe.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.runa.wfe.R
import ru.runa.wfe.rest.TokenManager
import ru.runa.wfe.rest.dto.WfeCredentials

data class LoginResult (
    val success: Boolean = false,
    val error: Int? = null
)

class LoginViewModel : ViewModel() {
    private var _loginForm = MutableStateFlow<LoginResult?>(LoginResult())
    val loginFormState: StateFlow<LoginResult?> = _loginForm

    fun login(login: String, password: String) {
        viewModelScope.launch {
            if (loginValidator(login) && passwordValidator(password)) {
                _loginForm.value = TokenManager.login(WfeCredentials(login, password))
            }
            else {
                _loginForm.value = LoginResult(false, R.string.empty_form)
            }
        }
    }

    private fun loginValidator(login: String): Boolean {
        return login.isNotBlank()
    }

    private fun passwordValidator(password: String): Boolean {
        return password.isNotBlank()
    }

}