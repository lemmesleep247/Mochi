package io.github.mochi.data.error

/** Every user-facing failure in the app, with a message that's already safe to show as-is. */
sealed class MochiError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data object NoConnection : MochiError("No internet connection")
    data object Timeout : MochiError("MyAnimeList is taking too long to respond")
    data object Unauthorized : MochiError("Your session expired — please sign in again")
    data object RateLimited : MochiError("Too many requests — please wait a moment and try again")
    data object NotFound : MochiError("That couldn't be found")
    data object ScrapeSessionExpired : MochiError("Your MyAnimeList website session expired — please sign in again")
    data class ServerError(val code: Int) : MochiError("MyAnimeList returned an error (code $code)")
    data class Unknown(val original: Throwable) :
        MochiError(original.message?.takeIf { it.isNotBlank() } ?: "Something went wrong", original)
}
