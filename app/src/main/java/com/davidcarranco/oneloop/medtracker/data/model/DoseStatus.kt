package com.davidcarranco.oneloop.medtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DoseStatus(val rawValue: String) {
    @SerialName("Due now")
    DUE_NOW("Due now"),

    @SerialName("Upcoming")
    UPCOMING("Upcoming"),

    @SerialName("Taken")
    TAKEN("Taken"),

    @SerialName("Missed")
    MISSED("Missed"),
    ;

    companion object {
        fun fromRaw(value: String): DoseStatus =
            entries.firstOrNull { it.rawValue.equals(value, ignoreCase = true) } ?: UPCOMING
    }
}
