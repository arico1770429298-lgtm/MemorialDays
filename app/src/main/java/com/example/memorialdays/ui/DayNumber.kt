package com.example.memorialdays.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * The rolling numeral. Direction follows the numeric delta: a larger number
 * enters from below and pushes the old number upward; a smaller number does the
 * opposite. All motion uses the shared spring, never tween.
 */
@Composable
fun DayNumber(
    value: Long,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.BottomStart
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            val slideSpec = spring<IntOffset>(dampingRatio = 0.75f, stiffness = 400f)
            val fadeSpec = spring<Float>(dampingRatio = 0.75f, stiffness = 400f)
            (
                slideInVertically(animationSpec = slideSpec) { height -> direction * height } +
                    fadeIn(animationSpec = fadeSpec)
                ).togetherWith(
                slideOutVertically(animationSpec = slideSpec) { height -> -direction * height } +
                    fadeOut(animationSpec = fadeSpec)
            )
        },
        contentAlignment = contentAlignment,
        label = "dayNumber",
        modifier = modifier
    ) { target ->
        Text(
            text = target.toString(),
            style = TextStyle(
                fontSize = fontSize,
                lineHeight = fontSize,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
                fontFeatureSettings = "tnum",
                color = color
            ),
            maxLines = 1,
            softWrap = false
        )
    }
}
