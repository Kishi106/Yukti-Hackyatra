package com.example.network

import com.squareup.moshi.JsonClass
import retrofit2.HttpException

@JsonClass(generateAdapter = true)
internal data class ErrorBody(val error: String? = null)

/**
 * Best-effort extraction of the backend's `{ "error": "..." }` message from an
 * HttpException's response body (e.g. 400 validation errors, 500s), falling back
 * to the exception's own message (network failures, timeouts, etc.) and finally
 * to [fallback]. Without this, HttpException.message is just a generic string
 * like "HTTP 400 Bad Request" — the actual backend-provided reason is lost.
 */
internal fun Exception.toUserMessage(fallback: String): String {
    if (this is HttpException) {
        val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
        if (!body.isNullOrBlank()) {
            val parsed = runCatching {
                NetworkModule.moshiInstance.adapter(ErrorBody::class.java).fromJson(body)
            }.getOrNull()
            val errorText = parsed?.error
            if (!errorText.isNullOrBlank()) return errorText
        }
    }
    return message ?: fallback
}
