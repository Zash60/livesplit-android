package com.livesplit.ui.splits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesplit.data.model.Segment
import com.livesplit.data.repository.CategoryRepository
import com.livesplit.data.repository.SegmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplitsUiState(
    val segments: List<Segment> = emptyList(),
    val categoryName: String = "",
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingSegment: Segment? = null,
    val editNameText: String = "",
    val editPbTimeText: String = "",
    val editBestTimeText: String = "",
    val sumOfBests: Long = 0,
    val lastPb: Long = 0
)

@HiltViewModel
class SplitsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val segmentRepository: SegmentRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val categoryId: Long = savedStateHandle.get<String>("categoryId")?.toLongOrNull() ?: -1L

    private val _uiState = MutableStateFlow(SplitsUiState())
    val uiState: StateFlow<SplitsUiState> = _uiState.asStateFlow()

    init {
        loadCategory()
        loadSegments()
    }

    private fun loadCategory() {
        viewModelScope.launch {
            val category = categoryRepository.getCategoryById(categoryId)
            _uiState.update { it.copy(categoryName = category?.name ?: "Splits") }
        }
    }

    private fun loadSegments() {
        viewModelScope.launch {
            segmentRepository.getSegmentsByCategoryId(categoryId)
                .collect { segments ->
                    val sumOfBests = segmentRepository.getSumOfBests(categoryId)
                    val lastPb = if (segments.isNotEmpty()) segments.last().pbTimeMs else 0L
                    _uiState.update {
                        it.copy(
                            segments = segments,
                            isLoading = false,
                            sumOfBests = sumOfBests,
                            lastPb = lastPb
                        )
                    }
                }
        }
    }

    fun addSegment(name: String, position: Int? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            segmentRepository.addSegment(categoryId, name.trim(), position)
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    fun showAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show) }
    }

    fun showEditDialog(segment: Segment?) {
        if (segment == null) {
            _uiState.update {
                it.copy(
                    showEditDialog = false,
                    editingSegment = null,
                    editNameText = "",
                    editPbTimeText = "",
                    editBestTimeText = ""
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    showEditDialog = true,
                    editingSegment = segment,
                    editNameText = segment.name,
                    editPbTimeText = formatTime(segment.pbTimeMs),
                    editBestTimeText = formatTime(segment.bestTimeMs)
                )
            }
        }
    }

    fun updateEditName(text: String) {
        _uiState.update { it.copy(editNameText = text) }
    }

    fun updateEditPbTime(text: String) {
        _uiState.update { it.copy(editPbTimeText = text) }
    }

    fun updateEditBestTime(text: String) {
        _uiState.update { it.copy(editBestTimeText = text) }
    }

    fun saveSegmentEdits() {
        val segment = _uiState.value.editingSegment ?: return
        val name = _uiState.value.editNameText.trim()
        if (name.isBlank()) return

        val pbTime = parseTime(_uiState.value.editPbTimeText)
        val bestTime = parseTime(_uiState.value.editBestTimeText)

        viewModelScope.launch {
            val updated = segment.copy(
                name = name,
                pbTimeMs = pbTime,
                bestTimeMs = bestTime
            )
            segmentRepository.updateSegment(updated)
            showEditDialog(null)
        }
    }

    fun deleteSegment(segmentId: Long) {
        viewModelScope.launch {
            segmentRepository.deleteSegmentById(segmentId)
        }
    }

    fun reorderSegments(fromIndex: Int, toIndex: Int) {
        val currentSegments = _uiState.value.segments.toMutableList()
        if (fromIndex < 0 || fromIndex >= currentSegments.size) return
        if (toIndex < 0 || toIndex >= currentSegments.size) return

        val moved = currentSegments.removeAt(fromIndex)
        currentSegments.add(toIndex, moved)

        val orderedIds = currentSegments.map { it.id }

        viewModelScope.launch {
            segmentRepository.reorderSegments(categoryId, orderedIds)
        }
    }

    fun recalculatePbTimes() {
        viewModelScope.launch {
            segmentRepository.recalculatePbTimes(categoryId)
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
