package io.github.mochi.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mochi.data.model.MalProfile
import io.github.mochi.data.repository.AuthRepository
import io.github.mochi.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: MalProfile? = null,
    val hasScrapeSession: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.hasScrapeSession.collect { has -> _uiState.update { it.copy(hasScrapeSession = has) } }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { profileRepository.profile() }
                .onSuccess { profile -> _uiState.update { it.copy(loading = false, profile = profile) } }
                .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.message ?: "Couldn't load profile") } }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
