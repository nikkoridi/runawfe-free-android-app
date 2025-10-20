package ru.runa.wfe.rest.dto

data class WfChatRoom(
    var process: WfProcess? = null,
    var newMessagesCount: Long = 0) {

    fun getId(): Long {
        return process?.id ?: 0
    }
}