package io.github.mochi.data.api

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/** MyAnimeList OAuth2 token endpoint (https://myanimelist.net/v1/oauth2/token). */
interface MalOAuthService {

    @FormUrlEncoded
    @POST("token")
    suspend fun token(
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String,
        @Field("code") code: String? = null,
        @Field("redirect_uri") redirectUri: String? = null,
        @Field("code_verifier") codeVerifier: String? = null,
        @Field("refresh_token") refreshToken: String? = null,
    ): MalTokenResponseDto
}
