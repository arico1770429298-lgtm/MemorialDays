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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.memorialdays.MainActivity
import com.example.memorialdays.R
import com.example.memorialdays.domain.DayStatus
import kotlinx.coroutines.withTimeoutOrNull

class SingleEventWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Never let a slow/blocked database keep the widget on its loading
        // spinner: fall back to the empty/error card after five seconds.
        val snapshot = withTimeoutOrNull(5_000L) {
            WidgetData.load(context)
        } ?: WidgetSnapshot(items = emptyList(), failed = true)
        try {
            provideContent {
                SingleEventContent(snapshot)
            }
        } catch (t: Throwable) {
            runCatching {
                provideContent { WidgetEmpty(failed = true) }
            }
        }
    }
}

@Composable
private fun SingleEventContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val item = snapshot.items.firstOrNull()
    val accent = accentProvider(context)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceProvider())
            .cornerRadius(4.dp)
            .padding(18.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.TopStart
    ) {
        if (item == null) {
            WidgetEmpty(failed = snapshot.failed)
        } else {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Text(
                    text = shortStatusText(context, item.status),
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = mutedProvider()
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.defaultWeight())
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = item.magnitude.toString(),
                        style = TextStyle(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Normal,
                            color = accent
                        ),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = context.getString(R.string.unit_day),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = mutedProvider()
                        ),
                        maxLines = 1
                    )
                }
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = item.title,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textProvider()
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun WidgetEmpty(failed: Boolean) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = context.getString(
                if (failed) R.string.widget_error_title else R.string.widget_empty_title
            ),
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textProvider()
            ),
            maxLines = 1
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = context.getString(R.string.widget_empty_action),
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = mutedProvider()
            ),
            maxLines = 1
        )
    }
}

private fun shortStatusText(context: Context, status: DayStatus): String = context.getString(
    when (status) {
        DayStatus.PAST -> R.string.widget_past
        DayStatus.TODAY -> R.string.widget_today
        DayStatus.FUTURE -> R.string.widget_remaining
    }
)
