package com.yu_morph.smartbudget.ui.main

import android.content.Context
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
import com.yu_morph.smartbudget.data.remote.model.AccountData
import com.yu_morph.smartbudget.data.remote.model.CreateTransactionRequest
import com.yu_morph.smartbudget.data.remote.model.ReplaceMonthlyBudgetRequest
import com.yu_morph.smartbudget.data.remote.model.TransactionSource
import com.yu_morph.smartbudget.data.remote.model.TransactionType
import com.yu_morph.smartbudget.data.remote.model.UpdateAccountRequest
import com.yu_morph.smartbudget.ui.showAppToast
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BUDGET_HISTORY_MONTHS = 3
private const val TRANSACTION_BATCH_LIMIT = 500
private const val BANK_SYNC_START_DATE = "2025-01-01"
private const val BANK_SYNC_END_DATE = "2026-12-31"
private const val BANK_SYNC_MAX_ATTEMPTS = 3
private const val BANK_SYNC_RETRY_DELAY_MILLIS = 250L
private const val HTTP_SERVICE_UNAVAILABLE = 503
private const val IDEMPOTENCY_REQUEST_IN_PROGRESS = "IDEMPOTENCY_REQUEST_IN_PROGRESS"

data class BudgetHistoryItem(val month: YearMonth, val amount: Int)

data class SettingsUiState(
    val displayName: String = "",
    val userId: String = "",
    val profileImageUri: String? = null,
    val versionName: String = "",
    val buildDateTime: String = "",
    val appSettings: AppSettings = AppSettings(),
    val budgetAmount: Int = DEFAULT_MONTHLY_BUDGET,
    val budgetHistory: List<BudgetHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isBankSyncing: Boolean = false,
)

@Suppress("TooManyFunctions")
class SettingsCoordinator(
    context: Context,
    private val api: SmartBudgetApi,
    private val scope: CoroutineScope,
    private val onDisplayModeChanged: (DisplayMode) -> Unit,
    private val onLanguageChanged: () -> Unit,
) {
    private val applicationContext = context.applicationContext
    private val userIdentityStore = PreferenceStoreFactory.userIdentity(applicationContext)
    private val profileImageStore = PreferenceStoreFactory.profileImage(applicationContext)
    private val appSettingsStore = PreferenceStoreFactory.appSettings(applicationContext)

    var state by mutableStateOf(
        SettingsUiState(
            userId = userIdentityStore.readUsername().orEmpty(),
            profileImageUri = profileImageStore.readUri(),
            versionName = readVersionName(),
            buildDateTime = BuildMetadata.BUILD_DATETIME,
            appSettings = appSettingsStore.read(),
        ),
    )
        private set

    fun refresh() {
        state =
            state.copy(
                userId = userIdentityStore.readUsername().orEmpty(),
                profileImageUri = profileImageStore.readUri(),
            )
        scope.launch {
            val result = api.auth.getAccount()
            if (result is ApiResult.Success && result.data != null) {
                persistAccount(result.data)
            }
        }
    }

    fun saveAccountSettings(
        displayName: String,
        currentPassword: String,
        newPassword: String,
        budgetAmount: Int,
        profileChanged: Boolean,
        budgetChanged: Boolean,
        onSuccess: (Boolean) -> Unit,
    ) {
        if (state.isLoading) return
        val passwordChanged = newPassword.isNotBlank()
        scope.launch {
            state = state.copy(isLoading = true)
            try {
                if (budgetChanged) {
                    val budgetResult =
                        api.monthlyBudgets.replace(
                            YearMonth.now().toString(),
                            ReplaceMonthlyBudgetRequest(budgetAmount),
                        )
                    if (budgetResult !is ApiResult.Success) {
                        showFailure(budgetResult)
                        return@launch
                    }
                }
                if (profileChanged) {
                    val profileResult =
                        api.auth.updateAccount(
                            UpdateAccountRequest(
                                displayName = displayName,
                                currentPassword = currentPassword.takeIf { passwordChanged },
                                password = newPassword.takeIf { passwordChanged },
                            ),
                        )
                    if (profileResult !is ApiResult.Success) {
                        showFailure(profileResult)
                        return@launch
                    }
                    profileResult.data?.let(::persistAccount)
                }
                updateSavedAccountState(displayName, budgetAmount, budgetChanged)
                showAccountSaveResult(passwordChanged, profileChanged, budgetChanged)
                onSuccess(passwordChanged)
            } catch (_: Exception) {
                showToast(R.string.unknown_error)
            } finally {
                state = state.copy(isLoading = false)
            }
        }
    }

    fun updateProfileImage(uri: String) {
        if (state.isLoading) return
        scope.launch {
            state = state.copy(isLoading = true)
            try {
                val encodedImage = profileImageStore.encodeUri(uri)
                if (encodedImage == null) {
                    showToast(R.string.invalid_request_error)
                    return@launch
                }
                when (val result = api.auth.updateAccount(UpdateAccountRequest(profileImage = encodedImage))) {
                    is ApiResult.Success -> {
                        val account = result.data
                        if (account == null) {
                            showToast(R.string.response_error)
                        } else {
                            persistAccount(account)
                            showToast(R.string.settings_profile_updated)
                        }
                    }

                    else -> showFailure(result)
                }
            } catch (_: Exception) {
                showToast(R.string.unknown_error)
            } finally {
                state = state.copy(isLoading = false)
            }
        }
    }

    private fun persistAccount(account: AccountData) {
        userIdentityStore.writeUsername(account.username)
        state =
            state.copy(
                displayName = account.displayName,
                userId = account.username,
                profileImageUri = profileImageStore.writeBase64(account.profileImage),
            )
    }

    private fun updateSavedAccountState(displayName: String, budgetAmount: Int, budgetChanged: Boolean) {
        val currentMonth = YearMonth.now()
        val budgetHistory =
            if (budgetChanged) {
                (
                    state.budgetHistory.filterNot { it.month == currentMonth } +
                        BudgetHistoryItem(currentMonth, budgetAmount)
                    ).sortedBy(BudgetHistoryItem::month)
            } else {
                state.budgetHistory
            }
        state = state.copy(displayName = displayName, budgetAmount = budgetAmount, budgetHistory = budgetHistory)
    }

    private fun showAccountSaveResult(passwordChanged: Boolean, profileChanged: Boolean, budgetChanged: Boolean) {
        val message =
            when {
                passwordChanged -> {
                    userIdentityStore.clear()
                    R.string.settings_password_updated
                }

                profileChanged && budgetChanged -> R.string.settings_account_settings_saved

                profileChanged -> R.string.settings_profile_updated

                else -> R.string.settings_budget_saved
            }
        showToast(message)
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
                history += BudgetHistoryItem(currentMonth, currentAmount)
                for (offset in 1 until BUDGET_HISTORY_MONTHS) {
                    val month = currentMonth.minusMonths(offset.toLong())
                    when (val result = api.monthlyBudgets.get(month.toString())) {
                        is ApiResult.Success -> result.data?.amount?.let { history += BudgetHistoryItem(month, it) }

                        else -> {
                            showFailure(result)
                            break
                        }
                    }
                }
                state =
                    state.copy(budgetAmount = currentAmount, budgetHistory = history.sortedBy(BudgetHistoryItem::month))
            } catch (_: Exception) {
                showToast(R.string.unknown_error)
            } finally {
                state = state.copy(isLoading = false)
            }
        }
    }

    fun syncBankTransactions(onSuccess: () -> Unit) {
        if (state.isBankSyncing) return
        scope.launch {
            state = state.copy(isBankSyncing = true)
            try {
                val result = api.mockTransactions.list(
                    startDate = BANK_SYNC_START_DATE,
                    endDate = BANK_SYNC_END_DATE,
                )
                if (result !is ApiResult.Success || result.data == null) {
                    showFailure(result)
                    return@launch
                }
                val requests =
                    result.data.transactions.map { transaction ->
                        CreateTransactionRequest(
                            date = transaction.date,
                            type =
                                if (transaction.type == "INCOME") {
                                    TransactionType.INCOME
                                } else {
                                    TransactionType.EXPENSE
                                },
                            source = TransactionSource.FINANCIAL_SYNC,
                            category = transaction.category,
                            storeName = transaction.description,
                            amount = transaction.amount,
                        )
                    }
                if (!deleteTransactionsInSyncRange()) return@launch
                if (requests.isNotEmpty() && !createTransactionsWithRetry(requests)) return@launch
                applicationContext.showAppToast(R.string.settings_bank_sync_success, requests.size)
                onSuccess()
            } catch (_: Exception) {
                showToast(R.string.unknown_error)
            } finally {
                state = state.copy(isBankSyncing = false)
            }
        }
    }

    private suspend fun deleteTransactionsInSyncRange(): Boolean = executeBankSyncMutation {
        api.transactions.deleteBatch(
            startDate = BANK_SYNC_START_DATE,
            endDate = BANK_SYNC_END_DATE,
        )
    }

    private suspend fun createTransactionsWithRetry(requests: List<CreateTransactionRequest>): Boolean {
        val syncSessionId = UUID.randomUUID().toString()
        requests.chunked(TRANSACTION_BATCH_LIMIT).forEachIndexed { index, batch ->
            val idempotencyKey = "bank-sync-$syncSessionId-$index"
            val succeeded = executeBankSyncMutation {
                api.transactions.createBatch(
                    requests = batch,
                    startDate = BANK_SYNC_START_DATE,
                    endDate = BANK_SYNC_END_DATE,
                    idempotencyKey = idempotencyKey,
                )
            }
            if (!succeeded) return false
        }
        return true
    }

    private suspend fun executeBankSyncMutation(request: suspend () -> ApiResult<*>): Boolean {
        var attempt = 1
        var result = request()
        while (
            result !is ApiResult.Success &&
            result.isTransientBankSyncFailure() &&
            attempt < BANK_SYNC_MAX_ATTEMPTS
        ) {
            delay(BANK_SYNC_RETRY_DELAY_MILLIS * (1L shl (attempt - 1)))
            result = request()
            attempt += 1
        }
        val succeeded = result is ApiResult.Success
        if (!succeeded) showFailure(result)
        return succeeded
    }

    fun setLanguage(languageTag: String) {
        updateAppSettings(state.appSettings.copy(languageTag = languageTag))
        onLanguageChanged()
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
        applicationContext.showAppToast(message)
    }

    private fun readVersionName(): String = applicationContext.packageManager
        .getPackageInfo(applicationContext.packageName, 0)
        .versionName
        .orEmpty()
}

enum class NotificationSetting { BUDGET_APPROACHING, BUDGET_EXCEEDED, ANALYSIS }

internal fun ApiResult<*>.isTransientBankSyncFailure(): Boolean = this is ApiResult.NetworkFailure ||
    this is ApiResult.DecodeFailure ||
    (
        this is ApiResult.HttpFailure &&
            (status == HTTP_SERVICE_UNAVAILABLE || code == IDEMPOTENCY_REQUEST_IN_PROGRESS)
        )
