package com.livesplit.ui.timer

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.livesplit.data.model.*
import com.livesplit.data.repository.CategoryRepository
import com.livesplit.data.repository.SegmentRepository
import com.livesplit.data.repository.SettingsRepository
import com.livesplit.service.TimerService
import com.livesplit.util.TimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class TimerUiState(
    val categoryName: String = "",
    val timerState: TimerState = TimerState(),
    val settings: AppSettings = AppSettings(),
    val displayTime: String = "0:00.000",
    val displayColor: Long = 0xFFFFFFFF, // White
    val currentSplitDelta: String = "",
    val hasNewPb: Boolean = false,
    val showPbDialog: Boolean = false
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val segmentRepository: SegmentRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val categoryId: Long = savedStateHandle.get<String>("categoryId")?.toLongOrNull() ?: -1L

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var startTimeStamp: Long = 0L
    private var pausedElapsed: Long = 0L

    init {
        loadCategory()
        loadSettings()
    }

    private fun loadCategory() {
        viewModelScope.launch {
            val category = categoryRepository.getCategoryById(categoryId)
            _uiState.update { it.copy(categoryName = category?.name ?: "Timer") }

            val segments = segmentRepository.getSegmentsByCategoryIdSync(categoryId)
            _uiState.update {
                it.copy(
                    timerState = it.timerState.copy(segments = segments)
                )
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun handleTap() {
        val state = _uiState.value.timerState

        when {
            !state.hasStarted -> startTimer()
            !state.isFinished -> split()
            else -> resetTimer()
        }
    }

    fun handleLongPress() {
        val state = _uiState.value.timerState
        if (state.hasStarted) {
            resetTimer()
        }
    }

    private fun startTimer() {
        val state = _uiState.value.timerState
        val settings = _uiState.value.settings

        startTimeStamp = System.currentTimeMillis() - pausedElapsed

        val newState = state.copy(
            isRunning = true,
            splitIndex = 0,
            startTimeMs = startTimeStamp,
            countdownMs = settings.countdownMs
        )
        _uiState.update { it.copy(timerState = newState) }

        startTimerUpdates()

        // Start foreground service
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_CATEGORY_ID, categoryId)
            putExtra(TimerService.EXTRA_CATEGORY_NAME, _uiState.value.categoryName)
        }
        getApplication<Application>().startService(intent)
    }

    private fun split() {
        val state = _uiState.value.timerState
        val segments = state.segments

        if (state.splitIndex >= segments.size) {
            // Already finished
            return
        }

        val currentSegment = segments[state.splitIndex]
        val elapsed = state.elapsedMs
        val previousSplitTime = if (state.splitIndex > 0) {
            state.results.getOrNull(state.splitIndex - 1)?.splitTimeMs ?: 0L
        } else {
            0L
        }

        val segmentTime = elapsed - previousSplitTime
        val delta = elapsed - currentSegment.pbTimeMs

        val result = SegmentResult(
            segmentId = currentSegment.id,
            segmentName = currentSegment.name,
            splitTimeMs = elapsed,
            segmentTimeMs = segmentTime,
            pbTimeMs = currentSegment.pbTimeMs,
            bestTimeMs = currentSegment.bestTimeMs,
            deltaMs = delta
        )

        val newResults = state.results + result
        val newIndex = state.splitIndex + 1
        val isFinished = newIndex >= segments.size

        val newState = state.copy(
            splitIndex = newIndex,
            results = newResults,
            isFinished = isFinished
        )
        _uiState.update { it.copy(timerState = newState) }

        // Update display
        updateDisplay(newState)

        if (isFinished) {
            finishRun(elapsed)
        }
    }

    private fun startTimerUpdates() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = now - startTimeStamp
                val countdown = _uiState.value.timerState.countdownMs
                val displayElapsed = if (countdown > 0) countdown - elapsed else elapsed

                _uiState.update { state ->
                    val newState = state.timerState.copy(elapsedMs = displayElapsed)
                    state.copy(timerState = newState)
                }

                updateDisplay(_uiState.value.timerState)
                delay(16) // ~60fps
            }
        }
    }

    private fun updateDisplay(state: TimerState) {
        val settings = _uiState.value.settings
        val elapsed = state.elapsedMs

        val displayTime = TimeFormatter.format(elapsed, settings.showMilliseconds)

        // Calculate delta color
        val color = if (state.hasStarted && !state.isFinished && state.results.isNotEmpty()) {
            val lastResult = state.results.last()
            when {
                lastResult.deltaMs < 0 -> parseColor(settings.colorAhead)  // Ahead - green
                lastResult.deltaMs > 0 -> parseColor(settings.colorBehind) // Behind - red
                else -> 0xFFFFFFFF // White
            }
        } else {
            0xFFFFFFFF // White
        }

        val deltaText = if (settings.showDelta && state.hasStarted && state.results.isNotEmpty()) {
            val lastResult = state.results.last()
            TimeFormatter.formatDelta(lastResult.deltaMs, settings.showMilliseconds)
        } else {
            ""
        }

        _uiState.update {
            it.copy(
                displayTime = displayTime,
                displayColor = color,
                currentSplitDelta = deltaText
            )
        }
    }

    private fun finishRun(finalTime: Long) {
        timerJob?.cancel()

        viewModelScope.launch {
            val category = categoryRepository.getCategoryById(categoryId) ?: return@launch
            val isNewPb = category.personalBestMs == 0L || finalTime < category.personalBestMs

            if (isNewPb) {
                _uiState.update { it.copy(hasNewPb = true, showPbDialog = true) }
            }

            categoryRepository.incrementRunCount(categoryId)
            if (isNewPb) {
                categoryRepository.updatePersonalBest(categoryId, finalTime)

                // Update segment PB times
                _uiState.value.timerState.results.forEach { result ->
                    segmentRepository.updatePbTime(result.segmentId, result.splitTimeMs)
                }
            }

            // Update best segment times
            _uiState.value.timerState.results.forEach { result ->
                if (result.segmentTimeMs > 0) {
                    val segment = segmentRepository.getSegmentById(result.segmentId)
                    if (segment != null && (segment.bestTimeMs == 0L || result.segmentTimeMs < segment.bestTimeMs)) {
                        segmentRepository.updateBestTime(result.segmentId, result.segmentTimeMs)
                    }
                }
            }

            // Stop service
            val intent = Intent(getApplication(), TimerService::class.java).apply {
                action = TimerService.ACTION_STOP
            }
            getApplication<Application>().startService(intent)
        }
    }

    private fun resetTimer() {
        timerJob?.cancel()
        pausedElapsed = 0L

        viewModelScope.launch {
            val segments = segmentRepository.getSegmentsByCategoryIdSync(categoryId)
            _uiState.update {
                it.copy(
                    timerState = TimerState(
                        segments = segments,
                        countdownMs = it.settings.countdownMs
                    ),
                    displayTime = TimeFormatter.format(0L, it.settings.showMilliseconds),
                    displayColor = 0xFFFFFFFF,
                    currentSplitDelta = "",
                    hasNewPb = false,
                    showPbDialog = false
                )
            }
        }

        // Stop service
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }

    fun dismissPbDialog() {
        _uiState.update { it.copy(showPbDialog = false) }
    }

    private fun parseColor(colorStr: String): Long {
        return try {
            android.graphics.Color.parseColor(colorStr).toLong() and 0xFFFFFFFF
        } catch (e: Exception) {
            0xFFFFFFFF
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
