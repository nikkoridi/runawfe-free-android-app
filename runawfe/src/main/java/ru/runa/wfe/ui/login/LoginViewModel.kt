package ru.runa.wfe.ui.login

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.runa.wfe.R
import ru.runa.wfe.rest.TokenManager
import ru.runa.wfe.rest.dto.WfeCredentials


class LoginViewModel : ViewModel() {
    private val _loginForm = MutableLiveData<LoginResult>()
    val loginFormState: LiveData<LoginResult> = _loginForm

    fun login(login: String, password: String) {
        viewModelScope.launch {
            if (loginValidator(login) && passwordValidator(password)) {
                _loginForm.value = TokenManager.login(WfeCredentials(login, password))
            }
            else {
                _loginForm.value = LoginResult.Error(
                    Resources.getSystem().getString(R.string.empty_form)
                )
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