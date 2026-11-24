package com.yu_morph.smartbudget.data.remote.api

import com.yu_morph.smartbudget.data.remote.ApiRequestExecutor
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.model.BatchCreateTransactionsRequest
import com.yu_morph.smartbudget.data.remote.model.BatchDeleteTransactionsData
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
    fun invalidateCache() {
        executor.invalidateResponseCache()
    }

    suspend fun list(query: TransactionListQuery = TransactionListQuery()): ApiResult<TransactionPageData> =
        executor.execute<TransactionPageData>(
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
        executor.execute<TransactionData>(
            method = HttpMethod.Post,
            path = "/api/v1/transaction",
            authenticated = true,
            body = executor.json.parseToJsonElement(executor.json.encodeToString(request)),
        ) {
            idempotencyKey?.let { header("Idempotency-Key", it) }
        }.also(::invalidateCacheOnSuccess)

    suspend fun createBatch(
        requests: List<CreateTransactionRequest>,
        startDate: String,
        endDate: String,
        idempotencyKey: String? = null,
    ): ApiResult<List<TransactionData>> = executor.execute<List<TransactionData>>(
        method = HttpMethod.Post,
        path = "/api/v1/transaction/batch",
        authenticated = true,
        body =
            executor.json.parseToJsonElement(
                executor.json.encodeToString(BatchCreateTransactionsRequest(requests)),
            ),
    ) {
        parameter("start_date", startDate)
        parameter("end_date", endDate)
        idempotencyKey?.let { header("Idempotency-Key", it) }
    }.also(::invalidateCacheOnSuccess)

    suspend fun get(id: Int): ApiResult<TransactionData> = executor.execute(
        method = HttpMethod.Get,
        path = "/api/v1/transaction/$id",
        authenticated = true,
        cacheKey = "transaction:$id",
    )

    suspend fun update(id: Int, request: UpdateTransactionRequest): ApiResult<TransactionData> =
        executor.execute<TransactionData>(
            method = HttpMethod.Patch,
            path = "/api/v1/transaction/$id",
            authenticated = true,
            body = request.toJson(executor.json),
        ).also(::invalidateCacheOnSuccess)

    suspend fun delete(id: Int): ApiResult<JsonElement> = executor.execute<JsonElement>(
        method = HttpMethod.Delete,
        path = "/api/v1/transaction/$id",
        authenticated = true,
    ).also(::invalidateCacheOnSuccess)

    suspend fun deleteBatch(
        startDate: String,
        endDate: String,
        limit: Int? = null,
        cursor: String? = null,
    ): ApiResult<BatchDeleteTransactionsData> = executor.execute<BatchDeleteTransactionsData>(
        method = HttpMethod.Delete,
        path = "/api/v1/transaction/batch",
        authenticated = true,
    ) {
        parameter("start_date", startDate)
        parameter("end_date", endDate)
        parameter("limit", limit)
        parameter("cursor", cursor)
    }.also(::invalidateCacheOnSuccess)

    private fun invalidateCacheOnSuccess(result: ApiResult<*>) {
        if (result is ApiResult.Success) invalidateCache()
    }
}
