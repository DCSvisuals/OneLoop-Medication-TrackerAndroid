package com.davidcarranco.oneloop.medtracker.data.local

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.davidcarranco.oneloop.medtracker.data.model.WidgetMedicationData
import com.davidcarranco.oneloop.medtracker.widget.OneLoopAppWidget
import kotlinx.serialization.json.Json

class WidgetDataStore(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun save(data: WidgetMedicationData) {
        prefs.edit()
            .putString(KEY, json.encodeToString(WidgetMedicationData.serializer(), data))
            .apply()
    }

    fun load(): WidgetMedicationData {
        val raw = prefs.getString(KEY, null) ?: return WidgetMedicationData.empty
        return runCatching {
            json.decodeFromString(WidgetMedicationData.serializer(), raw)
        }.getOrDefault(WidgetMedicationData.empty)
    }

    suspend fun saveAndRefresh(data: WidgetMedicationData) {
        save(data)
        OneLoopAppWidget().updateAll(context)
    }

    companion object {
        const val PREFS_NAME = "oneloop_widget"
        const val KEY = "widgetMedicationData"
    }
}
