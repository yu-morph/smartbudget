package com.yu_morph.smartbudget.data.remote

import com.yu_morph.smartbudget.data.local.ApiResponseCache
import com.yu_morph.smartbudget.data.local.AuthTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

object SmartBudgetHttpClientFactory {
    private const val REQUEST_TIMEOUT_MILLIS = 30_000L
    private const val CONNECT_TIMEOUT_MILLIS = 15_000L
    private const val SOCKET_TIMEOUT_MILLIS = 30_000L

    fun create(json: Json = defaultJson()): HttpClient = HttpClient(Android) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
        }
    }

    fun defaultJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
        encodeDefaults = false
    }
}

class ApiRequestExecutor(
    baseUrl: String,
    private val client: HttpClient,
    private val tokenStore: AuthTokenStore,
    private val responseCache: ApiResponseCache,
    val json: Json = SmartBudgetHttpClientFactory.defaultJson(),
) {
    private companion object {
        const val HTTP_SUCCESS_MIN = 200
        const val HTTP_SUCCESS_MAX = 299
    }

    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    suspend inline fun <reified T> execute(
        method: HttpMethod,
        path: String,
        authenticated: Boolean = false,
        body: JsonElement? = null,
        cacheKey: String? = null,
        noinline configure: HttpRequestBuilder.() -> Unit = {},
    ): ApiResult<T> = executeInternal(
        method = method,
        path = path,
        authenticated = authenticated,
        body = body,
        cacheKey = cacheKey,
        configure = configure,
        decode = { raw -> json.decodeFromString<ApiEnvelope<T>>(raw) },
    )

    @PublishedApi
    internal suspend fun <T> executeInternal(
        method: HttpMethod,
        path: String,
        authenticated: Boolean,
        body: JsonElement?,
        cacheKey: String?,
        configure: HttpRequestBuilder.() -> Unit,
        decode: (String) -> ApiEnvelope<T>,
    ): ApiResult<T> {
        return try {
            val response =
                client.request("$normalizedBaseUrl$path") {
                    this.method = method
                    contentType(ContentType.Application.Json)
                    if (authenticated) {
                        tokenStore.read()?.accessToken?.let(::bearerAuth)
                    }
                    body?.let(::setBody)
                    configure()
                }
            val rawBody = response.body<String>()
            val envelope = decodeEnvelope(rawBody, decode) ?: return decodeFailure(rawBody, decode)
            if (response.status.value !in HTTP_SUCCESS_MIN..HTTP_SUCCESS_MAX) {
                ApiResult.HttpFailure(
                    status = response.status.value,
                    code = envelope.code,
                    message = envelope.message,
                    retryAfterSeconds = response.headers["Retry-After"]?.toLongOrNull(),
                )
            } else {
                cacheKey?.let { responseCache.write(it, rawBody) }
                envelope.toSuccess(ResponseSource.NETWORK)
            }
        } catch (exception: IOException) {
            cachedResult(cacheKey, decode) ?: ApiResult.NetworkFailure(exception)
        }
    }

    private fun <T> decodeEnvelope(rawBody: String, decode: (String) -> ApiEnvelope<T>): ApiEnvelope<T>? = try {
        decode(rawBody)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun <T> decodeFailure(rawBody: String, decode: (String) -> ApiEnvelope<T>): ApiResult<T> = try {
        decode(rawBody)
        error("도달할 수 없는 코드")
    } catch (exception: SerializationException) {
        ApiResult.DecodeFailure(rawBody, exception)
    } catch (exception: IllegalArgumentException) {
        ApiResult.DecodeFailure(rawBody, exception)
    }

    private fun <T> cachedResult(cacheKey: String?, decode: (String) -> ApiEnvelope<T>): ApiResult.Success<T>? {
        val rawBody = cacheKey?.let(responseCache::read) ?: return null
        return decodeEnvelope(rawBody, decode)?.toSuccess(ResponseSource.CACHE)
    }

    private fun <T> ApiEnvelope<T>.toSuccess(source: ResponseSource) = ApiResult.Success(
        data = data,
        status = status,
        code = code,
        message = message,
        source = source,
    )
}
