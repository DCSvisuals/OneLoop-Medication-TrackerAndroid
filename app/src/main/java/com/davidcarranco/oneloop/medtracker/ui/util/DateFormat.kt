package com.davidcarranco.oneloop.medtracker.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

private val shortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

fun formatTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    instant.atZone(zone).toLocalTime().format(timeFormatter)

fun formatShortDate(date: LocalDate): String = date.format(shortDateFormatter)

fun formatShortDate(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    instant.atZone(zone).toLocalDate().format(shortDateFormatter)
