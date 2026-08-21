package com.davidcarranco.oneloop.medtracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    val title: String,
    val capsuleIcon: ImageVector,
    val barIcon: ImageVector,
) {
    Today("Today", Icons.Filled.Home, Icons.Filled.Home),
    Schedule("Schedule", Icons.Filled.CalendarMonth, Icons.Filled.CalendarMonth),
    History("History", Icons.Filled.BarChart, Icons.Filled.History),
    Settings("Settings", Icons.Filled.Settings, Icons.Filled.Settings),
}
