package com.davidcarranco.oneloop.medtracker

import android.app.Application
import com.davidcarranco.oneloop.medtracker.data.crypto.MedicationNameCipher
import com.davidcarranco.oneloop.medtracker.data.local.JsonPersistence
import com.davidcarranco.oneloop.medtracker.data.local.WidgetDataStore
import com.davidcarranco.oneloop.medtracker.data.preferences.UserPreferences
import com.davidcarranco.oneloop.medtracker.data.remote.SupabaseManager
import com.davidcarranco.oneloop.medtracker.data.repository.MedicationStore
import com.davidcarranco.oneloop.medtracker.notifications.DoseNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OneLoopApplication : Application() {

    lateinit var preferences: UserPreferences
        private set
    lateinit var notificationScheduler: DoseNotificationScheduler
        private set
    lateinit var medicationStore: MedicationStore
        private set
    lateinit var supabase: SupabaseManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = UserPreferences(this)
        notificationScheduler = DoseNotificationScheduler(this, preferences).also { it.createChannel() }
        val nameCipher = MedicationNameCipher(this)
        medicationStore = MedicationStore(
            persistence = JsonPersistence(this, nameCipher),
            widgetDataStore = WidgetDataStore(this),
            notificationScheduler = notificationScheduler,
        )
        supabase = SupabaseManager(this, preferences, nameCipher)
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        medicationStore.onMedicationsChanged = {
            appScope.launch { supabase.pushMedications(medicationStore, quiet = true) }
        }
        medicationStore.onMedicationRemoved = { id ->
            appScope.launch { supabase.deleteRemoteMedication(id) }
        }
    }

    companion object {
        lateinit var instance: OneLoopApplication
            private set
    }
}
