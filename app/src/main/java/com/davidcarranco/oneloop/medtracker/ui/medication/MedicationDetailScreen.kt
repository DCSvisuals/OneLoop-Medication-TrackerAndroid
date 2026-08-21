package com.davidcarranco.oneloop.medtracker.ui.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidcarranco.oneloop.medtracker.data.AppInfo
import com.davidcarranco.oneloop.medtracker.data.model.DoseStatus
import com.davidcarranco.oneloop.medtracker.data.repository.MedicationStore
import com.davidcarranco.oneloop.medtracker.ui.components.FloatingMenuSpacer
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme
import com.davidcarranco.oneloop.medtracker.ui.theme.icon
import com.davidcarranco.oneloop.medtracker.ui.theme.tint
import com.davidcarranco.oneloop.medtracker.ui.util.formatShortDate
import com.davidcarranco.oneloop.medtracker.ui.util.formatTime
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailScreen(
    medicationId: String,
    store: MedicationStore,
    showFloatingClearance: Boolean,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    val revision by store.revision.collectAsState()
    val medication = store.medication(medicationId)
    var confirmDelete by remember { mutableStateOf(false) }

    if (medication == null) {
        Scaffold(
            containerColor = colors.softBackground,
            topBar = {
                TopAppBar(
                    title = { Text("Medication") },
                    actions = { TextButton(onClick = onBack) { Text("Done") } },
                )
            },
        ) { padding ->
            Text("This medication is no longer on your schedule.", modifier = Modifier.padding(padding).padding(20.dp))
        }
        return
    }

    val todayDoses = store.scheduledDoses().filter { it.medication.id == medication.id }
    @Suppress("UNUSED_VARIABLE")
    val observed = revision

    Scaffold(
        containerColor = colors.softBackground,
        topBar = {
            TopAppBar(
                title = { Text(medication.name) },
                actions = { TextButton(onClick = onBack) { Text("Done") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.softBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SectionLabel("Medication")
            InfoRow("Name", medication.name)
            InfoRow("Dose", medication.dosage)
            InfoRow("Daily schedule", medication.instructions)
            InfoRow("Start date", formatShortDate(medication.startDate))
            InfoRow("First reminder", formatTime(medication.firstDoseTime))
            if (medication.dosesPerDay(LocalDate.now()) > 1) {
                InfoRow("Dose interval", "Every ${medication.intervalHours(LocalDate.now())} hours")
            }

            SectionLabel("Dose reminders today")
            if (todayDoses.isEmpty()) {
                Text("No doses scheduled for today.", modifier = Modifier.padding(16.dp), color = colors.mutedText)
            } else {
                todayDoses.forEach { dose ->
                    ListItem(
                        headlineContent = { Text("Dose ${dose.doseNumber}") },
                        supportingContent = { Text(formatTime(dose.scheduledTime)) },
                        leadingContent = {
                            Icon(
                                if (dose.status == DoseStatus.TAKEN) Icons.Filled.CheckCircle else medication.form.icon(),
                                contentDescription = null,
                                tint = dose.status.tint(),
                            )
                        },
                        trailingContent = {
                            if (dose.status == DoseStatus.TAKEN) {
                                Text("Taken", color = colors.success)
                            } else {
                                Button(onClick = { store.markTaken(dose) }) { Text("Mark taken") }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }

            ListItem(
                headlineContent = { Text("Edit doses") },
                leadingContent = { Icon(Icons.Filled.Edit, null, tint = colors.blue) },
                modifier = Modifier,
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                trailingContent = {
                    TextButton(onClick = onEdit) { Text("Edit") }
                },
            )
            if (medication.doses.any { it.status == DoseStatus.TAKEN }) {
                ListItem(
                    headlineContent = { Text("Undo taken doses") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Undo, null, tint = colors.orange) },
                    trailingContent = {
                        TextButton(onClick = { store.markAllDosesAsNotTaken(medication) }) { Text("Undo") }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            Text(
                AppInfo.MEDICAL_DISCLAIMER_SHORT +
                    " Update this medication only when following instructions from your medical practitioner.",
                fontSize = 12.sp,
                color = colors.mutedText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text("Remove Medication", color = Color.Red) },
                leadingContent = { Icon(Icons.Filled.Delete, null, tint = Color.Red) },
                trailingContent = {
                    TextButton(onClick = { confirmDelete = true }) { Text("Remove", color = Color.Red) }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            FloatingMenuSpacer(showFloatingClearance)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove ${medication.name}?") },
            text = { Text("This removes the medication and its reminder schedule from OneLoop.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.remove(medication)
                        confirmDelete = false
                        onBack()
                    },
                ) { Text("Remove Medication", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = OneLoopTheme.colors.mutedText,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp),
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = OneLoopTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = colors.navy)
        Text(value, color = colors.mutedText)
    }
}
