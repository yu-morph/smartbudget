package com.yu_morph.smartbudget.data.remote.api

import com.yu_morph.smartbudget.data.local.AuthTokenStore
import com.yu_morph.smartbudget.data.local.StoredAuthToken
import com.yu_morph.smartbudget.data.local.isValidAt
import com.yu_morph.smartbudget.data.remote.ApiRequestExecutor
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.model.AccountData
import com.yu_morph.smartbudget.data.remote.model.AuthTokenData
import com.yu_morph.smartbudget.data.remote.model.RefreshRequest
import com.yu_morph.smartbudget.data.remote.model.SignInRequest
import com.yu_morph.smartbudget.data.remote.model.SignUpRequest
import com.yu_morph.smartbudget.data.remote.model.UpdateAccountRequest
import io.ktor.http.HttpMethod
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement

class AuthApi(private val executor: ApiRequestExecutor, private val tokenStore: AuthTokenStore) {
    suspend fun signUp(request: SignUpRequest): ApiResult<JsonElement> = executor.execute(
        method = HttpMethod.Post,
        path = "/api/v1/auth/sign-up",
        body = executor.json.parseToJsonElement(executor.json.encodeToString(request)),
    )

    suspend fun signIn(request: SignInRequest): ApiResult<AuthTokenData> = executor
        .execute<AuthTokenData>(
            method = HttpMethod.Post,
            path = "/api/v1/auth/sign-in",
            body = executor.json.parseToJsonElement(executor.json.encodeToString(request)),
        ).also(::storeTokenOnSuccess)

    suspend fun refresh(token: String): ApiResult<AuthTokenData> = executor
        .execute<AuthTokenData>(
            method = HttpMethod.Post,
            path = "/api/v1/auth/refresh",
            body =
                executor.json.parseToJsonElement(
                    executor.json.encodeToString(RefreshRequest(token)),
                ),
        ).also(::storeTokenOnSuccess)

    suspend fun getAccount(): ApiResult<AccountData> = executor.execute(
        method = HttpMethod.Get,
        path = "/api/v1/auth/account",
        authenticated = true,
        cacheKey = "account",
    )

    suspend fun validateSession(): ApiResult<AccountData> = executor.execute(
        method = HttpMethod.Get,
        path = "/api/v1/auth/account",
        authenticated = true,
    )

    suspend fun updateAccount(request: UpdateAccountRequest): ApiResult<AccountData> = executor
        .execute<AccountData>(
            method = HttpMethod.Patch,
            path = "/api/v1/auth/account",
            authenticated = true,
            body = executor.json.parseToJsonElement(executor.json.encodeToString(request)),
        ).also { result ->
            if (request.password != null && result is ApiResult.Success) tokenStore.clear()
        }

    suspend fun deleteAccount(): ApiResult<JsonElement> = executor
        .execute<JsonElement>(
            method = HttpMethod.Delete,
            path = "/api/v1/auth/account",
            authenticated = true,
        ).also { result -> if (result is ApiResult.Success) tokenStore.clear() }

    fun signOut() = tokenStore.clear()

    fun hasValidStoredToken(): Boolean {
        val isValid = tokenStore.read()?.isValidAt() == true
        if (!isValid) tokenStore.clear()
        return isValid
    }

    private fun storeTokenOnSuccess(result: ApiResult<AuthTokenData>) {
        val token = (result as? ApiResult.Success)?.data ?: return
        tokenStore.write(
            StoredAuthToken(
                accessToken = token.token,
                expiresAt = token.expiresAt,
                tokenType = token.tokenType,
            ),
        )
    }
}
