package ru.runa.wfe.rest.dto

import ru.runa.wfe.restapi.model.WfeProcess

data class WfChatRoom(
    var process: WfeProcess? = null,
    var newMessagesCount: Long = 0) {

    fun getId(): Long {
        return process?.id ?: 0
    }
}