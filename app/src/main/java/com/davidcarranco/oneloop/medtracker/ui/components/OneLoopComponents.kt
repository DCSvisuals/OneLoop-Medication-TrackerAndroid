package com.davidcarranco.oneloop.medtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme

val CardShape = RoundedCornerShape(22.dp)
val SmallCardShape = RoundedCornerShape(18.dp)

@Composable
fun OneLoopCard(
    modifier: Modifier = Modifier,
    corner: Dp = 22.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OneLoopTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(corner))
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun PrimaryButton(
    title: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = OneLoopTheme.colors.blue,
            disabledContainerColor = OneLoopTheme.colors.blue.copy(alpha = 0.4f),
        ),
    ) {
        Text(title)
    }
}

@Composable
fun FloatingMenuSpacer(visible: Boolean) {
    if (visible) {
        Spacer(Modifier.height(160.dp))
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = OneLoopTheme.colors.mutedText,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
fun BackdropBlob(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(OneLoopTheme.colors.blue.copy(alpha = 0.06f)),
    )
}

@Composable
fun StepperRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
) {
    val colors = OneLoopTheme.colors
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text("$label: $value", modifier = Modifier.weight(1f), color = colors.navy)
        androidx.compose.material3.OutlinedButton(
            onClick = { onChange((value - 1).coerceAtLeast(min)) },
            enabled = value > min,
        ) { Text("−") }
        Spacer(Modifier.padding(4.dp))
        androidx.compose.material3.OutlinedButton(
            onClick = { onChange((value + 1).coerceAtMost(max)) },
            enabled = value < max,
        ) { Text("+") }
    }
}
