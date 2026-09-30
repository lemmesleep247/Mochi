package io.github.mochi.ui

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mochi.ui.navigation.MochiNavHost
import io.github.mochi.ui.screens.onboarding.LibraryLoadingScreen
import io.github.mochi.ui.screens.onboarding.OnboardingScreen

@Composable
fun MochiRoot(viewModel: MainViewModel) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel.authError) {
        viewModel.authError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeAuthError()
        }
    }

    // This wraps every app-level screen just to host a shared SnackbarHost — it
    // owns no chrome of its own, so it must not reserve any system-bar inset
    // space itself. Each screen underneath (or MochiNavHost's own Scaffold)
    // is responsible for its own insets; letting this one also reserve space
    // by default is what stacked into the large blank gap above the content.
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when (appState) {
            AppUiState.SignedOut -> OnboardingScreen(
                modifier = Modifier.padding(padding),
                onSignIn = {
                    viewModel.beginSignIn { url ->
                        CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
                    }
                },
            )
            AppUiState.LoadingLibrary -> LibraryLoadingScreen(modifier = Modifier.padding(padding))
            AppUiState.Ready -> MochiNavHost(onSignOut = viewModel::signOut, modifier = Modifier.padding(padding))
        }
    }
}
