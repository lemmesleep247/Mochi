package io.github.mochi.data.scrape

import io.github.mochi.data.error.MochiError
import io.github.mochi.data.model.MalProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

/**
 * Scrapes myanimelist.net/profile/{username} for manga stats — the one thing
 * the official API never exposes (there's no manga_statistics field at all).
 * Needs a logged-in session cookie (see [MalSessionCookieStore] /
 * MalLoginWebView) rather than just a desktop user-agent — MAL serves a
 * stripped-down page (or bounces to login.php) without one.
 */
@Singleton
class MalProfileScrapeApi @Inject constructor(
    private val client: OkHttpClient,
    private val session: MalSessionCookieStore,
) {

    /** Fetches manga stats for [username] and returns [profile] with those fields filled in. */
    suspend fun applyMangaStats(profile: MalProfile, username: String): MalProfile = withContext(Dispatchers.IO) {
        val doc = fetchProfileDocument(username)
        val mangaBlock = doc.selectFirst("div.stats.manga")
            ?: return@withContext profile // markup changed or block missing; leave profile as-is

        val days = mangaBlock.selectFirst("div.stat-score .di-tc.al")
            ?.text()?.substringAfter("Days:")?.trim()?.toDoubleOrNull() ?: 0.0
        val meanScore = mangaBlock.selectFirst("div.stat-score .score-label")?.text()?.toDoubleOrNull() ?: 0.0

        val statusCounts = mangaBlock.select("ul.stats-status li").associate { li ->
            val label = li.selectFirst("a")?.text().orEmpty()
            val count = li.selectFirst("span.di-ib.fl-r")?.text()?.replace(",", "")?.toIntOrNull() ?: 0
            label to count
        }
        val dataCounts = mangaBlock.select("ul.stats-data li").associate { li ->
            val spans = li.select("span")
            val label = spans.getOrNull(0)?.text().orEmpty()
            val value = spans.getOrNull(1)?.text()?.replace(",", "")?.toIntOrNull() ?: 0
            label to value
        }

        profile.copy(
            mangaDaysRead = days,
            mangaMeanScore = meanScore,
            mangaReading = statusCounts["Reading"] ?: 0,
            mangaCompleted = statusCounts["Completed"] ?: 0,
            mangaOnHold = statusCounts["On-Hold"] ?: 0,
            mangaDropped = statusCounts["Dropped"] ?: 0,
            mangaPlanToRead = statusCounts["Plan to Read"] ?: 0,
            mangaTotalEntries = dataCounts["Total Entries"] ?: 0,
            mangaReread = dataCounts["Reread"] ?: 0,
            mangaChaptersRead = dataCounts["Chapters"] ?: 0,
            mangaVolumesRead = dataCounts["Volumes"] ?: 0,
        )
    }

    private suspend fun fetchProfileDocument(username: String): Document {
        val cookie = session.cookie() ?: throw MochiError.ScrapeSessionExpired()

        val request = Request.Builder()
            .url("https://myanimelist.net/profile/$username")
            .header("Cookie", cookie)
            .header("User-Agent", DESKTOP_USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            val finalUrl = response.request.url.toString()
            val body = response.body.string()
            // An expired/invalid cookie gets redirected to the login form.
            if (finalUrl.contains("login.php") || body.contains("id=\"loginForm\"")) {
                throw MochiError.ScrapeSessionExpired()
            }
            if (!response.isSuccessful) throw IOException("MAL profile scrape failed (${response.code}): $username")
            return Jsoup.parse(body, finalUrl)
        }
    }
}
