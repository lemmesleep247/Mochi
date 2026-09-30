package io.github.mochi.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TenraiPageDto<T>(
    val data: List<T> = emptyList(),
    val pagination: TenraiPaginationDto = TenraiPaginationDto(),
)

@Serializable
data class TenraiPaginationDto(
    @SerialName("has_next_page") val hasNextPage: Boolean = false,
)

@Serializable
data class TenraiNodeDto(
    @SerialName("mal_id") val malId: Int,
    val title: String,
    @SerialName("title_english") val titleEnglish: String? = null,
    val type: String? = null,
    val episodes: Int? = null,
    val chapters: Int? = null,
    val status: String? = null,
    val score: Double? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val members: Int? = null,
    val synopsis: String? = null,
    val genres: List<TenraiNameDto> = emptyList(),
    @SerialName("explicit_genres") val explicitGenres: List<TenraiNameDto> = emptyList(),
    val studios: List<TenraiNameDto> = emptyList(),
    val authors: List<TenraiNameDto> = emptyList(),
    val images: TenraiImagesDto? = null,
    val aired: TenraiDateRangeDto? = null,
    val published: TenraiDateRangeDto? = null,
)

@Serializable
data class TenraiNameDto(
    @SerialName("mal_id") val malId: Int = 0,
    val name: String,
)

@Serializable
data class TenraiImagesDto(
    val jpg: TenraiImageUrlsDto? = null,
    val webp: TenraiImageUrlsDto? = null,
)

@Serializable
data class TenraiImageUrlsDto(
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("large_image_url") val largeImageUrl: String? = null,
)

@Serializable
data class TenraiDateRangeDto(
    val from: String? = null,
)
