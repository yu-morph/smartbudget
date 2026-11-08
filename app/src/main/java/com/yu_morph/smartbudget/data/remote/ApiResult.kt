package com.yu_morph.smartbudget.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(val status: Int, val code: String, val message: String, val data: T? = null)

enum class ResponseSource {
    NETWORK,
    CACHE,
}

sealed interface ApiResult<out T> {
    data class Success<T>(
        val data: T?,
        val status: Int,
        val code: String,
        val message: String,
        val source: ResponseSource,
    ) : ApiResult<T>

    data class HttpFailure(val status: Int, val code: String?, val message: String, val retryAfterSeconds: Long?) :
        ApiResult<Nothing>

    data class NetworkFailure(val cause: Throwable) : ApiResult<Nothing>

    data class DecodeFailure(val rawBody: String, val cause: Throwable) : ApiResult<Nothing>

    data class NotImplemented(val feature: String) : ApiResult<Nothing>
}
