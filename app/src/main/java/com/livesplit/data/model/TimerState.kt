package com.livesplit.data.model

/**
 * Timer state managed by TimerViewModel.
 * Not persisted to Room - lives only in memory during a run.
 */
data class TimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0,
    val splitIndex: Int = -1,        // -1 = not started, 0+ = current split index
    val segments: List<Segment> = emptyList(),
    val results: List<SegmentResult> = emptyList(),
    val startTimeMs: Long = 0,       // System.currentTimeMillis when started
    val pausedElapsedMs: Long = 0,   // Elapsed time when paused
    val countdownMs: Long = 0        // Negative start countdown (0 = disabled)
) {
    val currentSplitName: String
        get() = if (splitIndex in segments.indices) segments[splitIndex].name else ""
    
    val hasStarted: Boolean
        get() = splitIndex >= 0
    
    val isFinished: Boolean
        get() = splitIndex >= segments.size
    
    val splitsRemaining: Int
        get() = maxOf(0, segments.size - splitIndex)
}
