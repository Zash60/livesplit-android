package com.livesplit.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.livesplit.MainActivity
import com.livesplit.R
import com.livesplit.data.db.AppDatabase
import com.livesplit.data.model.Segment
import com.livesplit.data.model.SegmentResult
import com.livesplit.ui.timer.TimerOverlayContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that manages a floating timer overlay.
 * The timer can be dragged, tapped to split, and long-pressed to reset.
 */
class TimerOverlayService : Service(), LifecycleOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private var serviceScope: CoroutineScope? = null
    private var timerJob: Job? = null
    private var startTimeStamp: Long = 0L
    private var pausedElapsed: Long = 0L

    // Timer state
    var isRunning by mutableStateOf(false)
    var elapsedMs by mutableLongStateOf(0L)
    var segments by mutableStateOf<List<Segment>>(emptyList())
    var results by mutableStateOf<List<SegmentResult>>(emptyList())
    var splitIndex by mutableStateOf(-1)
    var categoryId by mutableStateOf(-1L)
    var categoryName by mutableStateOf("")
    var currentCategoryPb by mutableLongStateOf(0L)
    var showPbDialog by mutableStateOf(false)
    var newPbTime by mutableLongStateOf(0L)

    // Position tracking for drag
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var hasMoved = false
    private var touchStartTime = 0L

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "timer_overlay_channel"

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CLOSE = "ACTION_CLOSE"

        const val EXTRA_CATEGORY_ID = "EXTRA_CATEGORY_ID"
        const val EXTRA_CATEGORY_NAME = "EXTRA_CATEGORY_NAME"

        // SharedPreferences for position
        private const val PREFS_NAME = "timer_overlay_prefs"
        private const val KEY_POS_X = "pos_x"
        private const val KEY_POS_Y = "pos_y"

        var isServiceRunning = false
            private set

        fun getPositionPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        }

        fun savePosition(context: Context, x: Int, y: Int) {
            getPositionPrefs(context).edit()
                .putInt(KEY_POS_X, x)
                .putInt(KEY_POS_Y, y)
                .apply()
        }

        fun getPosition(context: Context): Pair<Int, Int> {
            val prefs = getPositionPrefs(context)
            return Pair(
                prefs.getInt(KEY_POS_X, WindowManager.LayoutParams.WRAP_CONTENT),
                prefs.getInt(KEY_POS_Y, 100)
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        serviceScope = CoroutineScope(Dispatchers.Main)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1)
                categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "Timer"
                isServiceRunning = true
                loadSegments()
                startForeground(NOTIFICATION_ID, createNotification())
                showOverlay()
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            }
            ACTION_STOP, ACTION_CLOSE -> {
                stopTimer()
                removeOverlay()
                isServiceRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope?.cancel()
        timerJob?.cancel()
        removeOverlay()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    private fun loadSegments() {
        serviceScope?.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val loadedSegments = db.segmentDao().getSegmentsByCategoryIdSync(categoryId)
                segments = loadedSegments
                resetTimerState()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun showOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val overlayView = ComposeView(this).apply {
            setContent {
                TimerOverlayContent(
                    elapsedMs = elapsedMs,
                    isRunning = isRunning,
                    splitIndex = splitIndex,
                    segments = segments,
                    results = results,
                    showPbDialog = showPbDialog,
                    newPbTime = newPbTime,
                    onTap = { handleTap() },
                    onLongPress = { handleLongPress() },
                    categoryName = categoryName
                )
            }
        }

        this.overlayView = overlayView

        // Set up lifecycle for ComposeView
        overlayView.setViewTreeLifecycleOwner(this)
        overlayView.setViewTreeViewModelStoreOwner(null)

        val (savedX, savedY) = getPosition(this)

        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            gravity = Gravity.TOP or Gravity.START
            x = if (savedX == Int.MAX_VALUE) 0 else savedX
            y = savedY
        }

        // Set up touch listener for drag
        overlayView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    hasMoved = false
                    touchStartTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        hasMoved = true
                    }

                    if (isDragging) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(overlayView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - touchStartTime

                    if (!hasMoved) {
                        // It was a click/tap
                        if (duration < 500) {
                            // Short tap
                            handleTap()
                        } else {
                            // Long press
                            handleLongPress()
                        }
                    } else {
                        // Save position after drag
                        savePosition(this@TimerOverlayService, params.x, params.y)
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            // Permission not granted
            stopSelf()
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // View already removed
            }
        }
        overlayView = null
    }

    private fun updateOverlay() {
        // Trigger recomposition by updating the ComposeView
        (overlayView as? ComposeView)?.setContent {
            TimerOverlayContent(
                elapsedMs = elapsedMs,
                isRunning = isRunning,
                splitIndex = splitIndex,
                segments = segments,
                results = results,
                showPbDialog = showPbDialog,
                newPbTime = newPbTime,
                onTap = { handleTap() },
                onLongPress = { handleLongPress() },
                categoryName = categoryName
            )
        }
    }

    private fun handleTap() {
        if (isRunning) {
            // Split or stop
            if (splitIndex < segments.size - 1) {
                performSplit()
            } else {
                // Final split or no splits - stop
                performSplit() // Complete the split first
                finishRun()
            }
        } else if (splitIndex < 0) {
            // Start timer
            startTimer()
        } else if (splitIndex >= segments.size - 1) {
            // Already finished, restart
            resetTimerState()
            startTimer()
        }
        updateOverlay()
    }

    private fun handleLongPress() {
        if (!isRunning && splitIndex >= 0) {
            // Reset with option to save PB
            val finalTime = elapsedMs

            if (finalTime > 0 && (currentCategoryPb == 0L || finalTime < currentCategoryPb)) {
                // New PB - show dialog
                newPbTime = finalTime
                showPbDialog = true
                updateOverlay()
            } else {
                // Just reset
                resetTimerState()
                updateOverlay()
            }
        } else if (isRunning) {
            // Running - instant reset without saving
            timerJob?.cancel()
            resetTimerState()
            updateOverlay()
        }
    }

    private fun startTimer() {
        isRunning = true
        startTimeStamp = System.currentTimeMillis() - pausedElapsed

        timerJob = serviceScope?.launch {
            while (isRunning) {
                elapsedMs = System.currentTimeMillis() - startTimeStamp
                updateOverlay()
                delay(33) // ~30fps update
            }
        }
    }

    private fun performSplit() {
        if (splitIndex >= segments.size - 1) return

        val currentSegment = segments[splitIndex]
        val previousTime = if (splitIndex > 0) results.getOrNull(splitIndex - 1)?.splitTimeMs ?: 0L else 0L
        val segmentTime = elapsedMs - previousTime
        val delta = elapsedMs - currentSegment.pbTimeMs

        val result = SegmentResult(
            segmentId = currentSegment.id,
            segmentName = currentSegment.name,
            splitTimeMs = elapsedMs,
            segmentTimeMs = segmentTime,
            pbTimeMs = currentSegment.pbTimeMs,
            bestTimeMs = currentSegment.bestTimeMs,
            deltaMs = delta
        )

        results = results + result
        splitIndex++
    }

    private fun finishRun() {
        timerJob?.cancel()
        isRunning = false

        val finalTime = elapsedMs

        serviceScope?.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val isNewPb = currentCategoryPb == 0L || finalTime < currentCategoryPb

                // Update run count
                val category = db.categoryDao().getCategoryById(categoryId)
                category?.let {
                    db.categoryDao().incrementRunCount(categoryId)
                }

                if (isNewPb) {
                    // Save new PB
                    db.categoryDao().updatePersonalBest(categoryId, finalTime)
                    newPbTime = finalTime
                    showPbDialog = true

                    // Update segment PB times
                    results.forEach { result ->
                        db.segmentDao().updatePbTime(result.segmentId, result.splitTimeMs)
                    }
                }

                // Update best segment times
                results.forEach { result ->
                    if (result.segmentTimeMs > 0) {
                        val segment = db.segmentDao().getSegmentById(result.segmentId)
                        if (segment != null && (segment.bestTimeMs == 0L || result.segmentTimeMs < segment.bestTimeMs)) {
                            db.segmentDao().updateBestTime(result.segmentId, result.segmentTimeMs)
                        }
                    }
                }

                updateOverlay()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        isRunning = false
        pausedElapsed = elapsedMs
    }

    fun resetTimerState() {
        timerJob?.cancel()
        isRunning = false
        elapsedMs = 0L
        splitIndex = -1
        results = emptyList()
        pausedElapsed = 0L
        showPbDialog = false
    }

    fun dismissPbDialog() {
        showPbDialog = false
        resetTimerState()
        updateOverlay()
    }

    fun savePb() {
        showPbDialog = false
        // PB already saved in finishRun
        resetTimerState()
        updateOverlay()
    }

    private fun createNotification(): Notification {
        val closeIntent = Intent(this, TimerOverlayService::class.java).apply {
            action = ACTION_CLOSE
        }
        val closePendingIntent = PendingIntent.getService(
            this, 0, closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LiveSplit - $categoryName")
            .setContentText(if (isRunning) "Running: ${formatTime(elapsedMs)}" else "Timer ready")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_notification, "Close Timer", closePendingIntent)
            .setContentIntent(openPendingIntent)
            .build()
    }
}

// Time formatting helper
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = ms % 1000
    return "%d:%02d.%03d".format(minutes, seconds, millis)
}
