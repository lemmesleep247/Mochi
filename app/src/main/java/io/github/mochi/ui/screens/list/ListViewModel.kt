package io.github.mochi.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mochi.data.model.ListStatus
import io.github.mochi.data.model.MediaItem
import io.github.mochi.data.model.MediaType
import io.github.mochi.data.repository.MalRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListUiState(
    val loading: Boolean = true,
    val selectedType: MediaType = MediaType.Anime,
    val statusFilter: ListStatus? = null,
    val items: List<MediaItem> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ListViewModel @Inject constructor(
    private val repository: MalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    private var animeItems: List<MediaItem> = emptyList()
    private var mangaItems: List<MediaItem> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                coroutineScope {
                    val anime = async { repository.userList(MediaType.Anime) }
                    val manga = async { repository.userList(MediaType.Manga) }
                    anime.await() to manga.await()
                }
            }.onSuccess { (anime, manga) ->
                animeItems = anime
                mangaItems = manga
                applyFilter()
            }.onFailure { e ->
                _uiState.update { it.copy(loading = false, error = e.message ?: "Couldn't load your list") }
            }
        }
    }

    fun selectTab(type: MediaType) {
        _uiState.update { it.copy(selectedType = type, statusFilter = null) }
        applyFilter()
    }

    fun selectStatusFilter(status: ListStatus?) {
        _uiState.update { it.copy(statusFilter = status) }
        applyFilter()
    }

    fun quickIncrement(item: MediaItem) {
        val newProgress = if (item.totalUnits > 0) (item.progress + 1).coerceAtMost(item.totalUnits) else item.progress + 1
        if (newProgress == item.progress) return
        updateLocal(item.copy(progress = newProgress))
        viewModelScope.launch {
            runCatching { repository.updateEntry(item.type, item.id, item.listStatus, newProgress, item.myScore) }
                .onFailure {
                    updateLocal(item)
                    _uiState.update { s -> s.copy(error = "Couldn't update progress") }
                }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    private fun updateLocal(updated: MediaItem) {
        if (updated.type == MediaType.Anime) {
            animeItems = animeItems.map { if (it.id == updated.id) updated else it }
        } else {
            mangaItems = mangaItems.map { if (it.id == updated.id) updated else it }
        }
        applyFilter()
    }

    private fun applyFilter() {
        val state = _uiState.value
        val source = if (state.selectedType == MediaType.Anime) animeItems else mangaItems
        val filtered = state.statusFilter?.let { filter -> source.filter { it.listStatus == filter } } ?: source
        _uiState.update { it.copy(loading = false, items = filtered.sortedByDescending { m -> m.score }) }
    }
}
