package io.github.mochi.ui.components

import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.mochi.data.scrape.MalSessionCookieStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val LOGIN_URL = "https://myanimelist.net/login.php"
// Reachable by any authenticated account and bounces to login.php without a
// session, so landing here confirms the login actually took.
private const val LOGIN_CHECK_URL = "https://myanimelist.net/editprofile.php"
private const val MAL_HOST = "myanimelist.net"

/**
 * Embedded WebView for signing into the plain myanimelist.net website — distinct
 * from the OAuth sign-in (which never exposes a session cookie). Captures the
 * session cookie once a post-login page confirms the user is authenticated,
 * then hands control back via [onLoginSuccess].
 */
@Composable
fun MalLoginWebView(
    sessionStore: MalSessionCookieStore,
    onLoginSuccess: () -> Unit,
    onVerifyingChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true // MAL's login form needs this
                webViewClient = MalLoginWebViewClient(sessionStore, scope, onLoginSuccess, onVerifyingChange)
                loadUrl(LOGIN_URL)
            }
        },
    )
}

private class MalLoginWebViewClient(
    private val sessionStore: MalSessionCookieStore,
    private val scope: CoroutineScope,
    private val onLoginSuccess: () -> Unit,
    private val onVerifyingChange: (Boolean) -> Unit,
) : WebViewClient() {

    // Setter reports every transition so the host screen can show a spinner for
    // the (real, unavoidable — it's an extra page load) gap between "looks
    // logged in" and onLoginSuccess actually firing.
    private var verifying = false
        set(value) {
            field = value
            onVerifyingChange(value)
        }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        if (url == null) return

        // MAL's login page also offers Google/Facebook/Apple/X sign-in, which
        // navigates this WebView to those providers' own domains as a
        // legitimate mid-flow step — not a failure, just not done yet.
        val host = Uri.parse(url).host.orEmpty()
        val onMal = host == MAL_HOST || host.endsWith(".$MAL_HOST")

        when {
            url.startsWith(LOGIN_CHECK_URL) -> {
                scope.launch {
                    sessionStore.captureFromWebView()
                    if (sessionStore.cookie() != null) onLoginSuccess() else verifying = false
                }
            }
            !onMal -> Unit // mid-flow on a third-party auth provider; wait for it to hand back control
            url.contains("login.php") -> verifying = false // bounced back to the login form; not signed in yet
            verifying -> verifying = false // landed somewhere unexpected mid-verification; let the next load retry
            else -> {
                // Landed somewhere other than the login form — probably authenticated.
                // Confirm with a page only a logged-in session can reach.
                verifying = true
                view.loadUrl(LOGIN_CHECK_URL)
            }
        }
    }
}
