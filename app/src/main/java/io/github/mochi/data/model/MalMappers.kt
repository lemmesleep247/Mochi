package io.github.mochi.data.model

import io.github.mochi.data.api.MalListEntryDto
import io.github.mochi.data.api.MalListStatusDto
import io.github.mochi.data.api.MalNodeDto

fun MalListEntryDto.toMediaItem(type: MediaType): MediaItem = node.toMediaItem(type, listStatus)

fun MalNodeDto.toMediaItem(type: MediaType, statusOverride: MalListStatusDto? = null): MediaItem {
    val listStatus = statusOverride ?: myListStatus
    val creators = if (type == MediaType.Anime) {
        studios.joinToString(", ") { it.name }
    } else {
        authors.joinToString(", ") { listOfNotNull(it.node.firstName, it.node.lastName).joinToString(" ") }
    }
    return MediaItem(
        id = id,
        type = type,
        title = title,
        titleEnglish = alternativeTitles?.en.orEmpty(),
        cover = mainPicture?.large ?: mainPicture?.medium ?: "",
        synopsis = synopsis.orEmpty(),
        score = mean ?: 0.0,
        rank = rank ?: 0,
        popularity = popularity ?: 0,
        members = numListUsers ?: 0,
        genres = genres.map { it.name },
        creators = creators,
        format = mediaType.orEmpty().prettify(),
        airStatus = status.orEmpty().prettify(),
        totalUnits = if (type == MediaType.Anime) numEpisodes ?: 0 else numChapters ?: 0,
        startYear = startDate?.take(4).orEmpty(),
        listStatus = ListStatus.from(listStatus?.status),
        progress = if (type == MediaType.Anime) listStatus?.numEpisodesWatched ?: 0 else listStatus?.numChaptersRead ?: 0,
        myScore = listStatus?.score ?: 0,
        isRewatching = if (type == MediaType.Anime) listStatus?.isRewatching ?: false else listStatus?.isRereading ?: false,
        inList = listStatus != null,
    )
}
