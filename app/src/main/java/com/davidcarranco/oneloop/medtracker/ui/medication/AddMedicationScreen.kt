package com.davidcarranco.oneloop.medtracker.ui.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidcarranco.oneloop.medtracker.data.model.Medication
import com.davidcarranco.oneloop.medtracker.data.repository.MedicationStore
import com.davidcarranco.oneloop.medtracker.ui.components.StepperRow
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme
import com.davidcarranco.oneloop.medtracker.ui.util.formatShortDate
import com.davidcarranco.oneloop.medtracker.ui.util.formatTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    store: MedicationStore,
    onDismiss: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    val zone = ZoneId.systemDefault()
    var name by remember { mutableStateOf("") }
    var form by remember { mutableStateOf(Medication.Form.PILL) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var dosesPerDay by remember { mutableIntStateOf(1) }
    var doseAmountText by remember { mutableStateOf("") }
    var doseUnit by remember { mutableStateOf(Medication.DoseUnit.MG) }
    var firstDose by remember { mutableStateOf(LocalTime.now()) }
    var intervalHours by remember { mutableIntStateOf(24) }
    var hasScheduleChange by remember { mutableStateOf(false) }
    var scheduleChangeAfterDays by remember { mutableIntStateOf(3) }
    var dosesPerDayAfterChange by remember { mutableIntStateOf(2) }
    var intervalHoursAfterChange by remember { mutableIntStateOf(12) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    val doseAmount = doseAmountText.toDoubleOrNull() ?: 0.0
    val canSave = name.trim().isNotEmpty() && doseAmount > 0

    Scaffold(
        containerColor = colors.softBackground,
        topBar = {
            TopAppBar(
                title = { Text("Add Medication") },
                navigationIcon = { TextButton(onClick = onDismiss) { Text("Cancel") } },
                actions = {
                    TextButton(
                        enabled = canSave,
                        onClick = {
                            val firstInstant = startDate.atTime(firstDose).atZone(zone).toInstant()
                            store.add(
                                Medication.normalized(
                                    name = name.trim(),
                                    startDate = startDate.atStartOfDay(zone).toInstant(),
                                    dosesPerDay = dosesPerDay,
                                    doseAmount = doseAmount,
                                    doseUnit = doseUnit,
                                    firstDoseTime = firstInstant,
                                    intervalHours = intervalHours,
                                    form = form,
                                    hasScheduleChange = hasScheduleChange,
                                    scheduleChangeAfterDays = scheduleChangeAfterDays,
                                    dosesPerDayAfterChange = dosesPerDayAfterChange,
                                    intervalHoursAfterChange = intervalHoursAfterChange,
                                ),
                            )
                            onDismiss()
                        },
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle("Medication details")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Medication name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text("Medication type", color = colors.mutedText)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Medication.Form.entries.forEach { option ->
                    FilterChip(
                        selected = form == option,
                        onClick = { form = option },
                        label = { Text(option.rawValue) },
                    )
                }
            }
            TextButton(onClick = { showDate = true }) { Text("Start date: ${formatShortDate(startDate)}") }
            StepperRow("Doses per day", dosesPerDay, 1, 6) {
                dosesPerDay = it
                intervalHours = max(1, 24 / it)
            }

            SectionTitle("Change daily schedule")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Change schedule after starting dose", modifier = Modifier.weight(1f), color = colors.navy)
                Switch(checked = hasScheduleChange, onCheckedChange = { hasScheduleChange = it })
            }
            if (hasScheduleChange) {
                StepperRow("Starting schedule duration (days)", scheduleChangeAfterDays, 1, 90) {
                    scheduleChangeAfterDays = it
                }
                StepperRow("New schedule doses per day", dosesPerDayAfterChange, 1, 6) {
                    dosesPerDayAfterChange = it
                    intervalHoursAfterChange = max(1, 24 / it)
                }
                if (dosesPerDayAfterChange > 1) {
                    StepperRow("New schedule interval (hours)", intervalHoursAfterChange, 1, 23) {
                        intervalHoursAfterChange = it
                    }
                } else {
                    Text("New schedule: one dose each day", color = colors.mutedText)
                }
                Text(
                    "The new schedule begins at midnight after the starting period ends.",
                    fontSize = 12.sp,
                    color = colors.mutedText,
                )
            }

            SectionTitle("Dose amount")
            OutlinedTextField(
                value = doseAmountText,
                onValueChange = { doseAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Amount per dose") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Medication.DoseUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = doseUnit == unit,
                        onClick = { doseUnit = unit },
                        label = { Text(unit.rawValue) },
                    )
                }
            }

            SectionTitle("Reminder schedule")
            TextButton(onClick = { showTime = true }) {
                val preview = startDate.atTime(firstDose).atZone(zone).toInstant()
                Text("First dose reminder: ${formatTime(preview)}")
            }
            if (dosesPerDay > 1) {
                StepperRow("Starting schedule interval (hours)", intervalHours, 1, 23) { intervalHours = it }
            } else {
                Text("Starting schedule: one dose each day", color = colors.mutedText)
            }

            Text("Starting schedule reminder times", fontSize = 12.sp, color = colors.mutedText)
            repeat(dosesPerDay) { index ->
                val time = firstDose.plusHours((intervalHours * index).toLong())
                val instant = startDate.atTime(time).atZone(zone).toInstant()
                Text("Dose ${index + 1}: ${formatTime(instant)}", fontSize = 13.sp, color = colors.navy)
            }
            if (hasScheduleChange) {
                Text(
                    "After $scheduleChangeAfterDays ${if (scheduleChangeAfterDays == 1) "day" else "days"}: $dosesPerDayAfterChange ${if (dosesPerDayAfterChange == 1) "dose" else "doses"} per day",
                    color = colors.blue,
                    fontSize = 13.sp,
                )
                repeat(dosesPerDayAfterChange) { index ->
                    val time = firstDose.plusHours((intervalHoursAfterChange * index).toLong())
                    val instant = startDate.atTime(time).atZone(zone).toInstant()
                    Text("Dose ${index + 1}: ${formatTime(instant)}", fontSize = 13.sp, color = colors.navy)
                }
            }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
    if (showTime) {
        val state = rememberTimePickerState(initialHour = firstDose.hour, initialMinute = firstDose.minute)
        DatePickerDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    firstDose = LocalTime.of(state.hour, state.minute)
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
        ) {
            TimePicker(state = state, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = OneLoopTheme.colors.mutedText)
}
