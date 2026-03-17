package com.livesplit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.livesplit.data.model.AppSettings
import com.livesplit.data.model.Comparison
import com.livesplit.data.model.TimerSize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val COMPARISON = stringPreferencesKey("comparison")
        val COUNTDOWN_MS = longPreferencesKey("countdown_ms")
        val LAUNCH_GAMES_SCREEN = booleanPreferencesKey("launch_games_screen")
        val SHOW_MILLISECONDS = booleanPreferencesKey("show_milliseconds")
        val SHOW_DELTA = booleanPreferencesKey("show_delta")
        val SHOW_CURRENT_SPLIT = booleanPreferencesKey("show_current_split")
        val TIMER_SIZE = stringPreferencesKey("timer_size")
        val SHOW_TIMER_BACKGROUND = booleanPreferencesKey("show_timer_background")
        val COLOR_AHEAD = stringPreferencesKey("color_ahead")
        val COLOR_BEHIND = stringPreferencesKey("color_behind")
        val COLOR_BEST = stringPreferencesKey("color_best")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            comparison = prefs[Keys.COMPARISON]?.let { Comparison.valueOf(it) }
                ?: Comparison.PERSONAL_BEST,
            countdownMs = prefs[Keys.COUNTDOWN_MS] ?: 0L,
            launchGamesScreen = prefs[Keys.LAUNCH_GAMES_SCREEN] ?: true,
            showMilliseconds = prefs[Keys.SHOW_MILLISECONDS] ?: true,
            showDelta = prefs[Keys.SHOW_DELTA] ?: true,
            showCurrentSplit = prefs[Keys.SHOW_CURRENT_SPLIT] ?: true,
            timerSize = prefs[Keys.TIMER_SIZE]?.let { TimerSize.valueOf(it) }
                ?: TimerSize.MEDIUM,
            showTimerBackground = prefs[Keys.SHOW_TIMER_BACKGROUND] ?: true,
            colorAhead = prefs[Keys.COLOR_AHEAD] ?: "#4CAF50",
            colorBehind = prefs[Keys.COLOR_BEHIND] ?: "#F44336",
            colorBest = prefs[Keys.COLOR_BEST] ?: "#2196F3"
        )
    }

    suspend fun updateComparison(comparison: Comparison) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COMPARISON] = comparison.name
        }
    }

    suspend fun updateCountdownMs(ms: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COUNTDOWN_MS] = ms
        }
    }

    suspend fun updateLaunchGamesScreen(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAUNCH_GAMES_SCREEN] = enabled
        }
    }

    suspend fun updateShowMilliseconds(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_MILLISECONDS] = enabled
        }
    }

    suspend fun updateShowDelta(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_DELTA] = enabled
        }
    }

    suspend fun updateShowCurrentSplit(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_CURRENT_SPLIT] = enabled
        }
    }

    suspend fun updateTimerSize(size: TimerSize) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TIMER_SIZE] = size.name
        }
    }

    suspend fun updateShowTimerBackground(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_TIMER_BACKGROUND] = enabled
        }
    }

    suspend fun updateColorAhead(color: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COLOR_AHEAD] = color
        }
    }

    suspend fun updateColorBehind(color: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COLOR_BEHIND] = color
        }
    }

    suspend fun updateColorBest(color: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COLOR_BEST] = color
        }
    }
}
