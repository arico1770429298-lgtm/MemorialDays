package com.example.memorialdays.widget

import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.glance.unit.ColorProvider
import com.example.memorialdays.R

internal object WidgetPalette {
    val lightSurface = Color(0xFFFFFFFF)
    val darkSurface = Color(0xFF171816)
    val lightText = Color(0xFF1B1C1A)
    val darkText = Color(0xFFF1F0EA)
    val lightMuted = Color(0xFF6E6F69)
    val darkMuted = Color(0xFF9A9B94)
}

internal fun surfaceProvider(): ColorProvider = androidx.glance.color.ColorProvider(
    day = WidgetPalette.lightSurface,
    night = WidgetPalette.darkSurface
)

internal fun textProvider(): ColorProvider = androidx.glance.color.ColorProvider(
    day = WidgetPalette.lightText,
    night = WidgetPalette.darkText
)

internal fun mutedProvider(): ColorProvider = androidx.glance.color.ColorProvider(
    day = WidgetPalette.lightMuted,
    night = WidgetPalette.darkMuted
)

/**
 * Material You accent on Android 12+, with a fixed brand fallback on older
 * releases. Only a small area of each widget is tinted, so even a highly
 * saturated wallpaper colour stays calm.
 */
internal fun accentProvider(context: Context): ColorProvider {
    val day = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.getColor(context, android.R.color.system_accent1_700)
    } else {
        ContextCompat.getColor(context, R.color.widget_accent_light)
    }
    val night = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.getColor(context, android.R.color.system_accent1_200)
    } else {
        ContextCompat.getColor(context, R.color.widget_accent_dark)
    }
    return androidx.glance.color.ColorProvider(day = Color(day), night = Color(night))
}
