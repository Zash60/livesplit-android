package com.livesplit.data.model

/**
 * App settings stored in DataStore.
 */
data class AppSettings(
    val comparison: Comparison = Comparison.PERSONAL_BEST,
    val countdownMs: Long = 0,
    val launchGamesScreen: Boolean = true,
    val showMilliseconds: Boolean = true,
    val showDelta: Boolean = true,
    val showCurrentSplit: Boolean = true,
    val timerSize: TimerSize = TimerSize.MEDIUM,
    val showTimerBackground: Boolean = true,
    val colorAhead: String = "#4CAF50",       // Green
    val colorBehind: String = "#F44336",      // Red
    val colorBest: String = "#2196F3"         // Blue (new PB)
)

enum class Comparison {
    PERSONAL_BEST,
    BEST_SEGMENTS,
    BEST_PLUS
}

enum class TimerSize {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE
}
