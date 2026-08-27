package com.davidcarranco.oneloop.medtracker.ui.schedule

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private enum class CalendarMode(val label: String) {
    Day("Daily"),
    Week("Weekly"),
    Month("Monthly"),
    Year("Yearly"),
}

@Composable
fun ScheduleScreen(
    store: MedicationStore,
    showFloatingClearance: Boolean,
) {
    val colors = OneLoopTheme.colors
    var mode by remember { mutableStateOf(CalendarMode.Day) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.softBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PageHeader()
        ModePicker(mode) { mode = it }
        DateNavigation(mode, selectedDate, store) { selectedDate = it }

        when (mode) {
            CalendarMode.Day -> DailyView(store, selectedDate) { selectedDate = it }
            CalendarMode.Week -> WeeklyView(store, selectedDate) {
                selectedDate = it
                mode = CalendarMode.Day
            }
            CalendarMode.Month -> MonthlyView(store, selectedDate) {
                selectedDate = it
                mode = CalendarMode.Day
            }
            CalendarMode.Year -> YearlyView(store, selectedDate) {
                selectedDate = it
                mode = CalendarMode.Month
            }
        }
        FloatingMenuSpacer(showFloatingClearance)
    }
}

@Composable
private fun PageHeader() {
    OneLoopPageHeader(
        eyebrow = "OneLoop",
        title = "Schedule",
        subtitle = "Plan your doses with confidence.",
        applyStatusBarPadding = false,
        trailing = {
            GlassCircleButton(
                icon = Icons.Filled.CalendarMonth,
                contentDescription = "Schedule",
            )
        },
    )
}

@Composable
private fun ModePicker(selected: CalendarMode, onSelect: (CalendarMode) -> Unit) {
    val colors = OneLoopTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(colors.elevatedCard)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(17.dp))
            .padding(5.dp),
    ) {
        CalendarMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Text(
                text = mode.label,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) colors.lime else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 11.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isSelected) colors.scheduleSelectionText else colors.mutedText,
            )
        }
    }
}

@Composable
private fun DateNavigation(
    mode: CalendarMode,
    selectedDate: LocalDate,
    store: MedicationStore,
    onChange: (LocalDate) -> Unit,
) {
    val colors = OneLoopTheme.colors
    val title = when (mode) {
        CalendarMode.Day, CalendarMode.Month ->
            selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        CalendarMode.Week ->
            "Week of " + selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))
        CalendarMode.Year -> selectedDate.year.toString()
    }.uppercase(Locale.getDefault())
    val subtitle = when (mode) {
        CalendarMode.Day -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE d"))
        CalendarMode.Week -> {
            val start = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .let { if (selectedDate.dayOfWeek == DayOfWeek.SUNDAY) selectedDate else it }
            val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(firstDow()))
            val total = (0 until 7).sumOf { store.doseCount(weekStart.plusDays(it.toLong())) }
            "$total doses this week"
        }
        CalendarMode.Month -> "${store.doseCount(selectedDate)} doses on selected day"
        CalendarMode.Year -> "Medication overview"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        NavArrow { onChange(shift(selectedDate, mode, -1)) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.Bold, color = colors.navy)
            Text(subtitle, fontSize = 12.sp, color = colors.mutedText)
        }
        NavArrow(forward = true) { onChange(shift(selectedDate, mode, 1)) }
    }
}

@Composable
private fun NavArrow(forward: Boolean = false, onClick: () -> Unit) {
    val colors = OneLoopTheme.colors
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (forward) Icons.Filled.ChevronRight else Icons.Filled.ChevronLeft, null, tint = colors.navy)
    }
}

@Composable
private fun DailyView(
    store: MedicationStore,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
) {
    val colors = OneLoopTheme.colors
    val items = store.scheduledDoses(selectedDate)
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        WeekStrip(selectedDate, onSelectDate)
        Row {
            Text("DOSE TIMELINE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.mutedText, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text("${items.size} DOSES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.blue)
        }
        if (items.isEmpty()) {
            EmptyTimeline()
        } else {
            OneLoopCard(corner = 24.dp) {
                items.forEachIndexed { index, item ->
                    TimelineRow(item, isLast = index == items.lastIndex)
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(selectedDate: LocalDate, onSelectDate: (LocalDate) -> Unit) {
    val colors = OneLoopTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(19.dp))
            .background(colors.elevatedCard)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(19.dp))
            .padding(5.dp),
    ) {
        (-3..3).forEach { offset ->
            val date = selectedDate.plusDays(offset.toLong())
            val selected = date == selectedDate
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) colors.lime else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelectDate(date) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (selected) colors.scheduleSelectionText else colors.mutedText,
                )
                Text(
                    date.dayOfMonth.toString(),
                    fontWeight = FontWeight.Bold,
                    color = if (selected) colors.scheduleSelectionText else colors.mutedText,
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(item: ScheduledDose, isLast: Boolean) {
    val colors = OneLoopTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            formatTime(item.scheduledTime),
            fontWeight = FontWeight.Bold,
            color = colors.navy,
            modifier = Modifier.width(72.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(item.status.tint()),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(colors.cardBorder),
                )
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(item.medication.name, fontWeight = FontWeight.SemiBold, color = colors.navy)
            Text("Dose ${item.doseNumber} • ${item.medication.dosage}", fontSize = 12.sp, color = colors.mutedText)
            Text(item.medication.instructions, fontSize = 11.sp, color = colors.mutedText.copy(0.85f))
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(item.medication.form.tint().copy(0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.medication.form.icon(), null, tint = item.medication.form.tint())
        }
    }
}

@Composable
private fun WeeklyView(
    store: MedicationStore,
    selectedDate: LocalDate,
    onOpenDay: (LocalDate) -> Unit,
) {
    val colors = OneLoopTheme.colors
    val start = selectedDate.with(TemporalAdjusters.previousOrSame(firstDow()))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("WEEKLY OVERVIEW", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.mutedText, letterSpacing = 1.sp)
        (0 until 7).forEach { offset ->
            val date = start.plusDays(offset.toLong())
            val selected = date == selectedDate
            val count = store.doseCount(date)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.cardBackground)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                    .clickable { onOpenDay(date) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) colors.lime else colors.blue.copy(0.12f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) colors.scheduleSelectionText else colors.blue,
                    )
                    Text(
                        date.dayOfMonth.toString(),
                        fontWeight = FontWeight.Bold,
                        color = if (selected) colors.scheduleSelectionText else colors.blue,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (date == LocalDate.now()) "Today" else date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.navy,
                    )
                    Text(
                        if (count == 0) "No medication reminders" else "$count doses scheduled",
                        fontSize = 12.sp,
                        color = colors.mutedText,
                    )
                }
                Icon(Icons.Filled.ChevronRight, null, tint = colors.mutedText)
            }
        }
    }
}

@Composable
private fun MonthlyView(
    store: MedicationStore,
    selectedDate: LocalDate,
    onOpenDay: (LocalDate) -> Unit,
) {
    val colors = OneLoopTheme.colors
    val month = YearMonth.from(selectedDate)
    val first = month.atDay(1)
    val leading = ((first.dayOfWeek.value % 7) - (firstDow().value % 7) + 7) % 7
    val days = buildList<LocalDate?> {
        repeat(leading) { add(null) }
        for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
        while (size % 7 != 0) add(null)
    }
    OneLoopCard(corner = 24.dp) {
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.rotated(firstDow()).forEach { dow ->
                Text(
                    dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = colors.mutedText,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    date == selectedDate -> colors.lime
                                    date == LocalDate.now() -> colors.blue.copy(0.12f)
                                    else -> androidx.compose.ui.graphics.Color.Transparent
                                },
                            )
                            .then(if (date != null) Modifier.clickable { onOpenDay(date) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (date == selectedDate) colors.scheduleSelectionText else colors.navy,
                                )
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (store.hasScheduledDoses(date)) colors.blue
                                            else androidx.compose.ui.graphics.Color.Transparent,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(colors.lime))
            Spacer(Modifier.width(8.dp))
            Text("Selected day", fontSize = 12.sp, color = colors.mutedText)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(8.dp).clip(CircleShape).background(colors.blue))
            Spacer(Modifier.width(8.dp))
            Text("Scheduled doses", fontSize = 12.sp, color = colors.mutedText)
        }
    }
}

@Composable
private fun YearlyView(
    store: MedicationStore,
    selectedDate: LocalDate,
    onOpenMonth: (LocalDate) -> Unit,
) {
    val colors = OneLoopTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        (1..12).chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { month ->
                    val monthDate = LocalDate.of(selectedDate.year, month, 1)
                    val doseDays = (1..monthDate.lengthOfMonth()).count { day ->
                        store.hasScheduledDoses(monthDate.withDayOfMonth(day))
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.cardBackground)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                            .clickable { onOpenMonth(monthDate) }
                            .padding(15.dp),
                    ) {
                        Text(
                            monthDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                            fontWeight = FontWeight.SemiBold,
                            color = colors.navy,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) { index ->
                                Box(
                                    Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index < minOf(doseDays, 5)) colors.blue.copy(0.75f)
                                            else colors.blue.copy(0.18f),
                                        ),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (doseDays == 0) "No scheduled doses" else "$doseDays days with doses",
                            fontSize = 12.sp,
                            color = colors.mutedText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTimeline() {
    val colors = OneLoopTheme.colors
    OneLoopCard(corner = 24.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.EventAvailable, null, tint = colors.blue, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(8.dp))
            Text("No doses scheduled", fontWeight = FontWeight.SemiBold, color = colors.navy)
            Text("Add medication to build your daily timeline.", color = colors.mutedText, textAlign = TextAlign.Center)
        }
    }
}

private fun firstDow(): DayOfWeek =
    java.time.temporal.WeekFields.of(Locale.getDefault()).firstDayOfWeek

private fun shift(date: LocalDate, mode: CalendarMode, direction: Int): LocalDate = when (mode) {
    CalendarMode.Day -> date.plusDays(direction.toLong())
    CalendarMode.Week -> date.plusWeeks(direction.toLong())
    CalendarMode.Month -> date.plusMonths(direction.toLong())
    CalendarMode.Year -> date.plusYears(direction.toLong())
}

private fun List<DayOfWeek>.rotated(start: DayOfWeek): List<DayOfWeek> {
    val ordered = DayOfWeek.entries
    val index = ordered.indexOf(start)
    return ordered.drop(index) + ordered.take(index)
}
