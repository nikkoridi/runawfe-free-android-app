package ru.runa.wfe.rest.dto

data class WfePagedList<T>(
    val total: Int = 0,
    val data: List<T> = emptyList()
)