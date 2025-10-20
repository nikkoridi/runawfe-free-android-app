package ru.runa.wfe.rest.dto

data class WfePagedList<T>(
    private var total: Int = 0,
    private var data: List<T> = emptyList()
)