package com.yu_morph.smartbudget.data.remote.api

import com.yu_morph.smartbudget.data.remote.ApiRequestExecutor
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.model.CreateTransactionRequest
import com.yu_morph.smartbudget.data.remote.model.TransactionData
import com.yu_morph.smartbudget.data.remote.model.TransactionPageData
import com.yu_morph.smartbudget.data.remote.model.UpdateTransactionRequest
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement

data class TransactionListQuery(
    val startDate: String? = null,
    val endDate: String? = null,
    val limit: Int? = null,
    val cursor: String? = null,
)

class TransactionApi(private val executor: ApiRequestExecutor) {
    suspend fun list(query: TransactionListQuery = TransactionListQuery()): ApiResult<TransactionPageData> =
        executor.execute(
            method = HttpMethod.Get,
            path = "/api/v1/transaction",
            authenticated = true,
            cacheKey = "transactions:$query",
        ) {
            parameter("start_date", query.startDate)
            parameter("end_date", query.endDate)
            parameter("limit", query.limit)
            parameter("cursor", query.cursor)
        }

    suspend fun create(request: CreateTransactionRequest, idempotencyKey: String? = null): ApiResult<TransactionData> =
        executor.execute(
            method = HttpMethod.Post,
            path = "/api/v1/transaction",
            authenticated = true,
            body = executor.json.parseToJsonElement(executor.json.encodeToString(request)),
        ) {
            idempotencyKey?.let { header("Idempotency-Key", it) }
        }

    suspend fun get(id: Int): ApiResult<TransactionData> = executor.execute(
        method = HttpMethod.Get,
        path = "/api/v1/transaction/$id",
        authenticated = true,
        cacheKey = "transaction:$id",
    )

    suspend fun update(id: Int, request: UpdateTransactionRequest): ApiResult<TransactionData> = executor.execute(
        method = HttpMethod.Patch,
        path = "/api/v1/transaction/$id",
        authenticated = true,
        body = request.toJson(executor.json),
    )

    suspend fun delete(id: Int): ApiResult<JsonElement> = executor.execute(
        method = HttpMethod.Delete,
        path = "/api/v1/transaction/$id",
        authenticated = true,
    )
}
