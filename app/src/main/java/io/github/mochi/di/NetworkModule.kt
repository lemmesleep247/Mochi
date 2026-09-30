package io.github.mochi.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.mochi.BuildConfig
import io.github.mochi.data.api.MalOAuthService
import io.github.mochi.data.api.MalService
import io.github.mochi.data.api.TenraiService
import io.github.mochi.data.auth.AuthTokenStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MalApiRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MalOAuthRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MalApiOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TenraiRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideBaseOkHttpClient(@ApplicationContext context: Context): OkHttpClient =
        OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http_cache"), 10L * 1024 * 1024))
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()

    @Provides
    @Singleton
    @MalOAuthRetrofit
    fun provideOAuthRetrofit(json: Json, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://myanimelist.net/v1/oauth2/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideMalOAuthService(@MalOAuthRetrofit retrofit: Retrofit): MalOAuthService = retrofit.create()

    @Provides
    @Singleton
    @MalApiOkHttp
    fun provideMalOkHttpClient(
        baseClient: OkHttpClient,
        tokenStore: AuthTokenStore,
        authenticator: MalAuthenticator,
    ): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { tokenStore.accessToken() }
            val request = chain.request().newBuilder()
                .addHeader("X-MAL-CLIENT-ID", BuildConfig.MAL_CLIENT_ID)
                .apply { if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token") }
                .build()
            chain.proceed(request)
        }
        return baseClient.newBuilder()
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .build()
    }

    @Provides
    @Singleton
    @MalApiRetrofit
    fun provideMalRetrofit(json: Json, @MalApiOkHttp client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.myanimelist.net/v2/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideMalService(@MalApiRetrofit retrofit: Retrofit): MalService = retrofit.create()

    @Provides
    @Singleton
    @TenraiRetrofit
    fun provideTenraiRetrofit(json: Json, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.tenrai.org/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideTenraiService(@TenraiRetrofit retrofit: Retrofit): TenraiService = retrofit.create()
}
