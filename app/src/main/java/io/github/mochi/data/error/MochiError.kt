package io.github.mochi.data.error

/**
 * Every user-facing failure in the app, with a message that's already safe to show as-is.
 *
 * These are plain classes, not singleton `object`s — [MochiError] extends [Exception], and an
 * exception declared as an `object` would share one frozen stack trace (captured once, at class
 * init) across every throw site, which is misleading to debug from. Construct a fresh instance
 * each time (`MochiError.NoConnection()`, not `MochiError.NoConnection`).
 */
sealed class MochiError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NoConnection : MochiError("No internet connection")
    class Timeout : MochiError("MyAnimeList is taking too long to respond")
    class Unauthorized : MochiError("Your session expired — please sign in again")
    class RateLimited : MochiError("Too many requests — please wait a moment and try again")
    class NotFound : MochiError("That couldn't be found")
    class ScrapeSessionExpired : MochiError("Your MyAnimeList website session expired — please sign in again")
    class ServerError(val code: Int) : MochiError("MyAnimeList returned an error (code $code)")
    class Unknown(val original: Throwable) :
        MochiError(original.message?.takeIf { it.isNotBlank() } ?: "Something went wrong", original)
}
