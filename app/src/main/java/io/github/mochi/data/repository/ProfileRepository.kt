package io.github.mochi.data.repository

import io.github.mochi.data.model.MalProfile
import io.github.mochi.data.scrape.MalProfileScrapeApi
import io.github.mochi.data.scrape.MalSessionCookieStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val malRepository: MalRepository,
    private val scrapeApi: MalProfileScrapeApi,
    private val sessionCookieStore: MalSessionCookieStore,
) {
    val hasScrapeSession: Flow<Boolean> = sessionCookieStore.hasSession

    /**
     * Fetches the signed-in user's profile from the official API, then layers
     * in manga stats via a website scrape if (and only if) a MAL website
     * session cookie is available. A scrape failure — expired cookie, markup
     * change — silently falls back to the API-only profile rather than
     * breaking the whole screen over data that was always best-effort.
     */
    suspend fun profile(): MalProfile {
        val base = malRepository.profile()
        if (base.name.isBlank() || sessionCookieStore.cookie() == null) return base
        return runCatching { scrapeApi.applyMangaStats(base, base.name).copy(hasMangaStats = true) }
            .getOrDefault(base)
    }
}
