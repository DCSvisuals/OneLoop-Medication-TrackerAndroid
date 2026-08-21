package com.davidcarranco.oneloop.medtracker.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.davidcarranco.oneloop.medtracker.data.model.DoseStatus
import com.davidcarranco.oneloop.medtracker.data.model.Medication

@Composable
fun Medication.Form.tint(): Color = when (this) {
    Medication.Form.PILL -> OneLoopTheme.colors.blue
    Medication.Form.INJECTION -> OneLoopTheme.colors.warning
    Medication.Form.CREAM -> Color(0xFF9C27B0)
}

fun Medication.Form.icon(): ImageVector = when (this) {
    Medication.Form.PILL -> Icons.Outlined.Medication
    Medication.Form.INJECTION -> Icons.Outlined.Vaccines
    Medication.Form.CREAM -> Icons.Outlined.Science
}

@Composable
fun DoseStatus.tint(): Color = when (this) {
    DoseStatus.TAKEN -> OneLoopTheme.colors.success
    DoseStatus.DUE_NOW -> OneLoopTheme.colors.orange
    DoseStatus.UPCOMING -> OneLoopTheme.colors.blue
    DoseStatus.MISSED -> Color(0xFFE53935)
}
