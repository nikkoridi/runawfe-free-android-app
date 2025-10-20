package ru.runa.wfe.rest.dto

data class WfeTransition(
    private var id: String,
    private var name: String,
    private var description: String,
    private var nodeFromId: String,
    private var nodeToId: String,
    private var color: String
)