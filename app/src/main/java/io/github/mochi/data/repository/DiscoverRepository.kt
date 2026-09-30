package io.github.mochi.data.repository

import io.github.mochi.data.api.TenraiService
import io.github.mochi.data.error.MochiError
import io.github.mochi.data.error.safeCall
import io.github.mochi.data.model.Genre
import io.github.mochi.data.model.MediaItem
import io.github.mochi.data.model.MediaType
import io.github.mochi.data.model.toMediaItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

data class DiscoverPage(val items: List<MediaItem>, val hasMore: Boolean)

@Singleton
class DiscoverRepository @Inject constructor(
    private val service: TenraiService,
) {
    // Tenrai mirrors Jikan's public rate limit. A modest concurrency cap plus a
    // short backoff-and-retry on 429 means a burst (genre list + search firing
    // together on tab switch) degrades gracefully instead of surfacing errors.
    private val gate = Semaphore(3)

    suspend fun search(type: MediaType, query: String, genreIds: List<Int>, page: Int): DiscoverPage = throttled {
        val response = service.search(
            type = type.path,
            genres = genreIds.takeIf { it.isNotEmpty() }?.joinToString(","),
            query = query.trim().ifBlank { null },
            page = page,
        )
        DiscoverPage(
            items = response.data.map { it.toMediaItem(type) },
            hasMore = response.pagination.hasNextPage,
        )
    }

    /** Genres and explicit genres (e.g. Hentai) merged into one pickable list, MAL's own grouping on-site. */
    suspend fun genres(type: MediaType): List<Genre> = throttled {
        coroutineScope {
            val genres = async { service.genres(type.path, "genres") }
            val explicit = async { service.genres(type.path, "explicit_genres") }
            (genres.await().data + explicit.await().data)
                .distinctBy { it.malId }
                .sortedBy { it.name }
                .map { Genre(it.malId, it.name) }
        }
    }

    private suspend fun <T> throttled(block: suspend () -> T): T = gate.withPermit {
        retryOnRateLimit(block, attempt = 0)
    }

    private suspend fun <T> retryOnRateLimit(block: suspend () -> T, attempt: Int): T = try {
        safeCall { block() }
    } catch (e: MochiError.RateLimited) {
        if (attempt >= 3) throw e
        delay((300L * (1L shl attempt)).milliseconds)
        retryOnRateLimit(block, attempt + 1)
    }
}
