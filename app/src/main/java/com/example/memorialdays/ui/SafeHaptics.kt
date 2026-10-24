package com.example.memorialdays.ui

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/**
 * Haptics with three guards: the device must report a vibrator, the view must
 * have haptic feedback enabled, and calls are throttled to one per 200ms. Only
 * View.performHapticFeedback is used, so no VIBRATE permission is required.
 */
class SafeHaptics(
    private val view: View,
    private val haptics: HapticFeedback,
    private val hasVibrator: Boolean
) {
    private var lastAt = 0L

    fun click() = perform(HapticFeedbackType.KeyboardTap, CLICK_INTERVAL_MS)
    fun longPress() = perform(HapticFeedbackType.LongPress, CLICK_INTERVAL_MS)
    fun dragTick() = perform(HapticFeedbackType.SegmentTick, DRAG_INTERVAL_MS)

    @Suppress("ObsoleteSdkInt")
    private fun perform(type: HapticFeedbackType, minIntervalMs: Long) {
        if (!hasVibrator) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!view.isHapticFeedbackEnabled) return
        val now = SystemClock.uptimeMillis()
        if (now - lastAt < minIntervalMs) return
        lastAt = now
        haptics.performHapticFeedback(type)
    }

    private companion object {
        const val CLICK_INTERVAL_MS = 200L
        const val DRAG_INTERVAL_MS = 30L
    }
}

@Composable
fun rememberSafeHaptics(): SafeHaptics {
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val vibratorAvailable = remember(context) { deviceHasVibrator(context) }
    return remember(view, haptics, vibratorAvailable) {
        SafeHaptics(view, haptics, vibratorAvailable)
    }
}

private fun deviceHasVibrator(context: Context): Boolean {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    return vibrator?.hasVibrator() == true
}
