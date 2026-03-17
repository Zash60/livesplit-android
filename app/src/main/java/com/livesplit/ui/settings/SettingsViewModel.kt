package com.livesplit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesplit.data.model.AppSettings
import com.livesplit.data.model.Comparison
import com.livesplit.data.model.TimerSize
import com.livesplit.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun updateComparison(comparison: Comparison) {
        viewModelScope.launch {
            settingsRepository.updateComparison(comparison)
        }
    }

    fun updateCountdownMs(ms: Long) {
        viewModelScope.launch {
            settingsRepository.updateCountdownMs(ms)
        }
    }

    fun updateLaunchGamesScreen(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateLaunchGamesScreen(enabled)
        }
    }

    fun updateShowMilliseconds(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowMilliseconds(enabled)
        }
    }

    fun updateShowDelta(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowDelta(enabled)
        }
    }

    fun updateShowCurrentSplit(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowCurrentSplit(enabled)
        }
    }

    fun updateTimerSize(size: TimerSize) {
        viewModelScope.launch {
            settingsRepository.updateTimerSize(size)
        }
    }

    fun updateShowTimerBackground(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowTimerBackground(enabled)
        }
    }

    fun updateColorAhead(color: String) {
        viewModelScope.launch {
            settingsRepository.updateColorAhead(color)
        }
    }

    fun updateColorBehind(color: String) {
        viewModelScope.launch {
            settingsRepository.updateColorBehind(color)
        }
    }

    fun updateColorBest(color: String) {
        viewModelScope.launch {
            settingsRepository.updateColorBest(color)
        }
    }
}
