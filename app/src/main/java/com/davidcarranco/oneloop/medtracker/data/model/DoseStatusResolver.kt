package com.davidcarranco.oneloop.medtracker.data.model

import java.time.Instant

object DoseStatusResolver {
    const val MISSED_GRACE_MINUTES = 60L

    fun resolve(
        savedStatus: DoseStatus,
        scheduledTime: Instant,
        snoozedUntil: Instant?,
        now: Instant,
        missedGraceMinutes: Long = MISSED_GRACE_MINUTES,
    ): DoseStatus {
        if (savedStatus == DoseStatus.TAKEN) return DoseStatus.TAKEN
        if (snoozedUntil != null && snoozedUntil.isAfter(now)) return DoseStatus.UPCOMING
        if (scheduledTime.isAfter(now)) return DoseStatus.UPCOMING
        val missedCutoff = scheduledTime.plusSeconds(missedGraceMinutes * 60)
        return if (now.isAfter(missedCutoff)) DoseStatus.MISSED else DoseStatus.DUE_NOW
    }
}
