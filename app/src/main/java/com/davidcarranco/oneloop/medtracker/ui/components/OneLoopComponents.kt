package com.davidcarranco.oneloop.medtracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .shadow(16.dp, RoundedCornerShape(corner), ambientColor = Color.Black.copy(alpha = 0.04f))
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
    val colors = OneLoopTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.blue,
            contentColor = colors.actionText,
            disabledContainerColor = colors.blue.copy(alpha = 0.45f),
            disabledContentColor = colors.actionText.copy(alpha = 0.7f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SecondaryButton(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, color = colors.navy)
    }
}

@Composable
fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = OneLoopTheme.colors
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.cardBackground.copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.navy, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun PageDots(
    count: Int,
    current: Int,
    onDark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = OneLoopTheme.colors
    val active = if (onDark) colors.splashWordmark else colors.blue
    val inactive = if (onDark) Color.White.copy(0.32f) else colors.mutedText.copy(0.22f)
    Row(
        modifier = modifier.semantics {
            contentDescription = "Page ${current + 1} of $count"
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 22.dp else 8.dp, label = "dotWidth")
            val color by animateColorAsState(if (selected) active else inactive, label = "dotColor")
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
fun OneLoopPageHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    applyStatusBarPadding: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = OneLoopTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                eyebrow.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.teal,
                letterSpacing = 1.6.sp,
            )
            Text(
                title,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = colors.navy,
            )
            if (subtitle != null) {
                Text(subtitle, fontSize = 14.sp, color = colors.mutedText)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
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
