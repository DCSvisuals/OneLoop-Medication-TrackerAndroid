package com.davidcarranco.oneloop.medtracker.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.davidcarranco.oneloop.medtracker.MainActivity
import com.davidcarranco.oneloop.medtracker.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DoseNotificationScheduler.ACTION_DOSE_REMINDER) return

        val title = intent.getStringExtra(DoseNotificationScheduler.EXTRA_TITLE)
            ?: context.getString(R.string.app_name)
        val body = intent.getStringExtra(DoseNotificationScheduler.EXTRA_BODY).orEmpty()
        val medicationId = intent.getStringExtra(DoseNotificationScheduler.EXTRA_MEDICATION_ID).orEmpty()
        val doseNumber = intent.getIntExtra(DoseNotificationScheduler.EXTRA_DOSE_NUMBER, 1)
        val identifier = intent.getStringExtra(DoseNotificationScheduler.EXTRA_IDENTIFIER)
            ?: "$medicationId-$doseNumber"

        val openApp = PendingIntent.getActivity(
            context,
            identifier.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(DoseNotificationScheduler.EXTRA_MEDICATION_ID, medicationId)
                putExtra(DoseNotificationScheduler.EXTRA_DOSE_NUMBER, doseNumber)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val markTaken = PendingIntent.getBroadcast(
            context,
            ("taken-$identifier").hashCode(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = DoseNotificationScheduler.ACTION_MARK_TAKEN
                putExtra(DoseNotificationScheduler.EXTRA_MEDICATION_ID, medicationId)
                putExtra(DoseNotificationScheduler.EXTRA_DOSE_NUMBER, doseNumber)
                putExtra(DoseNotificationScheduler.EXTRA_IDENTIFIER, identifier)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snooze = PendingIntent.getBroadcast(
            context,
            ("snooze-$identifier").hashCode(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = DoseNotificationScheduler.ACTION_SNOOZE
                putExtra(DoseNotificationScheduler.EXTRA_MEDICATION_ID, medicationId)
                putExtra(DoseNotificationScheduler.EXTRA_DOSE_NUMBER, doseNumber)
                putExtra(DoseNotificationScheduler.EXTRA_IDENTIFIER, identifier)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, DoseNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .addAction(0, "Mark taken", markTaken)
            .addAction(0, "Snooze 10 min", snooze)
            .build()

        NotificationManagerCompat.from(context)
            .notify(identifier, identifier.hashCode(), notification)
    }
}
