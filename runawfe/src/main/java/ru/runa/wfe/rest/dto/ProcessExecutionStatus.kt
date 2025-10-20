package ru.runa.wfe.rest.dto

// TODO: is it a part of dto, or it needs another package
enum class ProcessExecutionStatus() {
    ACTIVE,
    SUSPENDED,
    FAILED,
    ENDED;

    fun getLabelKey(): String {
        return "process.execution.status." + this.toString().lowercase()
    }
}