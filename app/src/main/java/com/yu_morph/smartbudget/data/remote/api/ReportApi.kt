package com.yu_morph.smartbudget.data.remote.api

import com.yu_morph.smartbudget.data.remote.ApiRequestExecutor
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.model.ReportDetailData
import com.yu_morph.smartbudget.data.remote.model.ReportPageData
import com.yu_morph.smartbudget.data.remote.model.RequestReportRequest
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonElement

data class ReportListQuery(
    val periodType: String? = null,
    val periodStart: String? = null,
    val limit: Int? = null,
    val cursor: String? = null,
)

class ReportApi(private val executor: ApiRequestExecutor) {
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

    @Suppress("UNUSED_PARAMETER")
    suspend fun requestReport(request: RequestReportRequest): ApiResult<ReportDetailData> =
        ApiResult.NotImplemented("소비 분석 요청")

    suspend fun get(id: Int): ApiResult<ReportDetailData> = executor.execute(
        method = HttpMethod.Get,
        path = "/api/v1/report/$id",
        authenticated = true,
        cacheKey = "report:$id",
    )

    suspend fun delete(id: Int): ApiResult<JsonElement> = executor.execute(
        method = HttpMethod.Delete,
        path = "/api/v1/report/$id",
        authenticated = true,
    )
}
