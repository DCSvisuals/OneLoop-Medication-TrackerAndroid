package com.davidcarranco.oneloop.medtracker.ui.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidcarranco.oneloop.medtracker.data.model.DoseStatus
import com.davidcarranco.oneloop.medtracker.data.model.ScheduledDose
import com.davidcarranco.oneloop.medtracker.data.repository.MedicationStore
import com.davidcarranco.oneloop.medtracker.ui.components.FloatingMenuSpacer
import com.davidcarranco.oneloop.medtracker.ui.components.GlassCircleButton
import com.davidcarranco.oneloop.medtracker.ui.components.OneLoopCard
import com.davidcarranco.oneloop.medtracker.ui.components.OneLoopPageHeader
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme
import com.davidcarranco.oneloop.medtracker.ui.theme.icon
import com.davidcarranco.oneloop.medtracker.ui.theme.tint
import com.davidcarranco.oneloop.medtracker.ui.util.formatTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    store: MedicationStore,
    showFloatingClearance: Boolean,
    onAddMedication: () -> Unit,
    onOpenMedication: (String) -> Unit,
) {
    val colors = OneLoopTheme.colors
    val scheduled = store.scheduledDoses()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.softBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        HeaderSection(onAddMedication)
        AdherenceCard(store)
        HeroStatusCard(store, onOpenMedication)
        ScheduleSection(scheduled, onOpenMedication)
        FloatingMenuSpacer(showFloatingClearance)
    }
}

@Composable
private fun HeaderSection(onAddMedication: () -> Unit) {
    val now = Instant.now().atZone(ZoneId.systemDefault())
    val greeting = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Hello"
    }
    val date = now.toLocalDate().format(
        DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault()),
    )
    OneLoopPageHeader(
        eyebrow = "Today",
        title = greeting,
        subtitle = date,
        applyStatusBarPadding = false,
        trailing = {
            GlassCircleButton(
                icon = Icons.Filled.Add,
                contentDescription = "Add medication",
                onClick = onAddMedication,
            )
        },
    )
}

@Composable
private fun AdherenceCard(store: MedicationStore) {
    val colors = OneLoopTheme.colors
    val progress = store.progress.toFloat()
    val animated by animateFloatAsState(progress, label = "adherence")
    val ringColor = when {
        store.totalCount == 0 -> colors.mutedText
        store.progress >= 1.0 -> colors.success
        store.progress >= 0.5 -> colors.blue
        else -> colors.orange
    }
    val message = when {
        store.totalCount == 0 -> "No medications scheduled today."
        store.allDosesTakenToday -> "All medications are logged for today."
        store.missedCount > 0 ->
            "${store.missedCount} missed dose${if (store.missedCount == 1) "" else "s"} · ${store.completedCount} of ${store.totalCount} taken."
        store.completedCount == 0 -> "Start today by logging your first dose."
        else -> "${store.remainingCount} dose${if (store.remainingCount == 1) "" else "s"} remaining today."
    }
    OneLoopCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .semantics {
                        contentDescription =
                            "Today's adherence: ${store.completedCount} of ${store.totalCount} doses taken"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(68.dp)) {
                    drawArc(
                        color = colors.mutedText.copy(alpha = 0.18f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.SemiBold, color = colors.navy)
            }
            Spacer(Modifier.size(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("TODAY'S ADHERENCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.mutedText)
                Text(
                    "${store.completedCount} of ${store.totalCount} doses taken",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.navy,
                )
                Text(message, fontSize = 13.sp, color = colors.mutedText)
            }
        }
    }
}

@Composable
private fun HeroStatusCard(store: MedicationStore, onOpenMedication: (String) -> Unit) {
    when {
        store.totalCount == 0 -> Unit
        store.allDosesTakenToday -> AllDoneCard()
        store.dueDose != null -> DueDoseCard(store, store.dueDose!!, onOpenMedication)
        store.missedDose != null -> MissedDoseCard(store, store.missedDose!!, onOpenMedication)
        store.nextUpcomingDose != null -> NextDoseCard(store, store.nextUpcomingDose!!, onOpenMedication)
    }
}

@Composable
private fun AllDoneCard() {
    val colors = OneLoopTheme.colors
    OneLoopCard(
        modifier = Modifier.border(1.dp, colors.success.copy(0.35f), RoundedCornerShape(22.dp)),
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = colors.success, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text("All done for today", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.navy)
        Text(
            "You have logged all scheduled medications. Your schedule will reset at midnight.",
            color = colors.mutedText,
        )
    }
}

@Composable
private fun DueDoseCard(
    store: MedicationStore,
    dose: ScheduledDose,
    onOpenMedication: (String) -> Unit,
) {
    val colors = OneLoopTheme.colors
    OneLoopCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Notifications, null, tint = colors.orange)
            Spacer(Modifier.size(8.dp))
            Text("DOSE DUE NOW", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.orange)
        }
        Spacer(Modifier.height(12.dp))
        MedicationHeader(dose)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { store.snooze(dose) }) { Text("Snooze 10 min") }
            Spacer(Modifier.weight(1f))
            Button(onClick = { store.markTaken(dose) }) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Mark taken")
            }
        }
        TextButton(onClick = { onOpenMedication(dose.medication.id) }) {
            Icon(Icons.Filled.Info, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
            Text("View medication details")
        }
    }
}

@Composable
private fun NextDoseCard(
    store: MedicationStore,
    dose: ScheduledDose,
    onOpenMedication: (String) -> Unit,
) {
    val colors = OneLoopTheme.colors
    OneLoopCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, null, tint = colors.blue)
            Spacer(Modifier.size(8.dp))
            Text("NEXT DOSE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.blue)
        }
        Spacer(Modifier.height(12.dp))
        MedicationHeader(dose)
        Text("Scheduled for ${formatTime(dose.scheduledTime)}", color = colors.mutedText)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { store.markTaken(dose) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Check, null)
            Spacer(Modifier.size(6.dp))
            Text("Mark taken early")
        }
        TextButton(onClick = { onOpenMedication(dose.medication.id) }) {
            Icon(Icons.Filled.Info, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
            Text("View medication details")
        }
    }
}

@Composable
private fun MissedDoseCard(
    store: MedicationStore,
    dose: ScheduledDose,
    onOpenMedication: (String) -> Unit,
) {
    val colors = OneLoopTheme.colors
    OneLoopCard(
        modifier = Modifier.border(1.dp, colors.warning.copy(0.45f), RoundedCornerShape(22.dp)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null, tint = colors.warning)
            Spacer(Modifier.size(8.dp))
            Text(
                if (store.missedCount == 1) "DOSE MISSED" else "${store.missedCount} DOSES MISSED",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.warning,
            )
        }
        Spacer(Modifier.height(12.dp))
        MedicationHeader(dose)
        Text(
            "Was scheduled for ${formatTime(dose.scheduledTime)}. You can still log it as taken.",
            color = colors.mutedText,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { store.markTaken(dose) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.warning),
        ) {
            Icon(Icons.Filled.Check, null)
            Spacer(Modifier.size(6.dp))
            Text("Mark taken")
        }
        TextButton(onClick = { onOpenMedication(dose.medication.id) }) {
            Icon(Icons.Filled.Info, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
            Text("View medication details")
        }
    }
}

@Composable
private fun MedicationHeader(dose: ScheduledDose) {
    val colors = OneLoopTheme.colors
    val todayCount = dose.medication.dosesPerDay(LocalDate.now())
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(dose.medication.form.tint()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(dose.medication.form.icon(), null, tint = androidx.compose.ui.graphics.Color.White)
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(dose.medication.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.navy)
            Text(dose.medication.dosage, color = colors.mutedText)
            Text("Dose ${dose.doseNumber} of $todayCount", fontSize = 13.sp, color = colors.mutedText)
        }
        Text(formatTime(dose.scheduledTime), fontWeight = FontWeight.SemiBold, color = colors.navy)
    }
}

@Composable
private fun ScheduleSection(
    doses: List<ScheduledDose>,
    onOpenMedication: (String) -> Unit,
) {
    val colors = OneLoopTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Today's schedule", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.navy)
        if (doses.isEmpty()) {
            OneLoopCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Schedule, null, tint = colors.blue, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No medications scheduled", fontWeight = FontWeight.SemiBold, color = colors.navy)
                    Text(
                        "Add a medication to create your daily schedule.",
                        color = colors.mutedText,
                    )
                }
            }
        } else {
            doses.forEach { dose ->
                ScheduleRow(dose, onClick = { onOpenMedication(dose.medication.id) })
            }
        }
    }
}

@Composable
private fun ScheduleRow(dose: ScheduledDose, onClick: () -> Unit) {
    val colors = OneLoopTheme.colors
    val statusColor = dose.status.tint()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (dose.status) {
                    DoseStatus.TAKEN -> Icons.Filled.Check
                    DoseStatus.DUE_NOW -> Icons.Filled.Notifications
                    DoseStatus.UPCOMING -> Icons.Filled.Schedule
                    DoseStatus.MISSED -> Icons.Filled.Warning
                },
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(dose.medication.name, fontWeight = FontWeight.SemiBold, color = colors.navy)
            Text(dose.medication.dosage, fontSize = 13.sp, color = colors.mutedText)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatTime(dose.scheduledTime), fontWeight = FontWeight.SemiBold, color = colors.navy)
            Text(dose.status.rawValue, fontSize = 12.sp, color = statusColor)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = colors.mutedText, modifier = Modifier.size(16.dp))
    }
}
