package io.github.mochi.data.scrape

import android.content.Context
import android.webkit.CookieManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.malCookieDataStore by preferencesDataStore("mal_cookie")
private const val MAL_DOMAIN = "https://myanimelist.net"

/**
 * Stores the raw MAL website session cookie ("name1=value1; name2=value2"),
 * kept separate from [io.github.mochi.data.auth.AuthTokenStore]'s OAuth tokens:
 * the OAuth token authenticates against the official API, this cookie
 * authenticates a plain myanimelist.net page load — the only way to reach data
 * the API doesn't expose at all (manga stats).
 */
@Singleton
class MalSessionCookieStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val COOKIE = stringPreferencesKey("mal_cookie")
    }

    val hasSession: Flow<Boolean> = context.malCookieDataStore.data.map { !it[Keys.COOKIE].isNullOrBlank() }

    suspend fun cookie(): String? = context.malCookieDataStore.data.first()[Keys.COOKIE]

    /** Pulls whatever cookies the system WebView currently holds for MAL and persists them. */
    suspend fun captureFromWebView() {
        val cookie = CookieManager.getInstance().getCookie(MAL_DOMAIN)
        if (!cookie.isNullOrBlank()) {
            context.malCookieDataStore.edit { it[Keys.COOKIE] = cookie }
        }
    }

    suspend fun clear() {
        context.malCookieDataStore.edit { it.clear() }
        CookieManager.getInstance().removeAllCookies(null)
    }
}
