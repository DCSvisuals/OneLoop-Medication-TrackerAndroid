package com.davidcarranco.oneloop.medtracker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidcarranco.oneloop.medtracker.data.model.MedicationHistoryEntry
import com.davidcarranco.oneloop.medtracker.data.repository.MedicationStore
import com.davidcarranco.oneloop.medtracker.ui.components.FloatingMenuSpacer
import com.davidcarranco.oneloop.medtracker.ui.components.GlassCircleButton
import com.davidcarranco.oneloop.medtracker.ui.components.OneLoopCard
import com.davidcarranco.oneloop.medtracker.ui.components.OneLoopPageHeader
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme
import com.davidcarranco.oneloop.medtracker.ui.theme.icon
import com.davidcarranco.oneloop.medtracker.ui.theme.tint
import com.davidcarranco.oneloop.medtracker.ui.util.formatShortDate
import com.davidcarranco.oneloop.medtracker.ui.util.formatTime
import java.time.Instant

@Composable
fun HistoryScreen(
    store: MedicationStore,
    showFloatingClearance: Boolean,
) {
    val colors = OneLoopTheme.colors
    val records by store.historyEntries.collectAsState()
    val sorted = records.sortedWith(
        compareByDescending<MedicationHistoryEntry> { it.recordedAt }
            .thenBy { it.name.lowercase() },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.softBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        OneLoopPageHeader(
            eyebrow = "OneLoop",
            title = "History",
            subtitle = "Saved medication details — kept even after you remove them from your schedule.",
            applyStatusBarPadding = false,
            trailing = {
                GlassCircleButton(
                    icon = Icons.Filled.History,
                    contentDescription = "History",
                )
            },
        )

        if (sorted.isEmpty()) {
            OneLoopCard(corner = 24.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.History, null, tint = colors.blue, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No medication history yet", fontWeight = FontWeight.SemiBold, color = colors.navy)
                    Text(
                        "When you add a medication, its details are saved here. Removing it from your schedule keeps the History record.",
                        color = colors.mutedText,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            sorted.forEach { HistoryEntryCard(it) }
        }
        FloatingMenuSpacer(showFloatingClearance)
    }
}

@Composable
private fun HistoryEntryCard(entry: MedicationHistoryEntry) {
    val colors = OneLoopTheme.colors
    OneLoopCard(
        corner = 20.dp,
        modifier = Modifier.alpha(if (entry.wasRemovedFromSchedule) 0.85f else 1f),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            androidx.compose.foundation.layout.Box(
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
                    "${entry.dosage} • ${entry.dosesPerDay} ${if (entry.dosesPerDay == 1) "dose" else "doses"} / day",
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
        if (entry.startDate != Instant.EPOCH) {
            InfoRow("Started", formatShortDate(entry.startDate))
        }
        InfoRow("Interval", "Every ${entry.intervalHours} hours")
        if (entry.firstDoseTime != Instant.EPOCH) {
            InfoRow("First dose", formatTime(entry.firstDoseTime))
        }
        if (entry.scheduledTimes.isNotEmpty()) {
            InfoRow("Times", entry.scheduledTimes.joinToString(", ") { formatTime(it) })
        }
        Spacer(Modifier.height(6.dp))
        Text(entry.instructions, fontSize = 12.sp, color = colors.mutedText)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    val colors = OneLoopTheme.colors
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.mutedText,
            modifier = Modifier.width(72.dp),
        )
        Text(value, fontSize = 12.sp, color = colors.navy)
    }
}
