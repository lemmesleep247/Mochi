package io.github.mochi.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Tenrai (Jikan-compatible) API (https://api.tenrai.org/v1) — used for the discovery/search
 * surface MAL's own API doesn't support (genre-filtered browsing), not for list management.
 */
interface TenraiService {

    @GET("{type}")
    suspend fun search(
        @Path("type") type: String,
        @Query("genres") genres: String? = null,
        @Query("q") query: String? = null,
        @Query("order_by") orderBy: String = "members",
        @Query("sort") sort: String = "desc",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 24,
        @Query("sfw") sfw: Boolean = true,
    ): TenraiPageDto<TenraiNodeDto>

    @GET("genres/{type}")
    suspend fun genres(
        @Path("type") type: String,
        @Query("filter") filter: String,
    ): TenraiPageDto<TenraiNameDto>
}
