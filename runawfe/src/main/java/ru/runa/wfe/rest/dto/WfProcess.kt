package ru.runa.wfe.rest.dto

import java.util.Date

data class WfProcess(
    val id: Long,
    val name: String,
    val startDate: Date,
    val endDate: Date,
    val version: Int,
    val archived: Boolean,
    val definitionId: Long,
    val hierarchyIds: String,
    val variables: List<WfeVariable> = emptyList(),
    val executionStatus: ProcessExecutionStatus,
    val errors: String,
    val externalData: Long)