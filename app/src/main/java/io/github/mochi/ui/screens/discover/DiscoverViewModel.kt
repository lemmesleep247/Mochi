package io.github.mochi.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mochi.data.model.Genre
import io.github.mochi.data.model.MediaItem
import io.github.mochi.data.model.MediaType
import io.github.mochi.data.repository.DiscoverRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class DiscoverUiState(
    val type: MediaType = MediaType.Anime,
    val query: String = "",
    val availableGenres: List<Genre> = emptyList(),
    val selectedGenreIds: Set<Int> = emptySet(),
    val items: List<MediaItem> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: DiscoverRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadGenres()
        search()
    }

    fun setType(type: MediaType) {
        if (type == _uiState.value.type) return
        _uiState.update { it.copy(type = type, selectedGenreIds = emptySet(), availableGenres = emptyList()) }
        loadGenres()
        search()
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        search(debounce = true)
    }

    fun toggleGenre(id: Int) {
        _uiState.update {
            val selection = if (id in it.selectedGenreIds) it.selectedGenreIds - id else it.selectedGenreIds + id
            it.copy(selectedGenreIds = selection)
        }
        search()
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasMore || state.loadingMore || state.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            runCatching { repository.search(state.type, state.query, state.selectedGenreIds.toList(), state.page + 1) }
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(loadingMore = false, items = it.items + page.items, hasMore = page.hasMore, page = it.page + 1)
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(loadingMore = false, error = e.message ?: "Couldn't load more") } }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    private fun loadGenres() {
        val type = _uiState.value.type
        viewModelScope.launch {
            runCatching { repository.genres(type) }
                .onSuccess { genres -> _uiState.update { it.copy(availableGenres = genres) } }
        }
    }

    private fun search(debounce: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(400.milliseconds)
            val state = _uiState.value
            _uiState.update { it.copy(loading = true, error = null, page = 1) }
            runCatching { repository.search(state.type, state.query, state.selectedGenreIds.toList(), 1) }
                .onSuccess { page -> _uiState.update { it.copy(loading = false, items = page.items, hasMore = page.hasMore, page = 1) } }
                .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.message ?: "Couldn't load results") } }
        }
    }
}
