package io.github.mochi.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MalListPageDto(
    val data: List<MalListEntryDto> = emptyList(),
    val paging: MalPagingDto = MalPagingDto(),
)

@Serializable
data class MalPagingDto(
    val next: String? = null,
)

@Serializable
data class MalListEntryDto(
    val node: MalNodeDto,
    @SerialName("list_status") val listStatus: MalListStatusDto? = null,
)

@Serializable
data class MalNodeDto(
    val id: Int,
    val title: String,
    @SerialName("main_picture") val mainPicture: MalPictureDto? = null,
    @SerialName("alternative_titles") val alternativeTitles: MalAltTitlesDto? = null,
    val synopsis: String? = null,
    val mean: Double? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    @SerialName("num_list_users") val numListUsers: Int? = null,
    val genres: List<MalGenreDto> = emptyList(),
    @SerialName("media_type") val mediaType: String? = null,
    val status: String? = null,
    @SerialName("num_episodes") val numEpisodes: Int? = null,
    @SerialName("num_chapters") val numChapters: Int? = null,
    @SerialName("num_volumes") val numVolumes: Int? = null,
    @SerialName("start_date") val startDate: String? = null,
    val studios: List<MalGenreDto> = emptyList(),
    val authors: List<MalAuthorEntryDto> = emptyList(),
    @SerialName("my_list_status") val myListStatus: MalListStatusDto? = null,
)

@Serializable
data class MalPictureDto(
    val medium: String? = null,
    val large: String? = null,
)

@Serializable
data class MalAltTitlesDto(
    val synonyms: List<String> = emptyList(),
    val en: String? = null,
    val ja: String? = null,
)

@Serializable
data class MalGenreDto(
    val id: Int = 0,
    val name: String,
)

@Serializable
data class MalAuthorEntryDto(
    val node: MalPersonDto,
    val role: String? = null,
)

@Serializable
data class MalPersonDto(
    val id: Int,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
)

@Serializable
data class MalListStatusDto(
    val status: String? = null,
    val score: Int = 0,
    @SerialName("num_episodes_watched") val numEpisodesWatched: Int = 0,
    @SerialName("num_chapters_read") val numChaptersRead: Int = 0,
    @SerialName("num_volumes_read") val numVolumesRead: Int = 0,
    @SerialName("is_rewatching") val isRewatching: Boolean = false,
    @SerialName("is_rereading") val isRereading: Boolean = false,
)

@Serializable
data class MalTokenResponseDto(
    @SerialName("token_type") val tokenType: String = "",
    @SerialName("expires_in") val expiresIn: Long = 0,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)
