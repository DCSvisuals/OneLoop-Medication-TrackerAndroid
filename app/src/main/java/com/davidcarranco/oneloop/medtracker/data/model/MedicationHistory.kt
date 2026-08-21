package com.davidcarranco.oneloop.medtracker.data.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class MedicationHistoryDose(
    val id: String = UUID.randomUUID().toString(),
    val doseNumber: Int,
    @Serializable(with = IsoInstantSerializer::class)
    val scheduledTime: Instant,
    val status: DoseStatus,
    @Serializable(with = IsoInstantSerializer::class)
    val recordedAt: Instant? = null,
)

@Serializable
data class MedicationHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val medicationID: String,
    @Serializable(with = IsoInstantSerializer::class)
    val day: Instant,
    val name: String,
    val dosage: String,
    val form: Medication.Form,
    val instructions: String,
    val doses: List<MedicationHistoryDose> = emptyList(),
    val wasRemovedFromSchedule: Boolean = false,
) {
    val storageKey: String get() = medicationID

    companion object {
        fun storageKey(medicationID: String): String = medicationID
    }
}
