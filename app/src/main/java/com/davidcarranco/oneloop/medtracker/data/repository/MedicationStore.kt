package com.davidcarranco.oneloop.medtracker.data.repository

import com.davidcarranco.oneloop.medtracker.data.local.JsonPersistence
import com.davidcarranco.oneloop.medtracker.data.local.WidgetDataStore
import com.davidcarranco.oneloop.medtracker.data.model.DoseStatus
import com.davidcarranco.oneloop.medtracker.data.model.DoseStatusResolver
import com.davidcarranco.oneloop.medtracker.data.model.Medication
import com.davidcarranco.oneloop.medtracker.data.model.MedicationHistoryEntry
import com.davidcarranco.oneloop.medtracker.data.model.ScheduledDose
import com.davidcarranco.oneloop.medtracker.data.model.WidgetMedicationData
import com.davidcarranco.oneloop.medtracker.notifications.DoseNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

class MedicationStore(
    private val persistence: JsonPersistence,
    private val widgetDataStore: WidgetDataStore,
    private val notificationScheduler: DoseNotificationScheduler,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val clock: () -> Instant = { Instant.now() },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    private val _historyEntries = MutableStateFlow<List<MedicationHistoryEntry>>(emptyList())
    val historyEntries: StateFlow<List<MedicationHistoryEntry>> = _historyEntries.asStateFlow()

    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision.asStateFlow()

    private val missedGraceMinutes = 60L

    var onMedicationsChanged: (() -> Unit)? = null
    var onMedicationRemoved: ((String) -> Unit)? = null

    init {
        _medications.value = persistence.loadMedications().sortedBy { it.firstDoseTime }
        _historyEntries.value = collapseLegacyHistory(persistence.loadHistory())
        persistHistory()
        resetDosesIfNeeded()
        updateWidgetData()
    }

    /** One record per medication — details only, not daily taken/missed tracking. */
    val historyRecords: List<MedicationHistoryEntry>
        get() = _historyEntries.value.sortedWith(
            compareBy<MedicationHistoryEntry> { it.name.lowercase() }
                .thenBy { it.medicationID },
        )

    fun scheduledDoses(on: LocalDate = today(), now: Instant = clock()): List<ScheduledDose> {
        val isToday = on == now.atZone(zone).toLocalDate()
        return _medications.value.flatMap { medication ->
            if (!medication.isActive(on, zone)) return@flatMap emptyList()
            val activeDoseCount = medication.dosesPerDay(on, zone)
            if (activeDoseCount <= 0) return@flatMap emptyList()
            val interval = medication.intervalHours(on, zone)
            (0 until activeDoseCount).map { index ->
                val doseNumber = index + 1
                val scheduledTime = medication.doseTime(index, on, interval, zone)
                val savedDose = medication.doses.firstOrNull { it.number == doseNumber }
                val status = if (isToday) {
                    resolvedStatus(
                        savedStatus = savedDose?.status ?: DoseStatus.UPCOMING,
                        scheduledTime = scheduledTime,
                        snoozedUntil = savedDose?.snoozedUntil,
                        now = now,
                    )
                } else {
                    DoseStatus.UPCOMING
                }
                ScheduledDose(medication, doseNumber, scheduledTime, status)
            }
        }.sortedBy { it.scheduledTime }
    }

    fun doseCount(on: LocalDate): Int = scheduledDoses(on).size

    fun hasScheduledDoses(on: LocalDate): Boolean = doseCount(on) > 0

    val dueDose: ScheduledDose?
        get() = scheduledDoses().firstOrNull { it.status == DoseStatus.DUE_NOW }

    val missedDose: ScheduledDose?
        get() = scheduledDoses().firstOrNull { it.status == DoseStatus.MISSED }

    val nextUpcomingDose: ScheduledDose?
        get() = scheduledDoses().firstOrNull { it.status == DoseStatus.UPCOMING }

    val nextIncompleteDose: ScheduledDose?
        get() = dueDose ?: missedDose ?: nextUpcomingDose

    val completedCount: Int
        get() = scheduledDoses().count { it.status == DoseStatus.TAKEN }

    val totalCount: Int
        get() = scheduledDoses().size

    val remainingCount: Int
        get() = max(0, totalCount - completedCount)

    val missedCount: Int
        get() = scheduledDoses().count { it.status == DoseStatus.MISSED }

    val progress: Double
        get() = if (totalCount == 0) 0.0 else completedCount.toDouble() / totalCount

    val allDosesTakenToday: Boolean
        get() = totalCount > 0 && completedCount == totalCount

    fun replaceAllMedications(remote: List<Medication>) {
        val today = today()
        _medications.value = remote
            .map { prepareDosesForDay(it.copy(startDate = startOfDay(it.startDate)), today) }
            .sortedBy { it.firstDoseTime }
        persistMedications()
        persistence.saveLastResetDate(startOfDay(clock()))
        _medications.value.forEach { recordMedicationInfo(it) }
        persistHistory()
        bump()
        scope.launch {
            _medications.value.forEach { notificationScheduler.scheduleNotifications(it) }
        }
    }

    fun mergeIncomingMedications(remote: List<Medication>) {
        val localIds = _medications.value.map { it.id }.toSet()
        val newcomers = remote.filter { it.id !in localIds }
        if (newcomers.isEmpty()) return
        val today = today()
        val prepared = newcomers.map { prepareDosesForDay(it.copy(startDate = startOfDay(it.startDate)), today) }
        _medications.update { (it + prepared).sortedBy { med -> med.firstDoseTime } }
        persistMedications()
        prepared.forEach { recordMedicationInfo(it) }
        persistHistory()
        bump()
        scope.launch {
            prepared.forEach { notificationScheduler.scheduleNotifications(it) }
        }
    }

    fun add(medication: Medication) {
        val today = today()
        val prepared = prepareDosesForDay(
            medication.copy(startDate = startOfDay(medication.startDate)),
            today,
        )
        _medications.update { (it + prepared).sortedBy { med -> med.firstDoseTime } }
        persistMedications()
        recordMedicationInfo(prepared)
        persistHistory()
        bump()
        scope.launch { notificationScheduler.scheduleNotifications(prepared) }
        onMedicationsChanged?.invoke()
    }

    fun update(medication: Medication) {
        val existing = _medications.value.firstOrNull { it.id == medication.id } ?: return
        val today = today()
        val prepared = preserveTakenDoses(
            existing,
            medication.copy(startDate = startOfDay(medication.startDate)),
            today,
        )
        _medications.update { list ->
            list.map { if (it.id == prepared.id) prepared else it }
                .sortedBy { it.firstDoseTime }
        }
        persistMedications()
        recordMedicationInfo(prepared)
        persistHistory()
        bump()
        scope.launch {
            notificationScheduler.removeNotifications(existing)
            notificationScheduler.scheduleNotifications(prepared)
        }
        onMedicationsChanged?.invoke()
    }

    fun remove(medication: Medication) {
        // History is independent of the live schedule. Snapshot first, then
        // drop the medication from Today/Schedule only.
        recordMedicationInfo(medication, markRemoved = true)
        persistHistory()
        _medications.update { list -> list.filterNot { it.id == medication.id } }
        persistMedications()
        bump()
        scope.launch { notificationScheduler.removeNotifications(medication) }
        onMedicationRemoved?.invoke(medication.id)
    }

    fun markTaken(medicationId: String, doseNumber: Int) {
        resetDosesIfNeeded()
        val index = _medications.value.indexOfFirst { it.id == medicationId }
        if (index < 0) return
        val updated = _medications.value[index].markDoseTaken(doseNumber)
        replaceMedicationAt(index, updated)
        bump()
    }

    fun markTaken(dose: ScheduledDose) {
        markTaken(dose.medication.id, dose.doseNumber)
    }

    fun markTaken(medication: Medication) {
        val dose = scheduledDoses().firstOrNull {
            it.medication.id == medication.id &&
                it.status in setOf(DoseStatus.DUE_NOW, DoseStatus.UPCOMING, DoseStatus.MISSED)
        } ?: return
        markTaken(dose)
    }

    fun markAllDosesAsNotTaken(medication: Medication) {
        resetDosesIfNeeded()
        val index = _medications.value.indexOfFirst { it.id == medication.id }
        if (index < 0) return
        val current = _medications.value[index]
        val updated = current.copy(
            status = DoseStatus.UPCOMING,
            doses = current.doses.map { it.copy(status = DoseStatus.UPCOMING, snoozedUntil = null) },
        )
        replaceMedicationAt(index, updated)
        bump()
    }

    fun updateDoseStatus(medicationId: String, doseNumber: Int, status: DoseStatus) {
        resetDosesIfNeeded()
        val index = _medications.value.indexOfFirst { it.id == medicationId }
        if (index < 0) return
        val updated = _medications.value[index].updateDoseStatus(doseNumber, status)
        replaceMedicationAt(index, updated)
        bump()
    }

    fun snooze(dose: ScheduledDose, minutes: Int = 10) {
        resetDosesIfNeeded()
        val index = _medications.value.indexOfFirst { it.id == dose.medication.id }
        if (index < 0) return
        val until = clock().plusSeconds(minutes * 60L)
        val updated = _medications.value[index].snoozeDose(dose.doseNumber, until)
        replaceMedicationAt(index, updated)
        bump()
        scope.launch {
            notificationScheduler.scheduleSnoozeNotification(
                medication = updated,
                doseNumber = dose.doseNumber,
                at = until,
            )
        }
    }

    fun snooze(medication: Medication, minutes: Int = 10) {
        val dose = scheduledDoses().firstOrNull {
            it.medication.id == medication.id &&
                it.status in setOf(DoseStatus.DUE_NOW, DoseStatus.MISSED)
        } ?: return
        snooze(dose, minutes)
    }

    fun resetDosesIfNeeded(now: Instant = clock()) {
        val today = now.atZone(zone).toLocalDate()
        val lastReset = persistence.loadLastResetDate()?.atZone(zone)?.toLocalDate()
        if (lastReset == null) {
            rebuildForNewDay(today)
            return
        }
        if (lastReset == today) {
            return
        }
        rebuildForNewDay(today)
    }

    fun medication(id: String): Medication? = _medications.value.firstOrNull { it.id == id }

    fun resolvedStatus(
        savedStatus: DoseStatus,
        scheduledTime: Instant,
        snoozedUntil: Instant?,
        now: Instant = clock(),
    ): DoseStatus = DoseStatusResolver.resolve(
        savedStatus = savedStatus,
        scheduledTime = scheduledTime,
        snoozedUntil = snoozedUntil,
        now = now,
        missedGraceMinutes = missedGraceMinutes,
    )

    private fun rebuildForNewDay(day: LocalDate) {
        _medications.update { list -> list.map { prepareDosesForDay(it, day) } }
        persistence.saveLastResetDate(day.atStartOfDay(zone).toInstant())
        persistMedications()
        bump()
        scope.launch {
            _medications.value.forEach { notificationScheduler.scheduleNotifications(it) }
        }
    }

    private fun recordMedicationInfo(
        medication: Medication,
        markRemoved: Boolean = false,
    ) {
        val existing = _historyEntries.value.firstOrNull { it.medicationID == medication.id }
        val now = clock()
        val today = now.atZone(zone).toLocalDate()
        val doseCount = max(1, medication.dosesPerDay(today, zone))
        val interval = medication.intervalHours(today, zone)
        val times = (0 until doseCount).map { index ->
            medication.doseTime(index, today, interval, zone)
        }
        upsertHistoryEntry(
            MedicationHistoryEntry(
                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                medicationID = medication.id,
                name = medication.name,
                dosage = medication.dosage,
                form = medication.form,
                instructions = medication.instructions,
                startDate = medication.startDate,
                dosesPerDay = doseCount,
                intervalHours = interval,
                firstDoseTime = medication.firstDoseTime,
                scheduledTimes = times,
                wasRemovedFromSchedule = markRemoved,
                recordedAt = now,
                day = existing?.day ?: now,
                doses = emptyList(),
            ),
        )
    }

    private fun collapseLegacyHistory(
        entries: List<MedicationHistoryEntry>,
    ): List<MedicationHistoryEntry> {
        return entries
            .groupBy { it.medicationID }
            .map { (_, group) ->
                val newest = group.maxBy { it.recordedAt.takeUnless { instant -> instant == Instant.EPOCH } ?: it.day }
                newest.copy(
                    doses = emptyList(),
                    wasRemovedFromSchedule = group.any { it.wasRemovedFromSchedule },
                    startDate = newest.startDate.takeUnless { it == Instant.EPOCH }
                        ?: group.minBy { it.day }.day,
                    recordedAt = newest.recordedAt.takeUnless { it == Instant.EPOCH } ?: newest.day,
                    day = group.minBy { it.day }.day,
                )
            }
            .sortedWith(
                compareByDescending<MedicationHistoryEntry> { it.recordedAt }
                    .thenBy { it.name.lowercase() },
            )
    }

    private fun upsertHistoryEntry(entry: MedicationHistoryEntry) {
        val current = _historyEntries.value
        val existingIndex = current.indexOfFirst { it.medicationID == entry.medicationID }
        val merged = if (existingIndex >= 0) {
            val previous = current[existingIndex]
            val live = _medications.value.any { it.id == entry.medicationID }
            val removedFlag = if (entry.wasRemovedFromSchedule) {
                true
            } else {
                previous.wasRemovedFromSchedule && !live
            }
            current.toMutableList().also {
                it[existingIndex] = entry.copy(
                    id = previous.id,
                    wasRemovedFromSchedule = removedFlag,
                )
            }
        } else {
            current + entry
        }
        val next = merged.sortedWith(
            compareByDescending<MedicationHistoryEntry> { it.recordedAt }
                .thenBy { it.name.lowercase() },
        )
        _historyEntries.value = next
    }

    private fun prepareDosesForDay(medication: Medication, day: LocalDate): Medication {
        val expected = medication.dosesPerDay(day, zone)
        return medication.copy(
            status = DoseStatus.UPCOMING,
            doses = (0 until expected).map { index ->
                Medication.Dose(number = index + 1, status = DoseStatus.UPCOMING)
            },
        )
    }

    private fun preserveTakenDoses(
        oldMedication: Medication,
        medication: Medication,
        today: LocalDate,
    ): Medication {
        val expected = medication.dosesPerDay(today, zone)
        val doses = (0 until expected).map { index ->
            val doseNumber = index + 1
            val oldDose = oldMedication.doses.firstOrNull { it.number == doseNumber }
            Medication.Dose(
                number = doseNumber,
                status = oldDose?.status ?: DoseStatus.UPCOMING,
                snoozedUntil = oldDose?.snoozedUntil,
            )
        }
        val status = if (doses.isNotEmpty() && doses.all { it.status == DoseStatus.TAKEN }) {
            DoseStatus.TAKEN
        } else {
            DoseStatus.UPCOMING
        }
        return medication.copy(doses = doses, status = status)
    }

    private fun replaceMedicationAt(index: Int, updated: Medication) {
        _medications.update { list ->
            list.toMutableList().also { it[index] = updated }
        }
        persistMedications()
    }

    private fun persistMedications() {
        persistence.saveMedications(_medications.value)
        updateWidgetData()
    }

    private fun persistHistory() {
        persistence.saveHistory(_historyEntries.value)
    }

    private fun updateWidgetData() {
        val next = nextIncompleteDose
        val data = if (next != null) {
            WidgetMedicationData(
                medicationName = next.medication.name,
                dosage = next.medication.dosage,
                reminderTime = next.scheduledTime,
                completedCount = completedCount,
                totalCount = totalCount,
                allMedicationsTaken = false,
            )
        } else {
            WidgetMedicationData(
                medicationName = if (_medications.value.isEmpty()) "No medications" else "All medications taken",
                dosage = if (_medications.value.isEmpty()) {
                    "Add a medication in OneLoop UIv2"
                } else {
                    "Great job for today"
                },
                reminderTime = clock(),
                completedCount = completedCount,
                totalCount = totalCount,
                allMedicationsTaken = _medications.value.isNotEmpty(),
            )
        }
        widgetDataStore.save(data)
        scope.launch { widgetDataStore.saveAndRefresh(data) }
    }

    private fun bump() {
        _revision.update { it + 1 }
    }

    private fun today(now: Instant = clock()): LocalDate = now.atZone(zone).toLocalDate()

    private fun startOfDay(instant: Instant): Instant =
        instant.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
}
