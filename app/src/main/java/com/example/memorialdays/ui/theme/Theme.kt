package com.example.memorialdays.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = OnAccentLight,
    background = Paper,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = MutedLight,
    outline = OutlineLight,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorLight
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = OnAccentDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = MutedDark,
    outline = OutlineDark,
    error = ErrorDark,
    onError = Color(0xFF3B0908),
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFFDAD6)
)

private val MemorialShapes = Shapes(
    extraLarge = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(24.dp),
    medium = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(12.dp)
)

/**
 * Monet is enabled only on Android 12+. The dynamic scheme contributes the
 * accent roles only; neutral surfaces stay on the restrained brand palette so
 * wallpaper colours cannot turn the minimal canvas into a themed gradient.
 * The explicit 300ms colour transition is the module-5 requirement; all
 * geometry and list motion use spring instead.
 */
@Composable
fun MemorialDaysTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val base = if (darkTheme) DarkColors else LightColors
    val target = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynamic = if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        base.copy(
            primary = dynamic.primary,
            onPrimary = dynamic.onPrimary,
            secondary = dynamic.secondary,
            tertiary = dynamic.tertiary
        )
    } else {
        base
    }
    val colorScheme = animateScheme(target)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MemorialTypography,
        shapes = MemorialShapes,
        content = content
    )
}

@Composable
private fun animateScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(durationMillis = 300)
    return target.copy(
        primary = animateColorAsState(target.primary, spec, label = "primary").value,
        onPrimary = animateColorAsState(target.onPrimary, spec, label = "onPrimary").value,
        secondary = animateColorAsState(target.secondary, spec, label = "secondary").value,
        tertiary = animateColorAsState(target.tertiary, spec, label = "tertiary").value,
        background = animateColorAsState(target.background, spec, label = "background").value,
        onBackground = animateColorAsState(target.onBackground, spec, label = "onBackground").value,
        surface = animateColorAsState(target.surface, spec, label = "surface").value,
        onSurface = animateColorAsState(target.onSurface, spec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, spec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(
            target.onSurfaceVariant,
            spec,
            label = "onSurfaceVariant"
        ).value,
        outline = animateColorAsState(target.outline, spec, label = "outline").value,
        error = animateColorAsState(target.error, spec, label = "error").value,
        onError = animateColorAsState(target.onError, spec, label = "onError").value,
        errorContainer = animateColorAsState(
            target.errorContainer,
            spec,
            label = "errorContainer"
        ).value,
        onErrorContainer = animateColorAsState(
            target.onErrorContainer,
            spec,
            label = "onErrorContainer"
        ).value
    )
}
