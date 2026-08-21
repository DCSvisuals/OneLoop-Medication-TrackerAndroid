package com.davidcarranco.oneloop.medtracker

import com.davidcarranco.oneloop.medtracker.data.model.DoseStatus
import com.davidcarranco.oneloop.medtracker.data.model.DoseStatusResolver
import com.davidcarranco.oneloop.medtracker.data.model.Medication
import com.davidcarranco.oneloop.medtracker.data.model.MedicationHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MedicationScheduleTest {

    private val zone: ZoneId = ZoneId.of("America/Chicago")

    @Test
    fun stagedScheduleChangesAfterConfiguredDays() {
        val start = LocalDate.of(2026, 8, 1)
        val medication = Medication.normalized(
            name = "Amoxicillin",
            startDate = start.atStartOfDay(zone).toInstant(),
            dosesPerDay = 3,
            doseAmount = 500.0,
            doseUnit = Medication.DoseUnit.MG,
            firstDoseTime = start.atTime(8, 0).atZone(zone).toInstant(),
            intervalHours = 8,
            hasScheduleChange = true,
            scheduleChangeAfterDays = 3,
            dosesPerDayAfterChange = 2,
            intervalHoursAfterChange = 12,
            zone = zone,
        )

        assertEquals(1, medication.dayNumber(start, zone))
        assertEquals(3, medication.dosesPerDay(start, zone))
        assertEquals(3, medication.dosesPerDay(start.plusDays(2), zone))
        assertEquals(2, medication.dosesPerDay(start.plusDays(3), zone))
        assertEquals(12, medication.intervalHours(start.plusDays(3), zone))
        assertFalse(medication.isActive(start.minusDays(1), zone))
        assertTrue(medication.isActive(start, zone))
    }

    @Test
    fun doseTimesUseFirstReminderClockOnSelectedDay() {
        val start = LocalDate.of(2026, 8, 1)
        val medication = Medication.normalized(
            name = "Vitamin D",
            startDate = start.atStartOfDay(zone).toInstant(),
            dosesPerDay = 2,
            doseAmount = 1000.0,
            doseUnit = Medication.DoseUnit.UNITS,
            firstDoseTime = start.atTime(7, 30).atZone(zone).toInstant(),
            intervalHours = 12,
            zone = zone,
        )
        val day = LocalDate.of(2026, 8, 10)
        val times = medication.scheduledTimes(day, zone)
        assertEquals(2, times.size)
        assertEquals(LocalTime.of(7, 30), times[0].atZone(zone).toLocalTime())
        assertEquals(LocalTime.of(19, 30), times[1].atZone(zone).toLocalTime())
    }

    @Test
    fun historyStorageKeyIsPerMedication() {
        assertEquals("abc", MedicationHistoryEntry.storageKey("abc"))
    }

    @Test
    fun resolvedStatusUsesGracePeriodAndSnooze() {
        val scheduled = Instant.parse("2026-08-16T13:00:00Z")
        assertEquals(
            DoseStatus.TAKEN,
            DoseStatusResolver.resolve(DoseStatus.TAKEN, scheduled, null, Instant.parse("2026-08-16T15:00:00Z")),
        )
        assertEquals(
            DoseStatus.UPCOMING,
            DoseStatusResolver.resolve(
                DoseStatus.DUE_NOW,
                scheduled,
                Instant.parse("2026-08-16T13:20:00Z"),
                Instant.parse("2026-08-16T13:10:00Z"),
            ),
        )
        assertEquals(
            DoseStatus.DUE_NOW,
            DoseStatusResolver.resolve(DoseStatus.UPCOMING, scheduled, null, Instant.parse("2026-08-16T13:30:00Z")),
        )
        assertEquals(
            DoseStatus.MISSED,
            DoseStatusResolver.resolve(DoseStatus.UPCOMING, scheduled, null, Instant.parse("2026-08-16T14:01:00Z")),
        )
    }

    @Test
    fun dosageFormatsWholeNumbersWithoutDecimal() {
        val medication = Medication.normalized(
            name = "Ibuprofen",
            dosesPerDay = 1,
            doseAmount = 200.0,
            doseUnit = Medication.DoseUnit.MG,
            firstDoseTime = Instant.parse("2026-08-16T12:00:00Z"),
            intervalHours = 24,
        )
        assertEquals("200 mg", medication.dosage)
    }
}
