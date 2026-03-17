package com.livesplit.data.model

/**
 * Result of a single split during a timer run.
 * Not persisted - used only during active timer.
 */
data class SegmentResult(
    val segmentId: Long,
    val segmentName: String,
    val splitTimeMs: Long,      // Cumulative time at this split
    val segmentTimeMs: Long,    // Time for just this segment
    val pbTimeMs: Long,         // PB cumulative at this split
    val bestTimeMs: Long,       // Best segment time
    val deltaMs: Long           // Difference from PB (negative = ahead)
)
