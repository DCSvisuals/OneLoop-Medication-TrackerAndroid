package com.davidcarranco.oneloop.medtracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidcarranco.oneloop.medtracker.ui.navigation.AppTab
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme

@Composable
fun FloatingCapsuleNav(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OneLoopTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(40.dp), ambientColor = Color.Black.copy(0.18f))
                .clip(RoundedCornerShape(40.dp))
                .background(colors.cardBackground.copy(alpha = 0.96f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(40.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CapsuleTab(AppTab.Today, selected, onSelect, Modifier.weight(1f))
            CapsuleTab(AppTab.Schedule, selected, onSelect, Modifier.weight(1f))
            Spacer(Modifier.width(64.dp))
            CapsuleTab(AppTab.History, selected, onSelect, Modifier.weight(1f))
            CapsuleTab(AppTab.Settings, selected, onSelect, Modifier.weight(1f))
        }

        Box(
            modifier = Modifier
                .offset(y = (-26).dp)
                .size(60.dp)
                .shadow(14.dp, CircleShape, ambientColor = colors.blue.copy(0.35f))
                .clip(CircleShape)
                .background(colors.blue)
                .border(1.dp, Color.White.copy(alpha = 0.34f), CircleShape)
                .clickable(onClick = onAdd)
                .semantics { contentDescription = "Add medication" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = colors.actionText,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun CapsuleTab(
    tab: AppTab,
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OneLoopTheme.colors
    val isSelected = tab == selected
    val tint by animateColorAsState(
        if (isSelected) colors.blue else colors.mutedText,
        label = "tabTint",
    )
    val background by animateColorAsState(
        if (isSelected) colors.blue.copy(alpha = 0.14f) else Color.Transparent,
        label = "tabBg",
    )
    Column(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(background)
            .clickable { onSelect(tab) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(tab.capsuleIcon, contentDescription = tab.title, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            text = tab.title,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
