package com.davidcarranco.oneloop.medtracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.davidcarranco.oneloop.medtracker.OneLoopApplication
import com.davidcarranco.oneloop.medtracker.data.model.ScheduledDose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? OneLoopApplication ?: return
        val medicationId = intent.getStringExtra(DoseNotificationScheduler.EXTRA_MEDICATION_ID) ?: return
        val doseNumber = intent.getIntExtra(DoseNotificationScheduler.EXTRA_DOSE_NUMBER, -1)
        if (doseNumber < 1) return
        val identifier = intent.getStringExtra(DoseNotificationScheduler.EXTRA_IDENTIFIER)

        when (intent.action) {
            DoseNotificationScheduler.ACTION_MARK_TAKEN -> {
                app.medicationStore.markTaken(medicationId, doseNumber)
                identifier?.let { NotificationManagerCompat.from(context).cancel(it, it.hashCode()) }
            }

            DoseNotificationScheduler.ACTION_SNOOZE -> {
                val medication = app.medicationStore.medication(medicationId) ?: return
                val scheduled = app.medicationStore.scheduledDoses()
                    .firstOrNull { it.medication.id == medicationId && it.doseNumber == doseNumber }
                    ?: ScheduledDose(
                        medication = medication,
                        doseNumber = doseNumber,
                        scheduledTime = java.time.Instant.now(),
                        status = com.davidcarranco.oneloop.medtracker.data.model.DoseStatus.DUE_NOW,
                    )
                app.medicationStore.snooze(scheduled, minutes = 10)
                identifier?.let { NotificationManagerCompat.from(context).cancel(it, it.hashCode()) }
                scope.launch {
                    // Store already requested the snooze alarm.
                }
            }
        }
    }
}
