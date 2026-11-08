package com.yu_morph.smartbudget.data.remote.api

import com.yu_morph.smartbudget.data.remote.ApiRequestExecutor
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.model.MonthlyBudgetData
import com.yu_morph.smartbudget.data.remote.model.ReplaceMonthlyBudgetRequest
import io.ktor.http.HttpMethod
import kotlinx.serialization.encodeToString

class MonthlyBudgetApi(private val executor: ApiRequestExecutor) {
    suspend fun get(month: String): ApiResult<MonthlyBudgetData> = executor.execute(
        method = HttpMethod.Get,
        path = "/api/v1/monthly-budget/$month",
        authenticated = true,
        cacheKey = "monthly-budget:$month",
    )

    suspend fun replace(month: String, request: ReplaceMonthlyBudgetRequest): ApiResult<MonthlyBudgetData> =
        executor.execute(
            method = HttpMethod.Put,
            path = "/api/v1/monthly-budget/$month",
            authenticated = true,
            body = executor.json.parseToJsonElement(executor.json.encodeToString(request)),
        )
}
