package com.davidcarranco.oneloop.medtracker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidcarranco.oneloop.medtracker.data.model.MedicationHistoryEntry
import com.davidcarranco.oneloop.medtracker.data.repository.MedicationStore
import com.davidcarranco.oneloop.medtracker.ui.components.FloatingMenuSpacer
import com.davidcarranco.oneloop.medtracker.ui.components.OneLoopCard
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme
import com.davidcarranco.oneloop.medtracker.ui.theme.icon
import com.davidcarranco.oneloop.medtracker.ui.theme.tint

@Composable
fun HistoryScreen(
    store: MedicationStore,
    showFloatingClearance: Boolean,
) {
    val colors = OneLoopTheme.colors
    val records by store.historyEntries.collectAsState()
    val active = records.filter { !it.wasRemovedFromSchedule }
        .sortedBy { it.name.lowercase() }
    val removed = records.filter { it.wasRemovedFromSchedule }
        .sortedBy { it.name.lowercase() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.softBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("ONELOOP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.blue, letterSpacing = 1.2.sp)
                Text("History", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = colors.navy)
                Text("Medication details stay here even after you remove them from the schedule.", color = colors.mutedText)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.cardBackground)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.History, null, tint = colors.blue)
            }
        }

        if (records.isEmpty()) {
            OneLoopCard(corner = 24.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.History, null, tint = colors.blue, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No medication history yet", fontWeight = FontWeight.SemiBold, color = colors.navy)
                    Text(
                        "When you add a medication, its name, dose, and schedule are saved here — even if you later remove it from Today.",
                        color = colors.mutedText,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            if (active.isNotEmpty()) {
                Text(
                    "ON SCHEDULE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.mutedText,
                    letterSpacing = 1.sp,
                )
                active.forEach { HistoryEntryCard(it) }
            }
            if (removed.isNotEmpty()) {
                Text(
                    "REMOVED FROM SCHEDULE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.mutedText,
                    letterSpacing = 1.sp,
                )
                removed.forEach { HistoryEntryCard(it) }
            }
        }
        FloatingMenuSpacer(showFloatingClearance)
    }
}

@Composable
private fun HistoryEntryCard(entry: MedicationHistoryEntry) {
    val colors = OneLoopTheme.colors
    OneLoopCard(corner = 20.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(entry.form.tint().copy(0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(entry.form.icon(), null, tint = entry.form.tint())
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.SemiBold, color = colors.navy)
                Text(
                    "${entry.dosage} • ${entry.form.rawValue}",
                    fontSize = 12.sp,
                    color = colors.mutedText,
                )
                if (entry.wasRemovedFromSchedule) {
                    Text(
                        "Removed from schedule",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.warning,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(entry.instructions, fontSize = 12.sp, color = colors.mutedText)
    }
}
