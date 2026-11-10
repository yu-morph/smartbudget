package com.yu_morph.smartbudget.ui.main

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.local.AppSettings
import com.yu_morph.smartbudget.data.local.DisplayMode
import com.yu_morph.smartbudget.data.local.PreferenceStoreFactory
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.SmartBudgetApi
import com.yu_morph.smartbudget.data.remote.model.ReplaceMonthlyBudgetRequest
import com.yu_morph.smartbudget.data.remote.model.SignInRequest
import com.yu_morph.smartbudget.data.remote.model.UpdateAccountRequest
import java.time.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val BUDGET_HISTORY_MONTHS = 12L

data class BudgetHistoryItem(val month: YearMonth, val amount: Int)

data class SettingsUiState(
    val displayName: String = "",
    val userId: String = "",
    val versionName: String = "",
    val buildDateTime: String = "",
    val appSettings: AppSettings = AppSettings(),
    val budgetAmount: Int = DEFAULT_MONTHLY_BUDGET,
    val budgetHistory: List<BudgetHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
)

@Suppress("TooManyFunctions")
class SettingsCoordinator(
    context: Context,
    private val api: SmartBudgetApi,
    private val scope: CoroutineScope,
    private val onDisplayModeChanged: (DisplayMode) -> Unit,
) {
    private val applicationContext = context.applicationContext
    private val userIdentityStore = PreferenceStoreFactory.userIdentity(applicationContext)
    private val appSettingsStore = PreferenceStoreFactory.appSettings(applicationContext)

    var state by mutableStateOf(
        SettingsUiState(
            userId = userIdentityStore.readUsername().orEmpty(),
            versionName = readVersionName(),
            buildDateTime = BuildMetadata.BUILD_DATETIME,
            appSettings = appSettingsStore.read(),
        ),
    )
        private set

    fun refresh() {
        state = state.copy(userId = userIdentityStore.readUsername().orEmpty())
        scope.launch {
            val result = api.auth.getAccount()
            if (result is ApiResult.Success) {
                state = state.copy(displayName = result.data?.displayName.orEmpty())
            }
        }
    }

    fun updateDisplayName(displayName: String, onSuccess: () -> Unit) {
        execute(
            request = { api.auth.updateAccount(UpdateAccountRequest(displayName = displayName)) },
            onSuccess = {
                state = state.copy(displayName = displayName)
                showToast(R.string.settings_profile_updated)
                onSuccess()
            },
        )
    }

    fun updatePassword(currentPassword: String, newPassword: String, onSuccess: () -> Unit) {
        execute(
            request = {
                api.auth.updateAccount(
                    UpdateAccountRequest(currentPassword = currentPassword, password = newPassword),
                )
            },
            onSuccess = {
                userIdentityStore.clear()
                showToast(R.string.settings_password_updated)
                onSuccess()
            },
        )
    }

    fun verifyWithdrawalPassword(password: String, onVerified: () -> Unit) {
        val username = userIdentityStore.readUsername().orEmpty()
        if (username.isBlank()) {
            showToast(R.string.settings_user_id_unavailable)
            return
        }
        execute(
            request = { api.auth.signIn(SignInRequest(username, password)) },
            onSuccess = onVerified,
        )
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        execute(
            request = api.auth::deleteAccount,
            onSuccess = {
                userIdentityStore.clear()
                showToast(R.string.settings_account_deleted)
                onSuccess()
            },
        )
    }

    fun loadMonthlyBudget() {
        if (state.isLoading) return
        scope.launch {
            state = state.copy(isLoading = true)
            try {
                val currentMonth = YearMonth.now()
                val currentResult = api.monthlyBudgets.get(currentMonth.toString())
                if (currentResult !is ApiResult.Success) {
                    showFailure(currentResult)
                    return@launch
                }
                var currentAmount = currentResult.data?.amount
                if (currentAmount == null) {
                    val previousResult = api.monthlyBudgets.get(currentMonth.minusMonths(1).toString())
                    if (previousResult !is ApiResult.Success) {
                        showFailure(previousResult)
                        return@launch
                    }
                    currentAmount = resolveMonthlyBudget(null, previousResult.data?.amount)
                    val saved = api.monthlyBudgets.replace(
                        currentMonth.toString(),
                        ReplaceMonthlyBudgetRequest(currentAmount),
                    )
                    if (saved !is ApiResult.Success) {
                        showFailure(saved)
                        return@launch
                    }
                }
                val history = mutableListOf<BudgetHistoryItem>()
                for (offset in 1..BUDGET_HISTORY_MONTHS) {
                    val month = currentMonth.minusMonths(offset)
                    when (val result = api.monthlyBudgets.get(month.toString())) {
                        is ApiResult.Success -> result.data?.amount?.let { history += BudgetHistoryItem(month, it) }

                        else -> {
                            showFailure(result)
                            break
                        }
                    }
                }
                state = state.copy(budgetAmount = currentAmount, budgetHistory = history)
            } catch (_: Exception) {
                showToast(R.string.unknown_error)
            } finally {
                state = state.copy(isLoading = false)
            }
        }
    }

    fun saveMonthlyBudget(amount: Int) {
        execute(
            request = {
                api.monthlyBudgets.replace(
                    YearMonth.now().toString(),
                    ReplaceMonthlyBudgetRequest(amount),
                )
            },
            onSuccess = {
                state = state.copy(budgetAmount = amount)
                showToast(R.string.settings_budget_saved)
            },
        )
    }

    fun setLanguage(languageTag: String) {
        updateAppSettings(state.appSettings.copy(languageTag = languageTag))
    }

    fun setNotification(type: NotificationSetting, enabled: Boolean) {
        val current = state.appSettings
        updateAppSettings(
            when (type) {
                NotificationSetting.BUDGET_APPROACHING -> current.copy(budgetApproachingNotification = enabled)
                NotificationSetting.BUDGET_EXCEEDED -> current.copy(budgetExceededNotification = enabled)
                NotificationSetting.ANALYSIS -> current.copy(analysisNotification = enabled)
            },
        )
    }

    fun setDisplayMode(mode: DisplayMode) {
        updateAppSettings(state.appSettings.copy(displayMode = mode))
        onDisplayModeChanged(mode)
    }

    private fun updateAppSettings(settings: AppSettings) {
        appSettingsStore.write(settings)
        state = state.copy(appSettings = settings)
    }

    private fun <T> execute(request: suspend () -> ApiResult<T>, onSuccess: () -> Unit) {
        if (state.isLoading) return
        scope.launch {
            state = state.copy(isLoading = true)
            try {
                when (val result = request()) {
                    is ApiResult.Success -> onSuccess()
                    else -> showFailure(result)
                }
            } catch (_: Exception) {
                showToast(R.string.unknown_error)
            } finally {
                state = state.copy(isLoading = false)
            }
        }
    }

    private fun showFailure(result: ApiResult<*>) {
        val message = when (result) {
            is ApiResult.HttpFailure -> when (result.code) {
                "CURRENT_PASSWORD_INCORRECT", "INVALID_CREDENTIALS" -> R.string.settings_password_incorrect
                "INVALID_REQUEST" -> R.string.invalid_request_error
                "SERVICE_UNAVAILABLE" -> R.string.service_unavailable_error
                else -> R.string.unknown_error
            }

            is ApiResult.NetworkFailure -> R.string.network_error

            is ApiResult.DecodeFailure -> R.string.response_error

            is ApiResult.NotImplemented -> R.string.service_unavailable_error

            is ApiResult.Success -> return
        }
        showToast(message)
    }

    private fun showToast(@StringRes message: Int) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun readVersionName(): String = applicationContext.packageManager
        .getPackageInfo(applicationContext.packageName, 0)
        .versionName
        .orEmpty()
}

enum class NotificationSetting { BUDGET_APPROACHING, BUDGET_EXCEEDED, ANALYSIS }
