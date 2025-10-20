package ru.runa.wfe.rest.dto

import java.util.Date

data class WfeChatMessage(
    var id: Long = 0,
    var text: String = "",
    var files: List<ChatMessageFileDetailDto> = emptyList(),
    var author: String = "",
    var createDate: Date? = null // TODO: check is it ok
)