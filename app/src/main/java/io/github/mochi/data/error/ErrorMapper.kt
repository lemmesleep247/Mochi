package io.github.mochi.data.error

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.toMochiError(): MochiError = when (this) {
    is MochiError -> this
    is UnknownHostException -> MochiError.NoConnection
    is SocketTimeoutException -> MochiError.Timeout
    is HttpException -> when (code()) {
        401 -> MochiError.Unauthorized
        404 -> MochiError.NotFound
        429 -> MochiError.RateLimited
        else -> MochiError.ServerError(code())
    }
    is IOException -> MochiError.NoConnection
    else -> MochiError.Unknown(this)
}
