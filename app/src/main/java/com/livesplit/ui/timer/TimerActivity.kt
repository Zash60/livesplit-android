package com.livesplit.ui.timer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.livesplit.service.TimerOverlayService
import com.livesplit.ui.theme.LiveSplitTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Activity that launches the timer overlay service.
 * Shows a simple UI to start the timer or close it.
 */
@AndroidEntryPoint
class TimerActivity : ComponentActivity() {

    private var categoryId: Long = -1L
    private var categoryName: String = "Timer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        categoryId = intent.getLongExtra("CATEGORY_ID", -1L)
        categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "Timer"

        setContent {
            LiveSplitTheme {
                TimerLaunchScreen(
                    categoryId = categoryId,
                    categoryName = categoryName,
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If service is running, activity can be closed since overlay is visible
        if (TimerOverlayService.isServiceRunning) {
            finish()
        }
    }
}

@Composable
fun TimerLaunchScreen(
    categoryId: Long,
    categoryName: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    var isServiceRunning by remember { mutableStateOf(TimerOverlayService.isServiceRunning) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
                isServiceRunning = TimerOverlayService.isServiceRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Check service state periodically
    LaunchedEffect(Unit) {
        while (true) {
            isServiceRunning = TimerOverlayService.isServiceRunning
            kotlinx.coroutines.delay(500)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!hasOverlayPermission) {
                // Need overlay permission
                Text(
                    text = "Overlay Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "To display the timer over other apps, LiveSplit needs the \"Display over other apps\" permission.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                ) {
                    @Suppress("DEPRECATION")
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Permission")
                }
            } else if (isServiceRunning) {
                // Service is running
                Text(
                    text = "Timer is Running",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "The timer overlay is visible on your screen.\nYou can drag it to reposition.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, TimerOverlayService::class.java).apply {
                            action = TimerOverlayService.ACTION_CLOSE
                        }
                        context.startService(intent)
                        onClose()
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Close Timer")
                }
            } else {
                // Start the timer
                Text(
                    text = "Start Timer",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val intent = Intent(context, TimerOverlayService::class.java).apply {
                            action = TimerOverlayService.ACTION_START
                            putExtra(TimerOverlayService.EXTRA_CATEGORY_ID, categoryId)
                            putExtra(TimerOverlayService.EXTRA_CATEGORY_NAME, categoryName)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Timer Overlay")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
