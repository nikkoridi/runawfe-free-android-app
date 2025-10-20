package ru.runa.wfe.rest.dto

import java.util.Date

data class WfeTask(
    private var id: Long,
    private var name: String,
    private var nodeId: String,
    private var description: String,
    private var swimlaneName: String,
    private var owner: WfeExecutor,
    private var targetUser: WfeUser,
    private var definitionId: Long,
    private var definitionName: String,
    private var processId: Long,
    private var processHierarchyIds: String,
    private var tokenId: Long,
    private var createDate: Date,
    private var deadlineDate: Date,
    private var deadlineWarningDate: Date,
    private var assignDate: Date,
    private var escalated: Boolean,
    private var firstOpen: Boolean,
    private var acquiredBySubstitution: Boolean,
    private var multitaskIndex: Int,
    private var readOnly: Boolean,
    private var outputTransitions: List<WfeTransition>,
    private var variables: List<WfeVariable>
)