package ru.runa.wfe.rest.dto

open class WfeExecutor(
    private var id: Long = 0,
    private var type: Type? = null,
    private var name: String = "",
    private var description: String = "",
    private var fullName: String = "") {

    // It is more complex in ru.runa.wfe.rest.dto
    enum class Type(var value: String) {
        EXECUTOR("EXECUTOR"),
        USER("USER"),
        GROUP("GROUP"),
        TEMPORARY_GROUP("TEMPORARY_GROUP"),
        ESCALATION_GROUP("ESCALATION_GROUP")
    }
}