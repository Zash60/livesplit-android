package com.livesplit.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.livesplit.data.model.AppSettings
import com.livesplit.data.model.Segment
import com.livesplit.util.TimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SegmentRow(
    segment: Segment,
    index: Int,
    settings: AppSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    deltaMs: Long? = null,
    isCurrentSplit: Boolean = false,
    isCompleted: Boolean = false
) {
    val backgroundColor = when {
        isCurrentSplit -> MaterialTheme.colorScheme.primaryContainer
        isCompleted && deltaMs != null && deltaMs < 0 -> MaterialTheme.colorScheme.tertiaryContainer
        isCompleted && deltaMs != null && deltaMs > 0 -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )

            // Name
            Text(
                text = segment.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // PB Time
            Text(
                text = TimeFormatter.format(segment.pbTimeMs, settings.showMilliseconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.End
            )

            // Best segment
            Text(
                text = TimeFormatter.format(segment.bestTimeMs, settings.showMilliseconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.End
            )

            // Delta (if shown)
            if (settings.showDelta && deltaMs != null) {
                val deltaColor = when {
                    deltaMs < 0 -> parseColor(settings.colorAhead)
                    deltaMs > 0 -> parseColor(settings.colorBehind)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(
                    text = TimeFormatter.formatDelta(deltaMs, settings.showMilliseconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = deltaColor,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SegmentRowEditable(
    segment: Segment,
    index: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )

            // Name
            Text(
                text = segment.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // PB Time
            Text(
                text = TimeFormatter.format(segment.pbTimeMs, showMs = true),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Drag handle
            if (showDragHandle) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun parseColor(colorStr: String): androidx.compose.ui.graphics.Color {
    return try {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(colorStr))
    } catch (e: Exception) {
        androidx.compose.ui.graphics.Color.Unspecified
    }
}
