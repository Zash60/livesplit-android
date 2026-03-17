package com.livesplit.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livesplit.data.model.Segment
import com.livesplit.data.model.SegmentResult
import com.livesplit.service.formatTime

@Composable
fun TimerOverlayContent(
    elapsedMs: Long,
    isRunning: Boolean,
    splitIndex: Int,
    segments: List<Segment>,
    results: List<SegmentResult>,
    showPbDialog: Boolean,
    newPbTime: Long,
    categoryName: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    var showPbDialogInternal by remember { mutableStateOf(false) }

    LaunchedEffect(showPbDialog) {
        showPbDialogInternal = showPbDialog
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp)),
        color = Color(0xDD1C1B1F),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Category name
            if (categoryName.isNotEmpty()) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Current split name
            if (splitIndex in segments.indices) {
                Text(
                    text = segments[splitIndex].name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Main timer - clickable area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Timer display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatTime(elapsedMs),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = getTimerColor(results),
                        textAlign = TextAlign.Center
                    )

                    // Delta from last split
                    if (results.isNotEmpty()) {
                        val lastResult = results.last()
                        val deltaColor = if (lastResult.deltaMs < 0) Color(0xFF4CAF50) // Green - ahead
                                         else if (lastResult.deltaMs > 0) Color(0xFFF44336) // Red - behind
                                         else Color.White

                        Text(
                            text = formatDelta(lastResult.deltaMs),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = deltaColor,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Status indicator
                    if (!isRunning && splitIndex < 0) {
                        Text(
                            text = "Tap to start",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    } else if (isRunning) {
                        Text(
                            text = "Tap to split",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50).copy(alpha = 0.7f)
                        )
                    } else if (splitIndex >= segments.size - 1 && elapsedMs > 0) {
                        Text(
                            text = "Tap to restart • Long press to reset",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Split summary
            if (results.isNotEmpty() && segments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Split ${results.size}/${segments.size}",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatTime(elapsedMs),
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // PB Dialog
    if (showPbDialogInternal) {
        AlertDialog(
            onDismissRequest = { /* Don't dismiss without action */ },
            title = {
                Text(
                    "New Personal Best!",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text("Congratulations! Your new time: ${formatTime(newPbTime)}")
            },
            confirmButton = {
                TextButton(onClick = { showPbDialogInternal = false }) {
                    Text("Saved!")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

private fun getTimerColor(results: List<SegmentResult>): Color {
    if (results.isEmpty()) return Color.White
    val lastResult = results.last()
    return when {
        lastResult.deltaMs < 0 -> Color(0xFF4CAF50) // Green - ahead of PB
        lastResult.deltaMs > 0 -> Color(0xFFF44336) // Red - behind PB
        else -> Color.White
    }
}

private fun formatDelta(ms: Long): String {
    val sign = if (ms < 0) "-" else "+"
    val absMs = Math.abs(ms)
    val totalSeconds = absMs / 1000
    val seconds = totalSeconds % 60
    val millis = absMs % 1000
    return "%s%d.%03d".format(sign, seconds, millis)
}
