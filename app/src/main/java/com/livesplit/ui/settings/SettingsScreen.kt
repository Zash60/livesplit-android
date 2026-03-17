package com.livesplit.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livesplit.data.model.AppSettings
import com.livesplit.data.model.Comparison
import com.livesplit.data.model.TimerSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timer Section
            SettingsSectionHeader("Timer")

            // Comparison mode
            ComparisonSetting(
                current = settings.comparison,
                onValueChange = { viewModel.updateComparison(it) }
            )

            // Countdown
            CountdownSetting(
                currentMs = settings.countdownMs,
                onValueChange = { viewModel.updateCountdownMs(it) }
            )

            // Timer Size
            TimerSizeSetting(
                current = settings.timerSize,
                onValueChange = { viewModel.updateTimerSize(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Display Section
            SettingsSectionHeader("Display")

            // Show milliseconds
            SwitchSetting(
                title = "Show milliseconds",
                description = "Display milliseconds in timer",
                checked = settings.showMilliseconds,
                onCheckedChange = { viewModel.updateShowMilliseconds(it) }
            )

            // Show delta
            SwitchSetting(
                title = "Show delta",
                description = "Display time ahead/behind PB",
                checked = settings.showDelta,
                onCheckedChange = { viewModel.updateShowDelta(it) }
            )

            // Show current split
            SwitchSetting(
                title = "Show current split",
                description = "Display current split name above timer",
                checked = settings.showCurrentSplit,
                onCheckedChange = { viewModel.updateShowCurrentSplit(it) }
            )

            // Show timer background
            SwitchSetting(
                title = "Timer background",
                description = "Show background behind timer overlay",
                checked = settings.showTimerBackground,
                onCheckedChange = { viewModel.updateShowTimerBackground(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Colors Section
            SettingsSectionHeader("Colors")

            Text(
                text = "Note: Color settings use hex format (e.g., #4CAF50)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Color ahead (green)
            ColorSetting(
                title = "Ahead (green)",
                current = settings.colorAhead,
                onValueChange = { viewModel.updateColorAhead(it) }
            )

            // Color behind (red)
            ColorSetting(
                title = "Behind (red)",
                current = settings.colorBehind,
                onValueChange = { viewModel.updateColorBehind(it) }
            )

            // Color best (blue)
            ColorSetting(
                title = "New PB (blue)",
                current = settings.colorBest,
                onValueChange = { viewModel.updateColorBest(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Startup Section
            SettingsSectionHeader("Startup")

            // Launch games screen
            SwitchSetting(
                title = "Launch to Games",
                description = "Show games list on app start",
                checked = settings.launchGamesScreen,
                onCheckedChange = { viewModel.updateLaunchGamesScreen(it) }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComparisonSetting(
    current: Comparison,
    onValueChange: (Comparison) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Comparison",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Compare against which baseline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            TextField(
                value = current.name.replace("_", " "),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Comparison.values().forEach { comparison ->
                    DropdownMenuItem(
                        text = { Text(comparison.name.replace("_", " ")) },
                        onClick = {
                            onValueChange(comparison)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerSizeSetting(
    current: TimerSize,
    onValueChange: (TimerSize) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Timer size",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Text size for timer display",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            TextField(
                value = current.name.replace("_", " "),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                TimerSize.values().forEach { size ->
                    DropdownMenuItem(
                        text = { Text(size.name.replace("_", " ")) },
                        onClick = {
                            onValueChange(size)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownSetting(
    currentMs: Long,
    onValueChange: (Long) -> Unit
) {
    var text by remember(currentMs) { mutableStateOf(if (currentMs > 0) (currentMs / 1000).toString() else "") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Countdown",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Seconds before timer starts (0 = disabled)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { newValue ->
                text = newValue
                val seconds = newValue.toLongOrNull() ?: 0L
                onValueChange(seconds * 1000)
            },
            modifier = Modifier.width(80.dp),
            singleLine = true
        )
    }
}

@Composable
private fun ColorSetting(
    title: String,
    current: String,
    onValueChange: (String) -> Unit
) {
    var text by remember(current) { mutableStateOf(current) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { newValue ->
                text = newValue
                if (newValue.startsWith("#") && newValue.length == 7) {
                    onValueChange(newValue)
                }
            },
            modifier = Modifier.width(100.dp),
            singleLine = true
        )
    }
}
