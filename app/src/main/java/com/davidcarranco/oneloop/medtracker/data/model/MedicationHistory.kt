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
    val name: String,
    val dosage: String,
    val form: Medication.Form,
    val instructions: String,
    @Serializable(with = IsoInstantSerializer::class)
    val startDate: Instant = Instant.EPOCH,
    val dosesPerDay: Int = 1,
    val intervalHours: Int = 24,
    @Serializable(with = IsoInstantSerializer::class)
    val firstDoseTime: Instant = Instant.EPOCH,
    val scheduledTimes: List<@Serializable(with = IsoInstantSerializer::class) Instant> = emptyList(),
    val wasRemovedFromSchedule: Boolean = false,
    @Serializable(with = IsoInstantSerializer::class)
    val recordedAt: Instant = Instant.EPOCH,
    @Serializable(with = IsoInstantSerializer::class)
    val day: Instant = Instant.EPOCH,
    val doses: List<MedicationHistoryDose> = emptyList(),
) {
    val storageKey: String get() = medicationID

    companion object {
        fun storageKey(medicationID: String): String = medicationID
    }
}
