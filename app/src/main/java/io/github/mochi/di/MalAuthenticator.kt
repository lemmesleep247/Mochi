package io.github.mochi.di

import io.github.mochi.BuildConfig
import io.github.mochi.data.api.MalOAuthService
import io.github.mochi.data.auth.AuthTokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * On a 401, refreshes the MAL access token (blocking — OkHttp always calls this off the
 * main thread) and retries the request once. Signs the user out if the refresh itself fails,
 * since a rejected refresh token means the session can't be recovered silently.
 */
@Singleton
class MalAuthenticator @Inject constructor(
    private val tokenStore: AuthTokenStore,
    private val oauthService: Provider<MalOAuthService>,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val refresh = runBlocking { tokenStore.refreshToken() } ?: return null
        val newAccessToken = runBlocking {
            runCatching {
                val result = oauthService.get().token(
                    clientId = BuildConfig.MAL_CLIENT_ID,
                    grantType = "refresh_token",
                    refreshToken = refresh,
                )
                tokenStore.saveTokens(result.accessToken, result.refreshToken)
                result.accessToken
            }.getOrNull()
        }

        if (newAccessToken == null) {
            runBlocking { tokenStore.clear() }
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
