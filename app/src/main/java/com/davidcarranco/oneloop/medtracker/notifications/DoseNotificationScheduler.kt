package com.davidcarranco.oneloop.medtracker.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.davidcarranco.oneloop.medtracker.data.model.Medication
import com.davidcarranco.oneloop.medtracker.data.preferences.UserPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DoseNotificationScheduler(
    private val context: Context,
    private val preferences: UserPreferences,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Medication reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alerts when a scheduled dose is due."
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    suspend fun scheduleNotifications(medication: Medication) {
        if (!preferences.current().notificationsEnabled) {
            removeNotifications(medication)
            return
        }
        if (!canScheduleExactAlarms()) return

        removeNotifications(medication)

        val today = LocalDate.now(zone)
        var remainingSlots = remainingExactSlots()
        if (remainingSlots <= 0) return

        for (offset in 0 until SCHEDULE_DAYS_AHEAD) {
            if (remainingSlots <= 0) break
            val day = today.plusDays(offset.toLong())
            if (!medication.isActive(day, zone)) continue
            val dosesForDay = medication.dosesPerDay(day, zone)
            if (dosesForDay <= 0) continue
            val interval = medication.intervalHours(day, zone)
            for (doseIndex in 0 until dosesForDay) {
                if (remainingSlots <= 0) break
                val scheduledTime = medication.doseTime(doseIndex, day, interval, zone)
                if (!scheduledTime.isAfter(Instant.now())) continue
                scheduleExact(
                    requestCode = requestCode(medication.id, day, doseIndex),
                    triggerAt = scheduledTime,
                    title = "Time for ${medication.name}",
                    body = "Dose ${doseIndex + 1} of $dosesForDay: ${medication.dosage}",
                    medicationId = medication.id,
                    doseNumber = doseIndex + 1,
                    identifier = notificationIdentifier(medication.id, day, doseIndex),
                )
                remainingSlots -= 1
            }
        }
    }

    suspend fun scheduleSnoozeNotification(
        medication: Medication,
        doseNumber: Int,
        at: Instant,
    ) {
        if (!preferences.current().notificationsEnabled) return
        if (!canScheduleExactAlarms()) return
        if (!at.isAfter(Instant.now())) return
        val identifier = "${medication.id}-snooze-$doseNumber"
        cancel(requestCode(identifier), identifier)
        scheduleExact(
            requestCode = requestCode(identifier),
            triggerAt = at,
            title = "Snoozed reminder: ${medication.name}",
            body = "Dose $doseNumber: ${medication.dosage}",
            medicationId = medication.id,
            doseNumber = doseNumber,
            identifier = identifier,
        )
    }

    fun removeNotifications(medication: Medication) {
        val prefix = "${medication.id}-"
        val prefs = identifierStore()
        val doomed = prefs.all.keys.filter { it.startsWith(prefix) }
        doomed.forEach { identifier ->
            cancel(requestCode(identifier), identifier)
        }
        notificationManager.activeNotifications
            .filter { it.tag?.startsWith(prefix) == true || it.notification.extras.getString(EXTRA_MEDICATION_ID) == medication.id }
            .forEach { notificationManager.cancel(it.tag, it.id) }
    }

    suspend fun rescheduleAll(medications: List<Medication>) {
        cancelAllTracked()
        if (!preferences.current().notificationsEnabled) return
        medications.forEach { scheduleNotifications(it) }
    }

    fun removeAllMedicationNotifications() {
        cancelAllTracked()
        notificationManager.cancelAll()
    }

    fun canScheduleExactAlarms(): Boolean = alarmManager.canScheduleExactAlarms()

    fun exactAlarmSettingsIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }

    fun appNotificationSettingsIntent(): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

    private fun scheduleExact(
        requestCode: Int,
        triggerAt: Instant,
        title: String,
        body: String,
        medicationId: String,
        doseNumber: Int,
        identifier: String,
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DOSE_REMINDER
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_DOSE_NUMBER, doseNumber)
            putExtra(EXTRA_IDENTIFIER, identifier)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val showIntent = PendingIntent.getActivity(
            context,
            requestCode,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val info = AlarmManager.AlarmClockInfo(triggerAt.toEpochMilli(), showIntent)
        alarmManager.setAlarmClock(info, pending)
        identifierStore().edit().putInt(identifier, requestCode).apply()
    }

    private fun cancel(requestCode: Int, identifier: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DOSE_REMINDER
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
        identifierStore().edit().remove(identifier).apply()
    }

    private fun cancelAllTracked() {
        val prefs = identifierStore()
        prefs.all.keys.toList().forEach { identifier ->
            cancel(requestCode(identifier), identifier)
        }
        prefs.edit().clear().apply()
    }

    private fun remainingExactSlots(): Int {
        val used = identifierStore().all.size
        return maxOf(0, MAX_PENDING - used)
    }

    private fun identifierStore() =
        context.getSharedPreferences("oneloop_notification_ids", Context.MODE_PRIVATE)

    private fun requestCode(identifier: String): Int = identifier.hashCode()

    private fun requestCode(medicationId: String, day: LocalDate, doseIndex: Int): Int =
        requestCode(notificationIdentifier(medicationId, day, doseIndex))

    private fun notificationIdentifier(
        medicationId: String,
        day: LocalDate,
        doseIndex: Int,
    ): String {
        val dateKey = day.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return "$medicationId-$dateKey-dose-$doseIndex"
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val ACTION_DOSE_REMINDER =
            "com.davidcarranco.oneloop.medtracker.action.DOSE_REMINDER"
        const val ACTION_MARK_TAKEN =
            "com.davidcarranco.oneloop.medtracker.action.MARK_TAKEN"
        const val ACTION_SNOOZE =
            "com.davidcarranco.oneloop.medtracker.action.SNOOZE"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_MEDICATION_ID = "medicationID"
        const val EXTRA_DOSE_NUMBER = "doseNumber"
        const val EXTRA_IDENTIFIER = "identifier"
        private const val SCHEDULE_DAYS_AHEAD = 7
        private const val MAX_PENDING = 60
    }
}
