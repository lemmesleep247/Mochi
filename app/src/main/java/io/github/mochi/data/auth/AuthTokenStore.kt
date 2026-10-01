package io.github.mochi.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore("mal_auth")

@Singleton
class AuthTokenStore @Inject constructor(@param:ApplicationContext private val context: Context) {

    private object Keys {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val VERIFIER = stringPreferencesKey("pkce_verifier")
        val STATE = stringPreferencesKey("oauth_state")
    }

    val isSignedIn: Flow<Boolean> = context.authDataStore.data.map { !it[Keys.ACCESS].isNullOrBlank() }

    suspend fun accessToken(): String? = context.authDataStore.data.first()[Keys.ACCESS]

    suspend fun refreshToken(): String? = context.authDataStore.data.first()[Keys.REFRESH]

    suspend fun pendingVerifier(): String? = context.authDataStore.data.first()[Keys.VERIFIER]

    suspend fun pendingState(): String? = context.authDataStore.data.first()[Keys.STATE]

    suspend fun savePkce(verifier: String, state: String) {
        context.authDataStore.edit {
            it[Keys.VERIFIER] = verifier
            it[Keys.STATE] = state
        }
    }

    suspend fun saveTokens(access: String, refresh: String) {
        context.authDataStore.edit {
            it[Keys.ACCESS] = access
            it[Keys.REFRESH] = refresh
            it.remove(Keys.VERIFIER)
            it.remove(Keys.STATE)
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }
}
