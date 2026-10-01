package io.github.mochi.data.repository

import io.github.mochi.data.api.MalService
import io.github.mochi.data.error.safeCall
import io.github.mochi.data.model.ListStatus
import io.github.mochi.data.model.MalProfile
import io.github.mochi.data.model.MediaItem
import io.github.mochi.data.model.MediaType
import io.github.mochi.data.model.prettify
import io.github.mochi.data.model.toMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MalRepository @Inject constructor(
    private val service: MalService,
) {
    suspend fun userList(type: MediaType): List<MediaItem> = safeCall {
        withContext(Dispatchers.IO) {
            val items = mutableListOf<MediaItem>()
            var nextUrl: String? = null
            do {
                val page = if (nextUrl == null) {
                    service.list(type.path, fields = listFields(type))
                } else {
                    service.listByUrl(nextUrl)
                }
                items += page.data.map { it.toMediaItem(type) }
                nextUrl = page.paging.next
            } while (nextUrl != null)
            items
        }
    }

    suspend fun detail(type: MediaType, id: Int): MediaItem = safeCall {
        withContext(Dispatchers.IO) {
            service.detail(type.path, id, fields = detailFields(type)).toMediaItem(type)
        }
    }

    suspend fun updateEntry(
        type: MediaType,
        id: Int,
        status: ListStatus,
        progress: Int,
        score: Int,
    ): Unit = safeCall {
        withContext(Dispatchers.IO) {
            val progressKey = if (type == MediaType.Anime) "num_watched_episodes" else "num_chapters_read"
            val fields = mapOf(
                "status" to status.apiValue,
                progressKey to progress.toString(),
                "score" to score.toString(),
            )
            service.updateListStatus(type.path, id, fields)
        }
    }

    suspend fun deleteEntry(type: MediaType, id: Int): Unit = safeCall {
        withContext(Dispatchers.IO) { service.deleteListStatus(type.path, id) }
    }

    suspend fun profile(): MalProfile = safeCall {
        withContext(Dispatchers.IO) {
            val dto = service.profile()
            val stats = dto.animeStatistics
            MalProfile(
                name = dto.name,
                picture = dto.picture.orEmpty(),
                gender = dto.gender.orEmpty().prettify(),
                location = dto.location.orEmpty(),
                birthday = dto.birthday.orEmpty(),
                joinedAt = dto.joinedAt.orEmpty(),
                animeDaysWatched = stats?.numDaysWatched ?: 0.0,
                animeMeanScore = stats?.meanScore ?: 0.0,
                animeEpisodesWatched = stats?.numEpisodes ?: 0,
                animeTotalEntries = stats?.numItems ?: 0,
                animeWatching = stats?.numItemsWatching ?: 0,
                animeCompleted = stats?.numItemsCompleted ?: 0,
                animeOnHold = stats?.numItemsOnHold ?: 0,
                animeDropped = stats?.numItemsDropped ?: 0,
                animePlanToWatch = stats?.numItemsPlanToWatch ?: 0,
            )
        }
    }

    private fun listFields(type: MediaType) = buildString {
        append("list_status{status,score,")
        append(if (type == MediaType.Anime) "num_episodes_watched,is_rewatching" else "num_chapters_read,num_volumes_read,is_rereading")
        append("},main_picture,alternative_titles,mean,media_type,status,genres,")
        append(if (type == MediaType.Anime) "num_episodes" else "num_chapters,num_volumes")
    }

    private fun detailFields(type: MediaType) = listFields(type).replace("list_status", "my_list_status") +
        ",synopsis,rank,popularity,num_list_users," +
        if (type == MediaType.Anime) "studios" else "authors{first_name,last_name}"
}
