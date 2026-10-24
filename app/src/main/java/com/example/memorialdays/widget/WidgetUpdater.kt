package com.example.memorialdays.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WidgetUpdater(
    context: Context,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext

    fun requestUpdate() {
        scope.launch {
            runCatching {
                updateWidget(
                    widget = SingleEventWidget(),
                    receiverClass = SingleEventWidgetReceiver::class.java
                )
            }
            runCatching {
                updateWidget(
                    widget = MultiEventWidget(),
                    receiverClass = MultiEventWidgetReceiver::class.java
                )
            }
        }
    }

    private suspend fun updateWidget(
        widget: GlanceAppWidget,
        receiverClass: Class<out AppWidgetProvider>
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(appContext, receiverClass)
        )
        if (appWidgetIds.isEmpty()) return

        val glanceManager = GlanceAppWidgetManager(appContext)
        appWidgetIds.forEach { appWidgetId ->
            runCatching {
                widget.update(appContext, glanceManager.getGlanceIdBy(appWidgetId))
            }
        }
    }
}
