package com.yu_morph.smartbudget.ui.auth

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.local.PreferenceStoreFactory
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.SmartBudgetApi
import com.yu_morph.smartbudget.data.remote.model.SignInRequest
import com.yu_morph.smartbudget.data.remote.model.SignUpRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AuthCoordinator(context: Context, private val api: SmartBudgetApi, private val scope: CoroutineScope) {
    private val applicationContext = context.applicationContext
    private val userIdentityStore = PreferenceStoreFactory.userIdentity(applicationContext)

    var isLoading by mutableStateOf(false)
        private set

    fun restoreSession(onAuthenticated: () -> Unit, onSignInRequired: () -> Unit) {
        if (!api.auth.hasValidStoredToken()) {
            onSignInRequired()
            return
        }
        executeRequest(
            request = api.auth::validateSession,
            isSuccessful = { it.data != null },
            onSuccess = onAuthenticated,
            onFailure = { result ->
                if (result is ApiResult.HttpFailure && result.status == HTTP_UNAUTHORIZED) api.auth.signOut()
                onSignInRequired()
            },
        )
    }

    fun signIn(username: String, password: String, onSuccess: () -> Unit) {
        executeRequest(
            request = { api.auth.signIn(SignInRequest(username, password)) },
            isSuccessful = { it.data != null },
            onSuccess = {
                userIdentityStore.writeUsername(username)
                onSuccess()
            },
        )
    }

    fun signUp(displayName: String, username: String, password: String, onSuccess: () -> Unit) {
        executeRequest(
            request = {
                api.auth.signUp(
                    SignUpRequest(
                        username = username,
                        password = password,
                        displayName = displayName,
                    ),
                )
            },
            onSuccess = {
                showToast(R.string.sign_up_success)
                onSuccess()
            },
        )
    }

    private fun <T> executeRequest(
        request: suspend () -> ApiResult<T>,
        isSuccessful: (ApiResult.Success<T>) -> Boolean = { true },
        onSuccess: () -> Unit,
        onFailure: (ApiResult<T>) -> Unit = {},
    ) {
        if (isLoading) return
        scope.launch {
            isLoading = true
            try {
                val result = request()
                when {
                    result is ApiResult.Success && isSuccessful(result) -> onSuccess()
                    result is ApiResult.Success -> showToast(R.string.response_error)
                    else -> showFailure(result)
                }
                if (result !is ApiResult.Success || !isSuccessful(result)) onFailure(result)
            } catch (_: Exception) {
                showToast(R.string.unknown_error)
            } finally {
                isLoading = false
            }
        }
    }

    private fun showFailure(result: ApiResult<*>) {
        val messageRes =
            when (result) {
                is ApiResult.HttpFailure -> result.code.toErrorMessage()
                is ApiResult.NetworkFailure -> R.string.network_error
                is ApiResult.DecodeFailure -> R.string.response_error
                is ApiResult.NotImplemented -> R.string.unknown_error
                is ApiResult.Success -> return
            }
        showToast(messageRes)
    }

    private fun String?.toErrorMessage(): Int = when (this) {
        "INVALID_CREDENTIALS" -> R.string.invalid_credentials_error
        "USERNAME_CONFLICT" -> R.string.username_conflict_error
        "SIGN_IN_RATE_LIMITED" -> R.string.sign_in_rate_limited_error
        "INVALID_REQUEST" -> R.string.invalid_request_error
        "SERVICE_UNAVAILABLE" -> R.string.service_unavailable_error
        else -> R.string.unknown_error
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(applicationContext, messageRes, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
