package com.livesplit.ui.game

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livesplit.data.model.Category

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun GameScreen(
    _gameId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToSplits: (Long) -> Unit,
    onNavigateToTimer: (Long) -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current
    val bottomSheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.game?.name ?: "Game") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog(true) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No categories yet.\nTap + to add a category.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = uiState.categories,
                    key = { it.id }
                ) { category ->
                    CategoryItem(
                        category = category,
                        onClick = { viewModel.showBottomSheet(category) },
                        onLongClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.showEditDialog(category)
                        }
                    )
                }
            }
        }
    }

    // Bottom Sheet
    if (uiState.showBottomSheet && uiState.selectedCategory != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showBottomSheet(null) },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Launch Timer") },
                    supportingContent = uiState.selectedCategory?.let { cat ->
                        {
                            Text(
                                if (cat.personalBestMs > 0) "PB: ${formatTime(cat.personalBestMs)}"
                                else "No PB set"
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        viewModel.showBottomSheet(null)
                        onNavigateToTimer(uiState.selectedCategory!!.id)
                    }
                )
                ListItem(
                    headlineContent = { Text("View Splits") },
                    supportingContent = { Text("${uiState.categories.size} segments") },
                    modifier = Modifier.clickable {
                        viewModel.showBottomSheet(null)
                        onNavigateToSplits(uiState.selectedCategory!!.id)
                    }
                )
            }
        }
    }

    // Add Category Dialog
    if (uiState.showAddDialog) {
        AddCategoryDialog(
            onDismiss = { viewModel.showAddDialog(false) },
            onConfirm = { viewModel.addCategory(it) }
        )
    }

    // Edit Category Dialog
    if (uiState.showEditDialog && uiState.editingCategory != null) {
        EditCategoryDialog(
            category = uiState.editingCategory!!,
            pbText = uiState.editPbText,
            runCountText = uiState.editRunCount,
            onPbChange = { viewModel.updateEditPbText(it) },
            onRunCountChange = { viewModel.updateEditRunCount(it) },
            onDismiss = { viewModel.showEditDialog(null) },
            onConfirm = { viewModel.saveCategoryEdits() },
            onDelete = {
                viewModel.deleteCategory(uiState.editingCategory!!.id)
                viewModel.showEditDialog(null)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryItem(
    category: Category,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(16.dp)
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (category.personalBestMs > 0) "PB: ${formatTime(category.personalBestMs)}" else "No PB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${category.runCount} runs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Category") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Category name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
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
private fun EditCategoryDialog(
    category: Category,
    pbText: String,
    runCountText: String,
    onPbChange: (String) -> Unit,
    onRunCountChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Category") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = pbText,
                    onValueChange = onPbChange,
                    label = { Text("Personal Best (m:ss.mmm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = runCountText,
                    onValueChange = onRunCountChange,
                    label = { Text("Run count") },
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

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.clickable(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier.combinedClickable(onClick = onClick, onLongClick = {})
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
