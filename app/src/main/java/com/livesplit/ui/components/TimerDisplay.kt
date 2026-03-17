package com.livesplit.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livesplit.data.model.AppSettings
import com.livesplit.data.model.TimerSize
import com.livesplit.util.TimeFormatter

@Composable
fun TimerDisplay(
    elapsedMs: Long,
    deltaMs: Long? = null,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val displayTime = TimeFormatter.format(elapsedMs, settings.showMilliseconds)
    val deltaColor = if (deltaMs != null && deltaMs != 0L) {
        if (deltaMs < 0) parseColor(settings.colorAhead)
        else parseColor(settings.colorBehind)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val timerSize = when (settings.timerSize) {
        TimerSize.SMALL -> 24.sp
        TimerSize.MEDIUM -> 36.sp
        TimerSize.LARGE -> 48.sp
        TimerSize.EXTRA_LARGE -> 64.sp
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = displayTime,
            fontFamily = FontFamily.Monospace,
            fontSize = timerSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (settings.showDelta && deltaMs != null && deltaMs != 0L) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = TimeFormatter.formatDelta(deltaMs, settings.showMilliseconds),
                    fontFamily = FontFamily.Monospace,
                    fontSize = (timerSize.value * 0.5).sp,
                    color = deltaColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TimerDisplayLarge(
    elapsedMs: Long,
    deltaMs: Long? = null,
    color: Long? = null,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val displayTime = TimeFormatter.format(elapsedMs, settings.showMilliseconds)

    val timerColor = color?.let {
        androidx.compose.ui.graphics.Color(
            android.graphics.Color.red(it.toInt()),
            android.graphics.Color.green(it.toInt()),
            android.graphics.Color.blue(it.toInt())
        )
    } ?: MaterialTheme.colorScheme.onSurface

    val timerSize = when (settings.timerSize) {
        TimerSize.SMALL -> 32.sp
        TimerSize.MEDIUM -> 48.sp
        TimerSize.LARGE -> 64.sp
        TimerSize.EXTRA_LARGE -> 80.sp
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = displayTime,
            fontFamily = FontFamily.Monospace,
            fontSize = timerSize,
            fontWeight = FontWeight.Bold,
            color = timerColor,
            textAlign = TextAlign.Center
        )

        if (settings.showDelta && deltaMs != null && deltaMs != 0L) {
            Text(
                text = TimeFormatter.formatDelta(deltaMs, settings.showMilliseconds),
                fontFamily = FontFamily.Monospace,
                fontSize = (timerSize.value * 0.4).sp,
                color = timerColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SegmentTimeDisplay(
    timeMs: Long,
    showMs: Boolean = true,
    modifier: Modifier = Modifier
) {
    Text(
        text = TimeFormatter.format(timeMs, showMs),
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

@Composable
fun DeltaDisplay(
    deltaMs: Long,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    if (deltaMs == 0L) return

    val color = if (deltaMs < 0) parseColor(settings.colorAhead)
    else parseColor(settings.colorBehind)

    Text(
        text = TimeFormatter.formatDelta(deltaMs, settings.showMilliseconds),
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = modifier
    )
}

private fun parseColor(colorStr: String): androidx.compose.ui.graphics.Color {
    return try {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(colorStr))
    } catch (e: Exception) {
        androidx.compose.ui.graphics.Color.Unspecified
    }
}
