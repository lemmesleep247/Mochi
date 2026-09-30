package io.github.mochi.data.model

import io.github.mochi.data.api.TenraiNodeDto

fun TenraiNodeDto.toMediaItem(mediaType: MediaType): MediaItem {
    val cover = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl
        ?: images?.webp?.largeImageUrl ?: images?.webp?.imageUrl ?: ""
    val creators = if (mediaType == MediaType.Anime) {
        studios.joinToString(", ") { it.name }
    } else {
        authors.joinToString(", ") { it.name }
    }
    val startDate = (aired ?: published)?.from

    // Tenrai results aren't tied to the signed-in user's list — they're always
    // "not yet added", same as any other browse/search surface. DetailScreen's
    // Save button already upserts on MAL regardless of prior list membership.
    return MediaItem(
        id = malId,
        type = mediaType,
        title = title,
        titleEnglish = titleEnglish.orEmpty(),
        cover = cover,
        synopsis = synopsis.orEmpty(),
        score = score ?: 0.0,
        rank = rank ?: 0,
        popularity = popularity ?: 0,
        members = members ?: 0,
        genres = (genres + explicitGenres).map { it.name },
        creators = creators,
        format = type.orEmpty(),
        airStatus = status.orEmpty(),
        totalUnits = if (mediaType == MediaType.Anime) episodes ?: 0 else chapters ?: 0,
        startYear = startDate?.take(4).orEmpty(),
        listStatus = ListStatus.planFor(mediaType),
        progress = 0,
        myScore = 0,
        isRewatching = false,
        inList = false,
    )
}
