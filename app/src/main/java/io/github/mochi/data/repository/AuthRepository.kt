package io.github.mochi.data.repository

import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import io.github.mochi.BuildConfig
import io.github.mochi.data.api.MalOAuthService
import io.github.mochi.data.auth.AuthTokenStore
import io.github.mochi.data.error.safeCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val oauthService: MalOAuthService,
    private val tokenStore: AuthTokenStore,
) {
    val isSignedIn: Flow<Boolean> = tokenStore.isSignedIn

    suspend fun authUrl(): String = withContext(Dispatchers.IO) {
        val verifier = randomToken(64)
        val state = randomToken(24)
        tokenStore.savePkce(verifier, state)

        "https://myanimelist.net/v1/oauth2/authorize".toUri().buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", BuildConfig.MAL_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.OAUTH_REDIRECT)
            .appendQueryParameter("code_challenge", verifier)
            .appendQueryParameter("code_challenge_method", "plain")
            .appendQueryParameter("state", state)
            .build()
            .toString()
    }

    suspend fun handleRedirect(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val expectedState = tokenStore.pendingState()
            val returnedState = uri.getQueryParameter("state")
            require(!expectedState.isNullOrBlank() && returnedState == expectedState) {
                "Sign-in session expired, please try again"
            }
            val code = uri.getQueryParameter("code")
                ?: error(uri.getQueryParameter("error_description") ?: uri.getQueryParameter("error") ?: "Sign-in was cancelled")
            val verifier = tokenStore.pendingVerifier() ?: error("Sign-in session expired, please try again")

            val response = safeCall {
                oauthService.token(
                    clientId = BuildConfig.MAL_CLIENT_ID,
                    grantType = "authorization_code",
                    code = code,
                    redirectUri = BuildConfig.OAUTH_REDIRECT,
                    codeVerifier = verifier,
                )
            }
            tokenStore.saveTokens(response.accessToken, response.refreshToken)
        }
    }

    suspend fun signOut() = tokenStore.clear()

    private fun randomToken(bytes: Int): String =
        ByteArray(bytes).also { SecureRandom().nextBytes(it) }
            .let { Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) }
}
