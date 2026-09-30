package io.github.mochi.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mochi.data.model.ListStatus
import io.github.mochi.data.model.MediaItem
import io.github.mochi.data.model.MediaType
import io.github.mochi.data.repository.MalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val item: MediaItem? = null,
    val error: String? = null,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: MalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var loadedFor: Pair<Int, MediaType>? = null

    fun load(id: Int, type: MediaType) {
        if (loadedFor == (id to type)) return
        loadedFor = id to type
        viewModelScope.launch {
            _uiState.value = DetailUiState(loading = true)
            runCatching { repository.detail(type, id) }
                .onSuccess { _uiState.value = DetailUiState(loading = false, item = it) }
                .onFailure { _uiState.value = DetailUiState(loading = false, error = it.message ?: "Couldn't load details") }
        }
    }

    fun updateStatus(status: ListStatus) = mutate { it.copy(listStatus = status) }

    fun updateProgress(progress: Int) = mutate { item ->
        val clamped = if (item.totalUnits > 0) progress.coerceIn(0, item.totalUnits) else progress.coerceAtLeast(0)
        item.copy(progress = clamped)
    }

    fun updateScore(score: Int) = mutate { it.copy(myScore = score.coerceIn(0, 10)) }

    fun save() {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            runCatching { repository.updateEntry(item.type, item.id, item.listStatus, item.progress, item.myScore) }
                .onSuccess { _uiState.update { s -> s.copy(saving = false, item = item.copy(inList = true)) } }
                .onFailure { _uiState.update { s -> s.copy(saving = false, error = "Couldn't save changes") } }
        }
    }

    fun remove() {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            runCatching { repository.deleteEntry(item.type, item.id) }
                .onSuccess {
                    _uiState.update { s ->
                        s.copy(item = item.copy(inList = false, progress = 0, myScore = 0, listStatus = ListStatus.planFor(item.type)))
                    }
                }
                .onFailure { _uiState.update { s -> s.copy(error = "Couldn't remove entry") } }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    private fun mutate(block: (MediaItem) -> MediaItem) {
        _uiState.update { state -> state.item?.let { state.copy(item = block(it)) } ?: state }
    }
}
