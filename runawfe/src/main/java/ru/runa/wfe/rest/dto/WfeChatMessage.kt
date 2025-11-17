package ru.runa.wfe.rest.dto

import java.util.Date

data class WfeChatMessage(
    var id: Long = 0,
    var text: String = "",
    var files: List<ChatMessageFileDetailDto> = emptyList(),
    var author: String = "",
    val createDate: Date
)