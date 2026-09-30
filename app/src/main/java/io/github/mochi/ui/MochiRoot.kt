package io.github.mochi.ui

import androidx.browser.customtabs.CustomTabsIntent
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
import io.github.mochi.ui.screens.signin.SignInScreen

@Composable
fun MochiRoot(viewModel: MainViewModel) {
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel.authError) {
        viewModel.authError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeAuthError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        if (isSignedIn) {
            MochiNavHost(onSignOut = viewModel::signOut, modifier = Modifier.padding(padding))
        } else {
            SignInScreen(
                modifier = Modifier.padding(padding),
                onSignIn = {
                    viewModel.beginSignIn { url ->
                        CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
                    }
                },
            )
        }
    }
}
