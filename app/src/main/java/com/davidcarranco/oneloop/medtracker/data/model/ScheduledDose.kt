package com.davidcarranco.oneloop.medtracker.data.model

import java.time.Instant

data class ScheduledDose(
    val medication: Medication,
    val doseNumber: Int,
    val scheduledTime: Instant,
    val status: DoseStatus,
) {
    val id: String get() = "${medication.id}-$doseNumber"
}
