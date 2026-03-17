package com.livesplit.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Permission states for the app
 */
data class AppPermissionState(
    val notificationsGranted: Boolean = false,
    val overlayGranted: Boolean = false
)

/**
 * Dialog to request notification permission (Android 13+)
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionDialog(
    onResult: (Boolean) -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // Permission not needed for older Android versions
        LaunchedEffect(Unit) { onResult(true) }
        return
    }

    val permissionState = rememberPermissionState(
        permission = android.Manifest.permission.POST_NOTIFICATIONS,
        onPermissionResult = { granted ->
            onResult(granted)
        }
    )

    if (permissionState.status.shouldShowRationale) {
        AlertDialog(
            onDismissRequest = { onResult(false) },
            title = {
                Text(
                    text = "Notifications Required",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "LiveSplit uses notifications to show the timer when running in the background. " +
                    "This allows you to see your splits while using other apps."
                )
            },
            confirmButton = {
                TextButton(onClick = { permissionState.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { onResult(false) }) {
                    Text("Not Now")
                }
            }
        )
    } else {
        LaunchedEffect(Unit) {
            permissionState.launchPermissionRequest()
        }
    }
}

/**
 * Dialog to request overlay permission (for timer overlay)
 */
@Composable
fun OverlayPermissionDialog(
    onResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var hasRequested by remember { mutableStateOf(false) }

    val canDrawOverlays = android.provider.Settings.canDrawOverlays(context)

    LaunchedEffect(Unit) {
        if (canDrawOverlays) {
            onResult(true)
        } else {
            showRationale = true
        }
    }

    if (showRationale && !hasRequested) {
        AlertDialog(
            onDismissRequest = { onResult(false) },
            title = {
                Text(
                    text = "Overlay Permission",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "LiveSplit needs permission to draw over other apps to display the timer overlay " +
                    "while you're playing. This is essential for the timer to work as an overlay."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                    hasRequested = true
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { onResult(false) }) {
                    Text("Not Now")
                }
            }
        )
    }

    // Check result when returning from settings
    LaunchedEffect(hasRequested) {
        if (hasRequested) {
            // Give user time to grant in settings
            kotlinx.coroutines.delay(1000)
            onResult(android.provider.Settings.canDrawOverlays(context))
        }
    }
}

/**
 * Combined permission request screen shown at app start
 */
@Composable
fun PermissionRequestScreen(
    onComplete: (AppPermissionState) -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var notificationGranted by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(false) }

    // Check if overlay permission is already granted
    val initialOverlayGranted = android.provider.Settings.canDrawOverlays(context)

    LaunchedEffect(Unit) {
        if (initialOverlayGranted) {
            overlayGranted = true
            step = 1 // Skip overlay step
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (step) {
            0 -> {
                // Notification permission
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    // Skip notification permission for older Android
                    notificationGranted = true
                    step = 1
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Step 1 of 2",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Notification Permission",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "LiveSplit needs to show notifications when the timer is running in the background. " +
                                   "This lets you see your splits while using other apps.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    // Request permission will be triggered via callback
                                    notificationGranted = true
                                    step = 1
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                notificationGranted = false
                                step = 1
                            }
                        ) {
                            Text("Skip")
                        }
                    }

                    // Show system permission dialog
                    NotificationPermissionDialog { granted ->
                        notificationGranted = granted
                        step = 1
                    }
                }
            }
            1 -> {
                // Overlay permission
                if (initialOverlayGranted) {
                    overlayGranted = true
                    onComplete(AppPermissionState(notificationGranted, overlayGranted))
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Step 2 of 2",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Overlay Permission",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "To display the timer over other apps, LiveSplit needs the \"Display over other apps\" permission. " +
                                   "This is essential for the timer overlay feature.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Settings")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                overlayGranted = false
                                onComplete(AppPermissionState(notificationGranted, overlayGranted))
                            }
                        ) {
                            Text("Skip")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                overlayGranted = android.provider.Settings.canDrawOverlays(context)
                                onComplete(AppPermissionState(notificationGranted, overlayGranted))
                            },
                            colors = ButtonDefaults.textButtonColors()
                        ) {
                            Text("I've Granted Permission")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating permission indicator for current permission status
 */
@Composable
fun PermissionStatusBar(
    permissionState: AppPermissionState,
    onRequestPermissions: () -> Unit
) {
    val allGranted = permissionState.notificationsGranted && permissionState.overlayGranted

    AnimatedVisibility(
        visible = !allGranted,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Some permissions are missing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                TextButton(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Fix")
                }
            }
        }
    }
}
