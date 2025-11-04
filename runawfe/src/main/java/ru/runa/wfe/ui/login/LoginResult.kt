package ru.runa.wfe.ui.login

sealed class LoginResult {
    data object Success: LoginResult()
    data class Error(val message: String): LoginResult()
}