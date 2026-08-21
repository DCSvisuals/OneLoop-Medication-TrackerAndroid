package com.davidcarranco.oneloop.medtracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.davidcarranco.oneloop.medtracker.OneLoopApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            intent.action != Intent.ACTION_TIMEZONE_CHANGED &&
            intent.action != Intent.ACTION_TIME_CHANGED
        ) {
            return
        }
        val app = context.applicationContext as? OneLoopApplication ?: return
        app.medicationStore.resetDosesIfNeeded()
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                app.notificationScheduler.rescheduleAll(app.medicationStore.medications.value)
            } finally {
                pending.finish()
            }
        }
    }
}
