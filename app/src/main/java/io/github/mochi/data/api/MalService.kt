package io.github.mochi.data.api

import retrofit2.http.DELETE
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/** MyAnimeList API v2 (https://api.myanimelist.net/v2). */
interface MalService {

    @GET("users/@me/{type}list")
    suspend fun list(
        @Path("type") type: String,
        @Query("fields") fields: String,
        @Query("limit") limit: Int = 1000,
        @Query("nsfw") nsfw: Boolean = true,
    ): MalListPageDto

    @GET
    suspend fun listByUrl(@Url url: String): MalListPageDto

    @GET("{type}/{id}")
    suspend fun detail(
        @Path("type") type: String,
        @Path("id") id: Int,
        @Query("fields") fields: String,
    ): MalNodeDto

    @FormUrlEncoded
    @PATCH("{type}/{id}/my_list_status")
    suspend fun updateListStatus(
        @Path("type") type: String,
        @Path("id") id: Int,
        @FieldMap fields: Map<String, String>,
    ): MalListStatusDto

    @DELETE("{type}/{id}/my_list_status")
    suspend fun deleteListStatus(
        @Path("type") type: String,
        @Path("id") id: Int,
    )

    @GET("users/@me")
    suspend fun profile(
        @Query("fields") fields: String = "name,picture,gender,birthday,location,joined_at,anime_statistics",
    ): MalUserDto
}
