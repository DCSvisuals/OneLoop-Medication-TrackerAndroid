package com.davidcarranco.oneloop.medtracker.data.model

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class WidgetMedicationData(
    val medicationName: String,
    val dosage: String,
    @Serializable(with = IsoInstantSerializer::class)
    val reminderTime: Instant,
    val completedCount: Int,
    val totalCount: Int,
    val allMedicationsTaken: Boolean,
) {
    val progressText: String get() = "$completedCount of $totalCount taken"

    val progress: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()

    companion object {
        val empty = WidgetMedicationData(
            medicationName = "No medications",
            dosage = "Add a medication in OneLoop",
            reminderTime = Instant.now(),
            completedCount = 0,
            totalCount = 0,
            allMedicationsTaken = false,
        )
    }
}
