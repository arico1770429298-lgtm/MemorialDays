package com.example.memorialdays.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.memorialdays.MainActivity
import com.example.memorialdays.R
import com.example.memorialdays.domain.DayStatus
import java.util.Locale
import kotlinx.coroutines.withTimeoutOrNull

class MultiEventWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withTimeoutOrNull(5_000L) {
            WidgetData.load(context)
        } ?: WidgetSnapshot(items = emptyList(), failed = true)
        try {
            provideContent {
                MultiEventContent(snapshot)
            }
        } catch (t: Throwable) {
            runCatching {
                provideContent { WidgetEmpty(failed = true) }
            }
        }
    }
}

@Composable
private fun MultiEventContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val accent = accentProvider(context)
    val rows = snapshot.items.take(3)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceProvider())
            .cornerRadius(4.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.TopStart
    ) {
        if (rows.isEmpty()) {
            WidgetEmpty(failed = snapshot.failed)
        } else {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Text(
                    text = context.getString(R.string.brand_name).uppercase(Locale.ROOT),
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = accent
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(7.dp))
                rows.forEachIndexed { index, item ->
                    WidgetRow(context = context, item = item)
                    if (index != rows.lastIndex) {
                        Spacer(GlanceModifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetRow(context: Context, item: WidgetItem) {
    val accent = accentProvider(context)
    val valueColor = when (item.status) {
        DayStatus.TODAY -> accent
        DayStatus.PAST -> mutedProvider()
        DayStatus.FUTURE -> textProvider()
    }
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (item.status == DayStatus.PAST) mutedProvider() else textProvider()
            ),
            maxLines = 1
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = rowValueText(context, item),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = valueColor
            ),
            maxLines = 1
        )
    }
}

private fun rowValueText(context: Context, item: WidgetItem): String = when (item.status) {
    DayStatus.TODAY -> context.getString(R.string.status_today)
    DayStatus.FUTURE -> context.getString(
        R.string.widget_days_format,
        context.getString(R.string.widget_remaining),
        item.magnitude
    )
    DayStatus.PAST -> context.getString(
        R.string.widget_days_format,
        context.getString(R.string.widget_past),
        item.magnitude
    )
}
