package com.livesplit.util

import java.util.Locale

/**
 * Utility for formatting time values in the LiveSplit app.
 */
object TimeFormatter {

    /**
     * Format milliseconds to "m:ss.mmm" or "m:ss.SSS" format.
     * @param ms Time in milliseconds
     * @param showMs Whether to show milliseconds (true) or hide them (false)
     */
    fun format(ms: Long, showMs: Boolean = true): String {
        val isNegative = ms < 0
        val absMs = Math.abs(ms)

        val totalSeconds = absMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = absMs % 1000

        val prefix = if (isNegative) "-" else ""

        return if (showMs) {
            "%s%d:%02d.%03d".format(prefix, minutes, seconds, millis)
        } else {
            "%s%d:%02d".format(prefix, minutes, seconds)
        }
    }

    /**
     * Format to hours if needed.
     */
    fun formatWithHours(ms: Long, showMs: Boolean = true): String {
        val isNegative = ms < 0
        val absMs = Math.abs(ms)

        val totalSeconds = absMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val millis = absMs % 1000

        val prefix = if (isNegative) "-" else ""

        return if (hours > 0) {
            if (showMs) {
                "%s%d:%02d:%02d.%03d".format(prefix, hours, minutes, seconds, millis)
            } else {
                "%s%d:%02d:%02d".format(prefix, hours, minutes, seconds)
            }
        } else {
            format(ms, showMs)
        }
    }

    /**
     * Format delta time with +/- prefix.
     * Negative delta means ahead (good), positive means behind (bad).
     */
    fun formatDelta(ms: Long, showMs: Boolean = true): String {
        if (ms == 0L) return ""

        val sign = if (ms < 0) "-" else "+"
        val absMs = Math.abs(ms)

        val totalSeconds = absMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = absMs % 1000

        return if (showMs && absMs < 60000) {
            // Under a minute, show seconds.milliseconds
            "%s%d.%03d".format(sign, seconds, millis)
        } else if (showMs) {
            "%s%d:%02d.%03d".format(sign, minutes, seconds, millis)
        } else {
            "%s%d:%02d".format(sign, minutes, seconds)
        }
    }

    /**
     * Parse a time string like "1:23.456" or "23.456" to milliseconds.
     */
    fun parse(timeStr: String): Long {
        if (timeStr.isBlank()) return 0L

        val isNegative = timeStr.startsWith("-")
        val cleanStr = timeStr.removePrefix("-")

        return try {
            val parts = cleanStr.split(":", ".")
            val ms = when (parts.size) {
                3 -> (parts[0].toLong() * 60000) + (parts[1].toLong() * 1000) + parts[2].toLong()
                2 -> {
                    if (cleanStr.contains(".")) {
                        (parts[0].toLong() * 1000) + parts[1].toLong()
                    } else {
                        (parts[0].toLong() * 60000) + (parts[1].toLong() * 1000)
                    }
                }
                1 -> {
                    if (cleanStr.contains(".")) {
                        parts[0].toLong()
                    } else {
                        parts[0].toLong() * 1000
                    }
                }
                else -> 0L
            }
            if (isNegative) -ms else ms
        } catch (e: Exception) {
            0L
        }
    }
}
