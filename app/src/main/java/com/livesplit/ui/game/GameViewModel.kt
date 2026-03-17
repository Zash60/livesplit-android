package com.livesplit.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesplit.data.model.Category
import com.livesplit.data.model.Game
import com.livesplit.data.repository.CategoryRepository
import com.livesplit.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameUiState(
    val game: Game? = null,
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingCategory: Category? = null,
    val editPbText: String = "",
    val editRunCount: String = "",
    val showBottomSheet: Boolean = false,
    val selectedCategory: Category? = null
)

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gameRepository: GameRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val gameId: Long = savedStateHandle.get<String>("gameId")?.toLongOrNull() ?: -1L

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        loadGame()
        loadCategories()
    }

    private fun loadGame() {
        viewModelScope.launch {
            val game = gameRepository.getGameById(gameId)
            _uiState.update { it.copy(game = game, isLoading = false) }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategoriesByGameId(gameId)
                .collect { categories ->
                    _uiState.update { it.copy(categories = categories) }
                }
        }
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.addCategory(gameId, name.trim())
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    fun showAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show) }
    }

    fun showBottomSheet(category: Category?) {
        _uiState.update {
            it.copy(
                showBottomSheet = category != null,
                selectedCategory = category
            )
        }
    }

    fun showEditDialog(category: Category?) {
        if (category == null) {
            _uiState.update {
                it.copy(showEditDialog = false, editingCategory = null, editPbText = "", editRunCount = "")
            }
        } else {
            _uiState.update {
                it.copy(
                    showEditDialog = true,
                    editingCategory = category,
                    editPbText = formatTime(category.personalBestMs),
                    editRunCount = category.runCount.toString()
                )
            }
        }
    }

    fun updateEditPbText(text: String) {
        _uiState.update { it.copy(editPbText = text) }
    }

    fun updateEditRunCount(text: String) {
        _uiState.update { it.copy(editRunCount = text) }
    }

    fun saveCategoryEdits() {
        val category = _uiState.value.editingCategory ?: return
        val pbMs = parseTime(_uiState.value.editPbText)
        val runCount = _uiState.value.editRunCount.toIntOrNull() ?: 0

        viewModelScope.launch {
            val updatedCategory = category.copy(
                personalBestMs = pbMs,
                runCount = runCount
            )
            categoryRepository.updateCategory(updatedCategory)
            showEditDialog(null)
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            categoryRepository.deleteCategoryById(categoryId)
        }
    }

    private fun formatTime(ms: Long): String {
        if (ms == 0L) return ""
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = ms % 1000
        return "%d:%02d.%03d".format(minutes, seconds, millis)
    }

    private fun parseTime(timeStr: String): Long {
        if (timeStr.isBlank()) return 0L
        return try {
            val parts = timeStr.split(":", ".")
            when (parts.size) {
                3 -> (parts[0].toLong() * 60000) + (parts[1].toLong() * 1000) + parts[2].toLong()
                2 -> (parts[0].toLong() * 1000) + parts[1].toLong()
                1 -> parts[0].toLong()
                else -> 0L
            }
        } catch (e: Exception) {
            0L
        }
    }
}
