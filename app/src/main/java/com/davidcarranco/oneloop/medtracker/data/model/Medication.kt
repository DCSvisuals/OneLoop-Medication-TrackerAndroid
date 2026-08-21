package com.davidcarranco.oneloop.medtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.max

@Serializable
data class Medication(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    @Serializable(with = IsoInstantSerializer::class)
    val startDate: Instant = Instant.now(),
    val dosesPerDay: Int,
    val doseAmount: Double,
    val doseUnit: DoseUnit,
    @Serializable(with = IsoInstantSerializer::class)
    val firstDoseTime: Instant,
    val intervalHours: Int,
    val form: Form = Form.PILL,
    val status: DoseStatus = DoseStatus.UPCOMING,
    val doses: List<Dose> = emptyList(),
    val hasScheduleChange: Boolean = false,
    val scheduleChangeAfterDays: Int = 3,
    val dosesPerDayAfterChange: Int = 2,
    val intervalHoursAfterChange: Int = 12,
) {
    @Serializable
    data class Dose(
        val id: String = UUID.randomUUID().toString(),
        val number: Int,
        val status: DoseStatus = DoseStatus.UPCOMING,
        @Serializable(with = IsoInstantSerializer::class)
        val snoozedUntil: Instant? = null,
    )

    @Serializable
    enum class DoseUnit(val rawValue: String) {
        @SerialName("mg")
        MG("mg"),
        @SerialName("g")
        GRAMS("g"),
        @SerialName("mL")
        ML("mL"),
        @SerialName("units")
        UNITS("units");

        companion object {
            fun fromRaw(value: String): DoseUnit =
                entries.firstOrNull { it.rawValue.equals(value, ignoreCase = true) } ?: MG
        }
    }

    @Serializable
    enum class Form(val rawValue: String) {
        @SerialName("Pill")
        PILL("Pill"),
        @SerialName("Injection")
        INJECTION("Injection"),
        @SerialName("Ointment / cream")
        CREAM("Ointment / cream");

        companion object {
            fun fromRaw(value: String): Form =
                entries.firstOrNull { it.rawValue.equals(value, ignoreCase = true) } ?: PILL
        }
    }

    val dosage: String
        get() {
            val formatted = if (doseAmount == doseAmount.toLong().toDouble()) {
                doseAmount.toLong().toString()
            } else {
                String.format("%.1f", doseAmount)
            }
            return "$formatted ${doseUnit.rawValue}"
        }

    val instructions: String
        get() = if (hasScheduleChange) {
            val startLabel = if (dosesPerDay == 1) "dose" else "doses"
            val daysLabel = if (scheduleChangeAfterDays == 1) "day" else "days"
            val afterLabel = if (dosesPerDayAfterChange == 1) "dose" else "doses"
            "$dosesPerDay $startLabel daily for $scheduleChangeAfterDays $daysLabel, then $dosesPerDayAfterChange $afterLabel daily"
        } else {
            val doseLabel = if (dosesPerDay == 1) "dose" else "doses"
            "$dosesPerDay $doseLabel per day • Every $intervalHours hours"
        }

    val completedDoseCount: Int
        get() = doses.count { it.status == DoseStatus.TAKEN }

    val isCompleteToday: Boolean
        get() = doses.isNotEmpty() && completedDoseCount == doses.size

    fun dayNumber(on: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int {
        val firstDay = startDate.atZone(zone).toLocalDate()
        val elapsed = java.time.temporal.ChronoUnit.DAYS.between(firstDay, on).toInt()
        return max(1, elapsed + 1)
    }

    fun dosesPerDay(on: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int {
        if (!hasScheduleChange) return dosesPerDay
        return if (dayNumber(on, zone) > scheduleChangeAfterDays) {
            dosesPerDayAfterChange
        } else {
            dosesPerDay
        }
    }

    fun intervalHours(on: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int {
        if (!hasScheduleChange) return intervalHours
        return if (dayNumber(on, zone) > scheduleChangeAfterDays) {
            intervalHoursAfterChange
        } else {
            intervalHours
        }
    }

    fun isActive(on: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        return !on.isBefore(startDate.atZone(zone).toLocalDate())
    }

    /**
     * @param doseNumber zero-based index (0 = first dose).
     */
    fun doseTime(
        doseNumber: Int,
        on: LocalDate,
        intervalHours: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Instant {
        val timeParts = firstDoseTime.atZone(zone)
        val firstDoseOnDay = on.atTime(timeParts.hour, timeParts.minute).atZone(zone)
        return firstDoseOnDay.plusHours((intervalHours * doseNumber).toLong()).toInstant()
    }

    fun scheduledTimes(on: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Instant> {
        if (!isActive(on, zone)) return emptyList()
        val count = dosesPerDay(on, zone)
        val interval = intervalHours(on, zone)
        return (0 until count).map { doseTime(it, on, interval, zone) }
    }

    fun status(forDoseNumber: Int): DoseStatus =
        doses.firstOrNull { it.number == forDoseNumber }?.status ?: DoseStatus.UPCOMING

    fun markDoseTaken(doseNumber: Int): Medication {
        val updated = doses.map { dose ->
            if (dose.number == doseNumber) {
                dose.copy(status = DoseStatus.TAKEN, snoozedUntil = null)
            } else {
                dose
            }
        }
        return copy(doses = updated).syncOverallStatus()
    }

    fun updateDoseStatus(doseNumber: Int, newStatus: DoseStatus): Medication {
        val updated = doses.map { dose ->
            if (dose.number == doseNumber) {
                dose.copy(
                    status = newStatus,
                    snoozedUntil = if (newStatus == DoseStatus.TAKEN) null else dose.snoozedUntil,
                )
            } else {
                dose
            }
        }
        return copy(doses = updated).syncOverallStatus()
    }

    fun snoozeDose(doseNumber: Int, until: Instant): Medication {
        val updated = doses.map { dose ->
            if (dose.number == doseNumber) {
                dose.copy(status = DoseStatus.UPCOMING, snoozedUntil = until)
            } else {
                dose
            }
        }
        return copy(doses = updated).syncOverallStatus()
    }

    fun resetDosesForNewDay(): Medication {
        return copy(
            status = DoseStatus.UPCOMING,
            doses = doses.map { it.copy(status = DoseStatus.UPCOMING, snoozedUntil = null) },
        )
    }

    private fun syncOverallStatus(): Medication {
        val nextStatus = when {
            doses.isNotEmpty() && doses.all { it.status == DoseStatus.TAKEN } -> DoseStatus.TAKEN
            doses.any { it.status == DoseStatus.DUE_NOW } -> DoseStatus.DUE_NOW
            doses.any { it.status == DoseStatus.MISSED } -> DoseStatus.MISSED
            else -> DoseStatus.UPCOMING
        }
        return copy(status = nextStatus)
    }

    companion object {
        fun normalized(
            id: String = UUID.randomUUID().toString(),
            name: String,
            startDate: Instant = Instant.now(),
            dosesPerDay: Int,
            doseAmount: Double,
            doseUnit: DoseUnit,
            firstDoseTime: Instant,
            intervalHours: Int,
            form: Form = Form.PILL,
            status: DoseStatus = DoseStatus.UPCOMING,
            doses: List<Dose>? = null,
            hasScheduleChange: Boolean = false,
            scheduleChangeAfterDays: Int = 3,
            dosesPerDayAfterChange: Int = 2,
            intervalHoursAfterChange: Int = 12,
            zone: ZoneId = ZoneId.systemDefault(),
        ): Medication {
            val safeDosesPerDay = max(1, dosesPerDay)
            val preparedDoses = if (doses != null && doses.size == safeDosesPerDay) {
                doses
            } else {
                (0 until safeDosesPerDay).map { index ->
                    Dose(number = index + 1, status = status)
                }
            }
            val startOfDay = startDate.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
            return Medication(
                id = id,
                name = name,
                startDate = startOfDay,
                dosesPerDay = safeDosesPerDay,
                doseAmount = doseAmount,
                doseUnit = doseUnit,
                firstDoseTime = firstDoseTime,
                intervalHours = max(1, intervalHours),
                form = form,
                status = status,
                doses = preparedDoses,
                hasScheduleChange = hasScheduleChange,
                scheduleChangeAfterDays = max(1, scheduleChangeAfterDays),
                dosesPerDayAfterChange = max(1, dosesPerDayAfterChange),
                intervalHoursAfterChange = max(1, intervalHoursAfterChange),
            )
        }
    }
}
