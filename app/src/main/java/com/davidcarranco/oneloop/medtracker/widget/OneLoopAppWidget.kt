package com.davidcarranco.oneloop.medtracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.davidcarranco.oneloop.medtracker.MainActivity
import com.davidcarranco.oneloop.medtracker.data.local.WidgetDataStore
import com.davidcarranco.oneloop.medtracker.data.model.WidgetMedicationData
import com.davidcarranco.oneloop.medtracker.ui.util.formatTime

class OneLoopAppWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataStore(context).load()
        provideContent {
            GlanceTheme {
                WidgetContent(data)
            }
        }
    }
}

@Composable
private fun WidgetContent(data: WidgetMedicationData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = if (data.allMedicationsTaken) "Today's progress" else "Next medication",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = if (data.allMedicationsTaken) "All done" else data.medicationName,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 2,
        )
        Text(
            text = data.dosage,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            maxLines = 2,
        )
        Spacer(GlanceModifier.height(8.dp))
        if (!data.allMedicationsTaken) {
            Text(
                text = formatTime(data.reminderTime),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 13.sp),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = data.progressText,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
    }
}

class OneLoopAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneLoopAppWidget()
}
