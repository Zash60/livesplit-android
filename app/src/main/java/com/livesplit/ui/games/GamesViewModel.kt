package com.livesplit.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesplit.data.model.Game
import com.livesplit.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GamesUiState(
    val games: List<Game> = emptyList(),
    val isLoading: Boolean = true,
    val selectedGameIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val showAddDialog: Boolean = false,
    val showRenameDialog: Boolean = false,
    val renameGameId: Long? = null,
    val renameText: String = ""
)

@HiltViewModel
class GamesViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamesUiState())
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    init {
        loadGames()
    }

    private fun loadGames() {
        viewModelScope.launch {
            gameRepository.getAllGames()
                .collect { games ->
                    _uiState.update { it.copy(games = games, isLoading = false) }
                }
        }
    }

    fun addGame(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            gameRepository.addGame(name.trim())
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    fun deleteGame(gameId: Long) {
        viewModelScope.launch {
            gameRepository.deleteGameById(gameId)
            _uiState.update {
                it.copy(
                    selectedGameIds = it.selectedGameIds - gameId,
                    isSelectionMode = it.selectedGameIds.size > 1
                )
            }
        }
    }

    fun deleteSelectedGames() {
        val selectedIds = _uiState.value.selectedGameIds
        viewModelScope.launch {
            selectedIds.forEach { id ->
                gameRepository.deleteGameById(id)
            }
            _uiState.update {
                it.copy(selectedGameIds = emptySet(), isSelectionMode = false)
            }
        }
    }

    fun toggleSelection(gameId: Long) {
        _uiState.update { state ->
            val newSelection = if (gameId in state.selectedGameIds) {
                state.selectedGameIds - gameId
            } else {
                state.selectedGameIds + gameId
            }
            state.copy(
                selectedGameIds = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedGameIds = emptySet(), isSelectionMode = false) }
    }

    fun showAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show) }
    }

    fun showRenameDialog(gameId: Long?, currentName: String = "") {
        _uiState.update {
            it.copy(
                showRenameDialog = gameId != null,
                renameGameId = gameId,
                renameText = currentName
            )
        }
    }

    fun updateRenameText(text: String) {
        _uiState.update { it.copy(renameText = text) }
    }

    fun renameGame() {
        val state = _uiState.value
        val gameId = state.renameGameId ?: return
        val newName = state.renameText.trim()
        if (newName.isBlank()) return

        viewModelScope.launch {
            gameRepository.renameGame(gameId, newName)
            _uiState.update {
                it.copy(
                    showRenameDialog = false,
                    renameGameId = null,
                    renameText = "",
                    selectedGameIds = emptySet(),
                    isSelectionMode = false
                )
            }
        }
    }
}
