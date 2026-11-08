package com.yu_morph.smartbudget.data.remote

import com.yu_morph.smartbudget.data.local.ApiResponseCache
import com.yu_morph.smartbudget.data.local.AuthTokenStore
import com.yu_morph.smartbudget.data.local.StoredAuthToken
import com.yu_morph.smartbudget.data.remote.api.AuthApi
import com.yu_morph.smartbudget.data.remote.api.OcrApi
import com.yu_morph.smartbudget.data.remote.api.ReportApi
import com.yu_morph.smartbudget.data.remote.model.PatchValue
import com.yu_morph.smartbudget.data.remote.model.RequestReportRequest
import com.yu_morph.smartbudget.data.remote.model.UpdateTransactionRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiRequestExecutorTest {
    @Test
    fun `인증 요청은 암호화 저장소의 토큰을 사용한다`() = runBlocking {
        var authorization: String? = null
        val stores = TestStores(token = TEST_TOKEN)
        val executor =
            executor(stores) { request ->
                authorization = request.headers[HttpHeaders.Authorization]
                respond(ACCOUNT_RESPONSE, headers = jsonHeaders())
            }

        AuthApi(executor, stores).getAccount()

        assertEquals("Bearer access-token", authorization)
    }

    @Test
    fun `토큰 갱신 요청에는 인증 헤더를 보내지 않고 새 토큰을 저장한다`() = runBlocking {
        var authorization: String? = "확인 전"
        val stores = TestStores(token = TEST_TOKEN)
        val executor =
            executor(stores) { request ->
                authorization = request.headers[HttpHeaders.Authorization]
                respond(TOKEN_RESPONSE, headers = jsonHeaders())
            }

        val result = AuthApi(executor, stores).refresh("old-token")

        assertTrue(result is ApiResult.Success)
        assertNull(authorization)
        assertEquals("new-token", stores.read()?.accessToken)
    }

    @Test
    fun `조회 실패 시 저장된 응답을 반환한다`() = runBlocking {
        val stores = TestStores()
        stores.write("version", VERSION_RESPONSE)
        val executor = executor(stores) { throw IOException("네트워크 연결 실패") }

        val result =
            executor.execute<com.yu_morph.smartbudget.data.remote.model.VersionData>(
                method = io.ktor.http.HttpMethod.Get,
                path = "/api/v1/version",
                cacheKey = "version",
            )

        assertTrue(result is ApiResult.Success)
        assertEquals(ResponseSource.CACHE, (result as ApiResult.Success).source)
        assertEquals("1.2.3", result.data?.version)
    }

    @Test
    fun `오류 코드와 재시도 시간을 보존한다`() = runBlocking {
        val stores = TestStores()
        val executor =
            executor(stores) {
                respond(
                    content = ERROR_RESPONSE,
                    status = HttpStatusCode.TooManyRequests,
                    headers =
                        headersOf(
                            HttpHeaders.ContentType to listOf("application/json"),
                            "Retry-After" to listOf("30"),
                        ),
                )
            }

        val result =
            executor.execute<com.yu_morph.smartbudget.data.remote.model.VersionData>(
                method = io.ktor.http.HttpMethod.Get,
                path = "/api/v1/version",
            )

        assertTrue(result is ApiResult.HttpFailure)
        result as ApiResult.HttpFailure
        assertEquals("SIGN_IN_RATE_LIMITED", result.code)
        assertEquals(30L, result.retryAfterSeconds)
    }

    @Test
    fun `미구현 API는 네트워크 요청 없이 템플릿 결과를 반환한다`() = runBlocking {
        val stores = TestStores()
        val executor = executor(stores) { error("네트워크 요청이 발생하면 안 됩니다") }

        val reportResult =
            ReportApi(executor).requestReport(RequestReportRequest("monthly", "2026-11-01"))
        val ocrResult = OcrApi().recognize(emptyList())

        assertTrue(reportResult is ApiResult.NotImplemented)
        assertTrue(ocrResult is ApiResult.NotImplemented)
    }

    @Test
    fun `거래 수정은 생략과 명시적 null을 구분한다`() {
        val json = SmartBudgetHttpClientFactory.defaultJson()
        val body =
            UpdateTransactionRequest(
                storeName = PatchValue.Present(null),
                note = PatchValue.Omitted,
            ).toJson(json)

        assertEquals(JsonNull, body["store_name"])
        assertFalse(body.containsKey("note"))
    }

    private fun executor(
        stores: TestStores,
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(
            io.ktor.client.request.HttpRequestData,
        ) -> io.ktor.client.request.HttpResponseData,
    ): ApiRequestExecutor {
        val json = SmartBudgetHttpClientFactory.defaultJson()
        val client =
            HttpClient(MockEngine(handler)) {
                expectSuccess = false
                install(ContentNegotiation) { json(json) }
            }
        return ApiRequestExecutor("https://example.test", client, stores, stores, json)
    }

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

    private class TestStores(token: StoredAuthToken? = null) :
        AuthTokenStore,
        ApiResponseCache {
        private var storedToken = token
        private val responses = mutableMapOf<String, String>()

        override fun read(): StoredAuthToken? = storedToken
        override fun write(token: StoredAuthToken) {
            storedToken = token
        }
        override fun clear() {
            storedToken = null
        }
        override fun read(key: String): String? = responses[key]
        override fun write(key: String, value: String) {
            responses[key] = value
        }
        override fun remove(key: String) {
            responses.remove(key)
        }
    }

    private companion object {
        val TEST_TOKEN = StoredAuthToken("access-token", "2026-11-09T00:00:00+09:00", "Bearer")
        const val ACCOUNT_RESPONSE =
            """{"status":200,"code":"OK","message":"success","data":{"username":"user","display_name":"사용자"}}"""
        const val TOKEN_RESPONSE =
            """{"status":200,"code":"OK","message":"success","data":{"token":"new-token","expires_at":"2026-11-10T00:00:00+09:00","token_type":"Bearer"}}"""
        const val VERSION_RESPONSE =
            """{"status":200,"code":"OK","message":"success","data":{"version":"1.2.3","channel":"development","build_timestamp":"2026-11-08T00:00:00+09:00"}}"""
        const val ERROR_RESPONSE =
            """{"status":429,"code":"SIGN_IN_RATE_LIMITED","message":"retry","data":null}"""
    }
}
