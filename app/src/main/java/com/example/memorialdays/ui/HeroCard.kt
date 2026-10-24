package com.example.memorialdays.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memorialdays.R

/**
 * Expanded hero card. It is always the first item of the single sortable list,
 * so dragging any compact card to index 0 expands it here automatically.
 *
 * The numeral is physically 30% of the card width regardless of the system
 * font scale; the surrounding text still honours font scale. That combination
 * keeps the focal number stable on 2K screens without breaking accessibility.
 *
 * The card is an opaque, pure-colour [Surface]. The only clip is [shape],
 * placed before the surface's internal clickable so the ripple never spills
 * outside the card and produces noise. Inner rows are separated by fixed
 * spacers, so an [animateContentSize] run only translates the content
 * vertically instead of letting it overlap.
 */
@Composable
fun HeroCard(
    item: EventUi,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val shape = RoundedCornerShape(24.dp)
    val dateInfo = item.baseDateInfoCompact()
    val unit = stringResource(R.string.unit_day)
    val subtitle = if (item.subtitleRes != 0) {
        stringResource(item.subtitleRes, *item.subtitleArgs.toTypedArray())
    } else {
        null
    }
    val surface = MaterialTheme.colorScheme.surface
    val tint = if (item.event.color != 0) {
        Color(item.event.color or 0xFF000000.toInt())
    } else {
        null
    }
    val cardBackground = remember(tint, surface) {
        if (tint != null) {
            Modifier.background(
                Brush.verticalGradient(
                    listOf(
                        lerp(surface, tint, 0.04f),
                        lerp(surface, tint, 0.07f)
                    )
                )
            )
        } else {
            Modifier
        }
    }
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.outline
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val cardWidth = maxWidth
        val numberSize = (cardWidth.value * 0.30f / fontScale)
            .coerceIn(40f, 160f)
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .clip(shape),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(cardBackground)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    DayNumber(
                        value = item.displayDays,
                        fontSize = numberSize.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.widthIn(max = cardWidth * 0.9f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = dateInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
