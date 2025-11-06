package ru.runa.wfe.rest.dto

import java.util.Date

data class WfeTask(
    val id: Long,
    val name: String,
    val nodeId: String,
    val description: String,
    val swimlaneName: String,
    val owner: WfeExecutor,
    val targetUser: WfeUser,
    val definitionId: Long,
    val definitionName: String,
    val processId: Long,
    val processHierarchyIds: String,
    val tokenId: Long,
    val createDate: Date,
    val deadlineDate: Date,
    val deadlineWarningDate: Date,
    val assignDate: Date,
    val escalated: Boolean,
    val firstOpen: Boolean,
    val acquiredBySubstitution: Boolean,
    val multitaskIndex: Int,
    val readOnly: Boolean,
    val outputTransitions: List<WfeTransition>,
    val variables: List<WfeVariable>
)