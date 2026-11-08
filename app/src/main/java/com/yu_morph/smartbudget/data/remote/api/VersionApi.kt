package com.yu_morph.smartbudget.data.remote.api

import com.yu_morph.smartbudget.data.remote.ApiRequestExecutor
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.model.VersionData
import io.ktor.http.HttpMethod

class VersionApi(private val executor: ApiRequestExecutor) {
    suspend fun getVersion(): ApiResult<VersionData> = executor.execute(
        method = HttpMethod.Get,
        path = "/api/v1/version",
        cacheKey = "version",
    )
}
