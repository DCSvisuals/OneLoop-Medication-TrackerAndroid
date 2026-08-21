package com.davidcarranco.oneloop.medtracker.ui.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.davidcarranco.oneloop.medtracker.ui.util.formatTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicationScreen(
    medication: Medication,
    store: MedicationStore,
    onDismiss: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    val zone = ZoneId.systemDefault()
    var dosesPerDay by remember { mutableIntStateOf(medication.dosesPerDay) }
    var doseAmountText by remember { mutableStateOf(medication.doseAmount.toString()) }
    var doseUnit by remember { mutableStateOf(medication.doseUnit) }
    var firstDose by remember { mutableStateOf(medication.firstDoseTime.atZone(zone).toLocalTime()) }
    var intervalHours by remember { mutableIntStateOf(medication.intervalHours) }
    var hasScheduleChange by remember { mutableStateOf(medication.hasScheduleChange) }
    var scheduleChangeAfterDays by remember { mutableIntStateOf(medication.scheduleChangeAfterDays) }
    var dosesPerDayAfterChange by remember { mutableIntStateOf(medication.dosesPerDayAfterChange) }
    var intervalHoursAfterChange by remember { mutableIntStateOf(medication.intervalHoursAfterChange) }
    var showTime by remember { mutableStateOf(false) }

    val doseAmount = doseAmountText.toDoubleOrNull() ?: 0.0

    Scaffold(
        containerColor = colors.softBackground,
        topBar = {
            TopAppBar(
                title = { Text("Edit Doses") },
                navigationIcon = { TextButton(onClick = onDismiss) { Text("Cancel") } },
                actions = {
                    TextButton(
                        enabled = doseAmount > 0,
                        onClick = {
                            val firstInstant = LocalDate.now().atTime(firstDose).atZone(zone).toInstant()
                            store.update(
                                medication.copy(
                                    dosesPerDay = dosesPerDay,
                                    doseAmount = doseAmount,
                                    doseUnit = doseUnit,
                                    firstDoseTime = firstInstant,
                                    intervalHours = intervalHours,
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
            Text("Dose instructions", color = colors.mutedText)
            Text(medication.name, color = colors.navy)
            StepperRow("Doses per day", dosesPerDay, 1, 6) {
                dosesPerDay = it
                intervalHours = max(1, 24 / it)
            }
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

            Text("Change daily schedule", color = colors.mutedText)
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

            Text("Reminder schedule", color = colors.mutedText)
            TextButton(onClick = { showTime = true }) {
                Text("First dose reminder: ${formatTime(LocalDate.now().atTime(firstDose).atZone(zone).toInstant())}")
            }
            if (dosesPerDay > 1) {
                StepperRow("Interval between doses (hours)", intervalHours, 1, 23) { intervalHours = it }
            } else {
                Text("One reminder each day", color = colors.mutedText)
            }

            Text("Preview", color = colors.mutedText)
            repeat(dosesPerDay) { index ->
                val time = firstDose.plusHours((intervalHours * index).toLong())
                Text(
                    "Dose ${index + 1}: ${formatTime(LocalDate.now().atTime(time).atZone(zone).toInstant())}",
                    color = colors.navy,
                )
            }
        }
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
