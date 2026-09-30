package io.github.mochi.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mochi.data.model.MediaType
import io.github.mochi.data.repository.AuthRepository
import io.github.mochi.data.repository.MalRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where the app is in the sign-in → first-load → main-app sequence. */
sealed interface AppUiState {
    data object SignedOut : AppUiState
    data object LoadingLibrary : AppUiState
    data object Ready : AppUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val malRepository: MalRepository,
) : ViewModel() {

    private val _appState = MutableStateFlow<AppUiState>(AppUiState.SignedOut)
    val appState: StateFlow<AppUiState> = _appState.asStateFlow()

    var authError by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            authRepository.isSignedIn.collect { signedIn ->
                if (signedIn) {
                    // Only kick off the first-load sequence on the SignedOut -> signed-in
                    // transition, not on every incidental re-emission of the same value
                    // (e.g. the PKCE verifier being written mid sign-in also touches this
                    // DataStore file) — otherwise a stray emission would restart the
                    // library fetch while it's already running or already done.
                    if (_appState.value is AppUiState.SignedOut) warmUpLibrary()
                } else {
                    _appState.value = AppUiState.SignedOut
                }
            }
        }
    }

    private fun warmUpLibrary() {
        viewModelScope.launch {
            _appState.value = AppUiState.LoadingLibrary
            // Best-effort — if this fails (offline, MAL hiccup), don't trap the user on a
            // loading screen. ListScreen does its own fetch and will surface a real error.
            runCatching {
                coroutineScope {
                    val anime = async { malRepository.userList(MediaType.Anime) }
                    val manga = async { malRepository.userList(MediaType.Manga) }
                    anime.await()
                    manga.await()
                }
            }
            _appState.value = AppUiState.Ready
        }
    }

    fun beginSignIn(onUrlReady: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.authUrl() }
                .onSuccess(onUrlReady)
                .onFailure { authError = it.message ?: "Couldn't start sign-in" }
        }
    }

    fun handleOAuthRedirect(uri: Uri) {
        viewModelScope.launch {
            authRepository.handleRedirect(uri).onFailure { authError = it.message ?: "Sign-in failed" }
        }
    }

    fun consumeAuthError() {
        authError = null
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _appState.value = AppUiState.SignedOut
        }
    }
}
