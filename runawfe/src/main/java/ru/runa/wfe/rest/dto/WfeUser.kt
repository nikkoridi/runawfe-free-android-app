package ru.runa.wfe.rest.dto

data class WfeUser(
    private var title: String = "",
    private var email: String = "",
    private var phone: String = "",
    private var department: String = "",
    private var code: Long = 0
) : WfeExecutor()