package com.livesplit.ui.splits

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livesplit.data.model.Segment

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SplitsScreen(
    categoryId: Long,
    onNavigateBack: () -> Unit,
    viewModel: SplitsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current
    var draggedIndex by remember { mutableStateOf(-1) }
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.categoryName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog(true) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Split")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Sum of Bests",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTime(uiState.sumOfBests),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Last PB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTime(uiState.lastPb),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Segments list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.segments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No splits yet.\nTap + to add a split.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(
                        items = uiState.segments,
                        key = { _, segment -> segment.id }
                    ) { index, segment ->
                        SegmentRow(
                            segment = segment,
                            index = index,
                            isDragged = draggedIndex == index,
                            onClick = { viewModel.showEditDialog(segment) },
                            onLongClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                draggedIndex = index
                            },
                            onDragEnd = { toIndex ->
                                if (draggedIndex >= 0 && toIndex >= 0 && draggedIndex != toIndex) {
                                    viewModel.reorderSegments(draggedIndex, toIndex)
                                }
                                draggedIndex = -1
                            },
                            modifier = Modifier
                                .animateItem()
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { _ ->
                                            draggedIndex = index
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragEnd = { draggedIndex = -1 },
                                        onDragCancel = { draggedIndex = -1 },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            // Calculate target position based on drag distance
                                            val itemHeight = 72.dp.toPx()
                                            val currentY = change.position.y
                                            val targetIndex = (index + (currentY / itemHeight).toInt())
                                                .coerceIn(0, uiState.segments.size - 1)
                                            if (targetIndex != draggedIndex && targetIndex >= 0) {
                                                viewModel.reorderSegments(draggedIndex, targetIndex)
                                                draggedIndex = targetIndex
                                            }
                                        }
                                    )
                                }
                        )
                    }
                }
            }
        }
    }

    // Add Segment Dialog
    if (uiState.showAddDialog) {
        AddSegmentDialog(
            currentIndex = uiState.segments.size,
            onDismiss = { viewModel.showAddDialog(false) },
            onConfirm = { name, position -> viewModel.addSegment(name, position) }
        )
    }

    // Edit Segment Dialog
    if (uiState.showEditDialog && uiState.editingSegment != null) {
        EditSegmentDialog(
            name = uiState.editNameText,
            pbTime = uiState.editPbTimeText,
            bestTime = uiState.editBestTimeText,
            onNameChange = { viewModel.updateEditName(it) },
            onPbTimeChange = { viewModel.updateEditPbTime(it) },
            onBestTimeChange = { viewModel.updateEditBestTime(it) },
            onDismiss = { viewModel.showEditDialog(null) },
            onConfirm = { viewModel.saveSegmentEdits() },
            onDelete = {
                viewModel.deleteSegment(uiState.editingSegment!!.id)
                viewModel.showEditDialog(null)
            }
        )
    }
}

@Composable
private fun SegmentRow(
    segment: Segment,
    index: Int,
    isDragged: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragEnd: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .alpha(if (isDragged) 0.5f else 1f)
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
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = segment.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "PB: ${formatTime(segment.pbTimeMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatTime(segment.bestTimeMs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.End
            )
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

@Composable
private fun AddSegmentDialog(
    currentIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var position by remember { mutableIntStateOf(currentIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Split") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Split name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Position:", modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { if (position > 0) position-- }) { Text("-") }
                    Text(
                        "${position + 1}",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    OutlinedButton(onClick = { if (position < currentIndex) position++ }) { Text("+") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, position) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditSegmentDialog(
    name: String,
    pbTime: String,
    bestTime: String,
    onNameChange: (String) -> Unit,
    onPbTimeChange: (String) -> Unit,
    onBestTimeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Split") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Split name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pbTime,
                    onValueChange = onPbTimeChange,
                    label = { Text("PB time (m:ss.mmm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bestTime,
                    onValueChange = onBestTimeChange,
                    label = { Text("Best segment (m:ss.mmm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onConfirm) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(ms: Long): String {
    if (ms == 0L) return "0:00.000"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = ms % 1000
    return "%d:%02d.%03d".format(minutes, seconds, millis)
}
