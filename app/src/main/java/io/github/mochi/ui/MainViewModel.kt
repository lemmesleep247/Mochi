package io.github.mochi.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mochi.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean> = authRepository.isSignedIn
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    var authError by mutableStateOf<String?>(null)
        private set

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
        viewModelScope.launch { authRepository.signOut() }
    }
}
