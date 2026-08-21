package com.davidcarranco.oneloop.medtracker.data.remote

import com.davidcarranco.oneloop.medtracker.data.model.Medication
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Serializable
data class MedicationRemoteRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("doses_per_day") val dosesPerDay: Int,
    @SerialName("dose_amount") val doseAmount: Double,
    @SerialName("dose_unit") val doseUnit: String,
    @SerialName("first_dose_time") val firstDoseTime: String,
    @SerialName("interval_hours") val intervalHours: Int,
    val form: String,
    @SerialName("has_schedule_change") val hasScheduleChange: Boolean,
    @SerialName("schedule_change_after_days") val scheduleChangeAfterDays: Int,
    @SerialName("doses_per_day_after_change") val dosesPerDayAfterChange: Int,
    @SerialName("interval_hours_after_change") val intervalHoursAfterChange: Int,
) {
    fun asMedication(): Medication {
        val start = runCatching { LocalDate.parse(startDate, DAY_FORMATTER) }
            .getOrDefault(LocalDate.now())
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
        val firstTime = parseInstant(firstDoseTime) ?: Instant.now()
        return Medication.normalized(
            id = id,
            name = name,
            startDate = start,
            dosesPerDay = dosesPerDay,
            doseAmount = doseAmount,
            doseUnit = Medication.DoseUnit.fromRaw(doseUnit),
            firstDoseTime = firstTime,
            intervalHours = intervalHours,
            form = Medication.Form.fromRaw(form),
            hasScheduleChange = hasScheduleChange,
            scheduleChangeAfterDays = scheduleChangeAfterDays,
            dosesPerDayAfterChange = dosesPerDayAfterChange,
            intervalHoursAfterChange = intervalHoursAfterChange,
        )
    }

    companion object {
        private val DAY_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd")

        fun from(medication: Medication, userId: String): MedicationRemoteRow {
            val start = medication.startDate.atZone(ZoneOffset.UTC).toLocalDate()
            return MedicationRemoteRow(
                id = medication.id,
                userId = userId,
                name = medication.name,
                startDate = start.format(DAY_FORMATTER),
                dosesPerDay = medication.dosesPerDay,
                doseAmount = medication.doseAmount,
                doseUnit = medication.doseUnit.rawValue,
                firstDoseTime = DateTimeFormatter.ISO_INSTANT.format(medication.firstDoseTime),
                intervalHours = medication.intervalHours,
                form = medication.form.rawValue,
                hasScheduleChange = medication.hasScheduleChange,
                scheduleChangeAfterDays = medication.scheduleChangeAfterDays,
                dosesPerDayAfterChange = medication.dosesPerDayAfterChange,
                intervalHoursAfterChange = medication.intervalHoursAfterChange,
            )
        }

        private fun parseInstant(value: String): Instant? {
            return runCatching { Instant.parse(value) }.getOrNull()
        }
    }
}
