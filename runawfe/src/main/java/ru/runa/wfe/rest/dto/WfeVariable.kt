package ru.runa.wfe.rest.dto

data class WfeVariable(
    private var name: String,
    private var type: String, // It is an enum in ru.runa.wfe.rest.dto declaration
    private var format: String,
    private var value: Any
)