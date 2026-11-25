package com.yu_morph.smartbudget.data.remote.api

import com.yu_morph.smartbudget.data.remote.ApiRequestExecutor
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.model.ReportDetailData
import com.yu_morph.smartbudget.data.remote.model.ReportPageData
import com.yu_morph.smartbudget.data.remote.model.RequestReportRequest
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement

data class ReportListQuery(
    val periodType: String? = null,
    val periodStart: String? = null,
    val limit: Int? = null,
    val cursor: String? = null,
)

class ReportApi(private val executor: ApiRequestExecutor) {
    fun invalidateCache() {
        executor.invalidateResponseCache()
    }

    suspend fun list(query: ReportListQuery = ReportListQuery()): ApiResult<ReportPageData> = executor.execute(
        method = HttpMethod.Get,
        path = "/api/v1/report",
        authenticated = true,
        cacheKey = "reports:$query",
    ) {
        parameter("period_type", query.periodType)
        parameter("period_start", query.periodStart)
        parameter("limit", query.limit)
        parameter("cursor", query.cursor)
    }

    suspend fun requestReport(
        request: RequestReportRequest,
        idempotencyKey: String? = null,
    ): ApiResult<ReportDetailData> = executor.execute<ReportDetailData>(
        method = HttpMethod.Post,
        path = "/api/v1/report",
        authenticated = true,
        body = executor.json.parseToJsonElement(executor.json.encodeToString(request)),
    ) {
        idempotencyKey?.let { header("Idempotency-Key", it) }
        timeout {
            requestTimeoutMillis = REPORT_REQUEST_TIMEOUT_MILLIS
            socketTimeoutMillis = REPORT_REQUEST_TIMEOUT_MILLIS
        }
    }.also(::invalidateCacheOnSuccess)

    suspend fun get(id: Int): ApiResult<ReportDetailData> = executor.execute(
        method = HttpMethod.Get,
        path = "/api/v1/report/$id",
        authenticated = true,
        cacheKey = "report:$id",
    )

    suspend fun delete(id: Int): ApiResult<JsonElement> = executor.execute<JsonElement>(
        method = HttpMethod.Delete,
        path = "/api/v1/report/$id",
        authenticated = true,
    ).also(::invalidateCacheOnSuccess)

    private fun invalidateCacheOnSuccess(result: ApiResult<*>) {
        if (result is ApiResult.Success) invalidateCache()
    }

    private companion object {
        /** 서버의 외부 LLM 제한 60초에 요청·응답 전송 여유를 더한다. */
        const val REPORT_REQUEST_TIMEOUT_MILLIS = 70_000L
    }
}
