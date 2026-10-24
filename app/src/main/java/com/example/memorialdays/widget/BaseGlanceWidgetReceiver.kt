package com.example.memorialdays.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A deliberately small AppWidgetProvider that talks to Glance's public
 * update API directly. Glance's own GlanceAppWidgetReceiver pulls in
 * WorkManager, which would merge WAKE_LOCK, FOREGROUND_SERVICE,
 * ACCESS_NETWORK_STATE and RECEIVE_BOOT_COMPLETED into the manifest.
 *
 * Updates are rendered here, inside goAsync(), and never in a Service.
 * AppWidget IDs come from the framework rather than from Glance's receiver
 * bookkeeping, so the custom receiver works on a cold install as well.
 */
abstract class BaseGlanceWidgetReceiver : AppWidgetProvider() {

    protected abstract val widget: GlanceAppWidget

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        requestUpdate(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        requestUpdate(context, intArrayOf(appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        val ownedIds = ownedWidgetIds(appContext)
        val ids = appWidgetIds.filter { it in ownedIds }
        if (ids.isEmpty()) return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                runCatching {
                    val manager = GlanceAppWidgetManager(appContext)
                    ids.forEach { appWidgetId ->
                        runCatching {
                            widget.onDelete(appContext, manager.getGlanceIdBy(appWidgetId))
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun requestUpdate(context: Context, requestedIds: IntArray) {
        val appContext = context.applicationContext
        val ownedIds = ownedWidgetIds(appContext)
        // AppWidgetProvider is exported so the system can deliver updates. Only
        // accept IDs that actually belong to this app's receiver, otherwise a
        // third-party broadcast could force unbounded database reads.
        val appWidgetIds = requestedIds.filter { it in ownedIds }
        if (appWidgetIds.isEmpty()) return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                runCatching {
                    val manager = GlanceAppWidgetManager(appContext)
                    appWidgetIds.forEach { appWidgetId ->
                        runCatching {
                            widget.update(appContext, manager.getGlanceIdBy(appWidgetId))
                        }.onFailure {
                            Log.e(TAG, "widget update failed (id=$appWidgetId)", it)
                        }
                    }
                }.onFailure {
                    Log.e(TAG, "widget update batch failed", it)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun ownedWidgetIds(context: Context): Set<Int> {
        val manager = AppWidgetManager.getInstance(context)
        return manager.getAppWidgetIds(ComponentName(context, this.javaClass)).toSet()
    }

    private companion object {
        const val TAG = "MemorialWidget"
    }
}
