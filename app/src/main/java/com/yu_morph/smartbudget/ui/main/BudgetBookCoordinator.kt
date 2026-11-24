package com.yu_morph.smartbudget.ui.main

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.SmartBudgetApi
import com.yu_morph.smartbudget.data.remote.api.TransactionListQuery
import com.yu_morph.smartbudget.data.remote.model.CreateTransactionRequest
import com.yu_morph.smartbudget.data.remote.model.TransactionData
import com.yu_morph.smartbudget.data.remote.model.UpdateTransactionRequest
import com.yu_morph.smartbudget.notification.BudgetNotificationCoordinator
import com.yu_morph.smartbudget.ui.showAppToast
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TRANSACTION_PAGE_LIMIT = 100
data class BudgetBookOverviewUiState(
    val referenceMonth: YearMonth? = null,
    val transactionsByMonth: Map<YearMonth, List<TransactionData>> = emptyMap(),
    val selectedBudgetAmount: Int? = null,
    val isLoading: Boolean = false,
)

data class BudgetBookUiState(
    val month: YearMonth? = null,
    val transactions: List<TransactionData> = emptyList(),
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
)

class BudgetBookCoordinator(
    context: Context,
    private val api: SmartBudgetApi,
    private val scope: CoroutineScope,
    private val budgetNotifications: BudgetNotificationCoordinator,
) {
    private val applicationContext = context.applicationContext
    private var loadJob: Job? = null
    private var overviewLoadJob: Job? = null

    var state by mutableStateOf(BudgetBookUiState())
        private set
    var overviewState by mutableStateOf(BudgetBookOverviewUiState())
        private set

    fun loadOverview(referenceMonth: YearMonth = YearMonth.now()) {
        val latestMonth = YearMonth.now()
        val selectedMonth = referenceMonth.coerceAtMost(latestMonth)
        overviewLoadJob?.cancel()
        overviewLoadJob = scope.launch {
            overviewState =
                overviewState.copy(
                    referenceMonth = selectedMonth,
                    selectedBudgetAmount = null,
                    isLoading = true,
                )
            try {
                val currentMonth = YearMonth.now()
                val historyTransactions =
                    loadAllTransactions(selectedMonth.minusMonths(1).atDay(1), selectedMonth.atEndOfMonth())
                        ?: return@launch
                val historyByMonth = groupTransactionsByMonth(historyTransactions)
                val currentTransactions =
                    historyByMonth[currentMonth]
                        ?: overviewState.transactionsByMonth[currentMonth]
                        ?: loadAllTransactions(currentMonth.atDay(1), currentMonth.atEndOfMonth())
                        ?: return@launch
                val budgetAmount = loadMonthlyBudget(selectedMonth)
                overviewState =
                    BudgetBookOverviewUiState(
                        referenceMonth = selectedMonth,
                        transactionsByMonth =
                            groupTransactionsByMonth(currentTransactions) +
                                historyByMonth,
                        selectedBudgetAmount = budgetAmount,
                    )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                applicationContext.showAppToast(R.string.unknown_error)
            } finally {
                if (overviewState.referenceMonth == selectedMonth) {
                    overviewState = overviewState.copy(isLoading = false)
                }
            }
        }
    }

    private suspend fun loadMonthlyBudget(month: YearMonth): Int? =
        when (val result = api.monthlyBudgets.get(month.toString())) {
            is ApiResult.Success -> result.data?.amount

            else -> {
                showFailure(result)
                null
            }
        }

    fun loadMonth(month: YearMonth) {
        loadJob?.cancel()
        loadJob = scope.launch {
            state = state.copy(month = month, transactions = emptyList(), isLoading = true)
            try {
                val transactions = loadAllTransactions(month.atDay(1), month.atEndOfMonth()) ?: return@launch
                state = state.copy(month = month, transactions = transactions)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                applicationContext.showAppToast(R.string.unknown_error)
            } finally {
                if (state.month == month) state = state.copy(isLoading = false)
            }
        }
    }

    fun createTransaction(request: CreateTransactionRequest, onSuccess: () -> Unit) {
        mutateTransaction(
            request = { api.transactions.create(request, UUID.randomUUID().toString()) },
            onSuccess = onSuccess,
        )
    }

    fun updateTransaction(id: Int, request: UpdateTransactionRequest, onSuccess: () -> Unit) {
        mutateTransaction(request = { api.transactions.update(id, request) }, onSuccess = onSuccess)
    }

    fun deleteTransaction(id: Int, onSuccess: () -> Unit) {
        mutateTransaction(request = { api.transactions.delete(id) }, onSuccess = onSuccess)
    }

    private fun mutateTransaction(request: suspend () -> ApiResult<*>, onSuccess: () -> Unit) {
        if (state.isMutating) return
        scope.launch {
            state = state.copy(isMutating = true)
            try {
                when (val result = request()) {
                    is ApiResult.Success -> {
                        onSuccess()
                        scope.launch { budgetNotifications.evaluateAfterLedgerUpdate() }
                        state.month?.let(::loadMonth)
                        loadOverview(overviewState.referenceMonth ?: YearMonth.now())
                    }

                    else -> showFailure(result)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                applicationContext.showAppToast(R.string.unknown_error)
            } finally {
                state = state.copy(isMutating = false)
            }
        }
    }

    private suspend fun loadAllTransactions(startDate: LocalDate, endDate: LocalDate): List<TransactionData>? {
        val transactions = mutableListOf<TransactionData>()
        var cursor: String? = null
        var succeeded = true
        do {
            when (
                val result =
                    api.transactions.list(
                        TransactionListQuery(
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            limit = TRANSACTION_PAGE_LIMIT,
                            cursor = cursor,
                        ),
                    )
            ) {
                is ApiResult.Success -> {
                    result.data?.let { page ->
                        transactions += page.items
                        cursor = page.nextCursor
                    } ?: run { cursor = null }
                }

                else -> {
                    showFailure(result)
                    succeeded = false
                    cursor = null
                }
            }
        } while (cursor != null)
        return transactions.takeIf { succeeded }
    }

    private fun showFailure(result: ApiResult<*>) {
        val message =
            when (result) {
                is ApiResult.NetworkFailure -> R.string.network_error

                is ApiResult.DecodeFailure -> R.string.response_error

                is ApiResult.HttpFailure,
                is ApiResult.NotImplemented,
                -> R.string.service_unavailable_error

                is ApiResult.Success -> return
            }
        applicationContext.showAppToast(message)
    }
}

internal fun groupTransactionsByMonth(transactions: List<TransactionData>): Map<YearMonth, List<TransactionData>> =
    transactions.groupBy { YearMonth.from(LocalDate.parse(it.date)) }
