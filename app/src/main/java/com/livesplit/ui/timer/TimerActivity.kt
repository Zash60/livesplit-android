package com.livesplit.ui.timer

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livesplit.ui.theme.LiveSplitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimerActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_CATEGORY_ID = "EXTRA_CATEGORY_ID"

        fun createIntent(context: Context, categoryId: Long): Intent {
            return Intent(context, TimerActivity::class.java).apply {
                putExtra(EXTRA_CATEGORY_ID, categoryId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up transparent background for overlay
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        setContent {
            LiveSplitTheme {
                TimerOverlay()
            }
        }
    }
}

@Composable
fun TimerOverlay(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val timerColor = Color(uiState.displayColor).let { androidx.compose.ui.graphics.Color(it.red, it.green, it.blue, it.alpha) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = if (uiState.settings.showTimerBackground) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)
        }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Current split name
            if (uiState.settings.showCurrentSplit && uiState.timerState.hasStarted) {
                Text(
                    text = uiState.timerState.currentSplitName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Main timer display
            Text(
                text = uiState.displayTime,
                fontFamily = FontFamily.Monospace,
                fontSize = when (uiState.settings.timerSize) {
                    com.livesplit.data.model.TimerSize.SMALL -> 24.sp
                    com.livesplit.data.model.TimerSize.MEDIUM -> 36.sp
                    com.livesplit.data.model.TimerSize.LARGE -> 48.sp
                    com.livesplit.data.model.TimerSize.EXTRA_LARGE -> 64.sp
                },
                fontWeight = FontWeight.Bold,
                color = timerColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { viewModel.handleTap() },
                            onLongPress = { viewModel.handleLongPress() }
                        )
                    }
            )

            // Delta display
            AnimatedVisibility(
                visible = uiState.currentSplitDelta.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = uiState.currentSplitDelta,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    color = timerColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // New PB indicator
            AnimatedVisibility(
                visible = uiState.hasNewPb,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Text(
                    text = "NEW PB!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }

    // PB Dialog
    if (uiState.showPbDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissPbDialog() },
            title = { Text("New Personal Best!") },
            text = {
                Text("Congratulations! You set a new personal best: ${uiState.displayTime}")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.dismissPbDialog() }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
