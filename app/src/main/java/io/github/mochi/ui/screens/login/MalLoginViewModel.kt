package io.github.mochi.ui.screens.login

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mochi.data.scrape.MalSessionCookieStore
import javax.inject.Inject

/** Thin Hilt bridge — hands the injected [MalSessionCookieStore] singleton into Compose. */
@HiltViewModel
class MalLoginViewModel @Inject constructor(
    val sessionStore: MalSessionCookieStore,
) : ViewModel()
