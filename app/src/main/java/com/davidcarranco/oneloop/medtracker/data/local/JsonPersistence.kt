package com.davidcarranco.oneloop.medtracker.data.local

import android.content.Context
import com.davidcarranco.oneloop.medtracker.data.model.IsoInstantSerializer
import com.davidcarranco.oneloop.medtracker.data.model.Medication
import com.davidcarranco.oneloop.medtracker.data.model.MedicationHistoryEntry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter

class JsonPersistence(context: Context) {

    private val folder: File = File(context.filesDir, "OneLoop").apply { mkdirs() }
    private val medicationsFile = File(folder, "medications.json")
    private val historyFile = File(folder, "medicationHistory.json")
    private val resetDateFile = File(folder, "lastDoseResetDate.txt")

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
        serializersModule = SerializersModule {
            contextual(IsoInstantSerializer)
        }
    }

    fun loadMedications(): List<Medication> {
        if (!medicationsFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(
                ListSerializer(Medication.serializer()),
                medicationsFile.readText(),
            )
        }.getOrElse { emptyList() }
    }

    fun saveMedications(medications: List<Medication>) {
        val payload = json.encodeToString(
            ListSerializer(Medication.serializer()),
            medications,
        )
        writeAtomically(medicationsFile, payload)
    }

    fun loadHistory(): List<MedicationHistoryEntry> {
        if (!historyFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(
                ListSerializer(MedicationHistoryEntry.serializer()),
                historyFile.readText(),
            )
        }.getOrElse { emptyList() }
    }

    fun saveHistory(entries: List<MedicationHistoryEntry>) {
        val payload = json.encodeToString(
            ListSerializer(MedicationHistoryEntry.serializer()),
            entries,
        )
        writeAtomically(historyFile, payload)
    }

    fun loadLastResetDate(): Instant? {
        if (!resetDateFile.exists()) return null
        val text = resetDateFile.readText().trim()
        return runCatching { Instant.parse(text) }.getOrNull()
    }

    fun saveLastResetDate(date: Instant) {
        writeAtomically(resetDateFile, DateTimeFormatter.ISO_INSTANT.format(date))
    }

    private fun writeAtomically(target: File, contents: String) {
        val temp = File(target.parentFile, "${target.name}.tmp")
        temp.writeText(contents)
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }
}
