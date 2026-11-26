package com.yu_morph.smartbudget.ui.main

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.SmartBudgetApi
import com.yu_morph.smartbudget.data.remote.api.TransactionListQuery
import com.yu_morph.smartbudget.data.remote.model.ReplaceMonthlyBudgetRequest
import com.yu_morph.smartbudget.data.remote.model.TransactionData
import com.yu_morph.smartbudget.data.remote.model.TransactionType
import com.yu_morph.smartbudget.ui.showAppToast
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

private const val HOME_TRANSACTION_PAGE_LIMIT = 100

data class HomeDashboardUiState(
    val month: YearMonth = YearMonth.now(),
    val income: Int = 0,
    val expense: Int = 0,
    val budget: Int = DEFAULT_MONTHLY_BUDGET,
    val spendingTemperature: Double = 36.5,
    val isLoading: Boolean = false,
    val recentExpense: TransactionData? = null,
)

data class HomeMonthlyAmounts(val income: Int, val expense: Int)

class HomeDashboardCoordinator(context: Context, private val api: SmartBudgetApi, private val scope: CoroutineScope) {
    private val applicationContext = context.applicationContext
    private var loadJob: Job? = null

    var state by mutableStateOf(HomeDashboardUiState())
        private set

    fun load(month: YearMonth = YearMonth.now()) {
        loadJob?.cancel()
        loadJob = scope.launch {
            state = state.copy(
                month = month,
                isLoading = true,
                recentExpense = state.recentExpense.takeIf { month == state.month },
            )
            try {
                val (transactions, budget) = supervisorScope {
                    val transactionRequest = async { loadAllTransactions(month) }
                    val budgetRequest = async { loadBudget(month) }
                    transactionRequest.await() to budgetRequest.await()
                }
                val amounts = transactions?.let(::summarizeHomeTransactions)
                val resolvedBudget = budget ?: state.budget
                state =
                    state.copy(
                        income = amounts?.income ?: state.income,
                        expense = amounts?.expense ?: state.expense,
                        budget = resolvedBudget,
                        recentExpense = if (transactions ==
                            null
                        ) {
                            state.recentExpense
                        } else {
                            recentHomeExpense(transactions)
                        },
                        spendingTemperature =
                            transactions?.let {
                                calculateSpendingTemperature(
                                    transactions = it,
                                    budget = resolvedBudget,
                                    month = month,
                                )
                            } ?: state.spendingTemperature,
                    )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                applicationContext.showAppToast(R.string.unknown_error)
            } finally {
                if (state.month == month) state = state.copy(isLoading = false)
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun loadAllTransactions(month: YearMonth): List<TransactionData>? {
        val transactions = mutableListOf<TransactionData>()
        var cursor: String? = null
        do {
            when (
                val result =
                    api.transactions.list(
                        TransactionListQuery(
                            startDate = month.atDay(1).toString(),
                            endDate = month.atEndOfMonth().toString(),
                            limit = HOME_TRANSACTION_PAGE_LIMIT,
                            cursor = cursor,
                        ),
                    )
            ) {
                is ApiResult.Success -> {
                    val page = result.data ?: return transactions
                    transactions += page.items
                    cursor = page.nextCursor
                }

                else -> {
                    showFailure(result)
                    return null
                }
            }
        } while (cursor != null)
        return transactions
    }

    @Suppress("ReturnCount")
    private suspend fun loadBudget(month: YearMonth): Int? {
        when (val currentResult = api.monthlyBudgets.get(month.toString())) {
            is ApiResult.Success -> currentResult.data?.amount?.let { return it }

            else -> {
                showFailure(currentResult)
                return null
            }
        }
        val previousAmount =
            when (val previousResult = api.monthlyBudgets.get(month.minusMonths(1).toString())) {
                is ApiResult.Success -> previousResult.data?.amount

                else -> {
                    showFailure(previousResult)
                    return null
                }
            }
        val amount = resolveMonthlyBudget(currentAmount = null, previousAmount = previousAmount)
        val replaceResult = api.monthlyBudgets.replace(month.toString(), ReplaceMonthlyBudgetRequest(amount))
        if (replaceResult !is ApiResult.Success) showFailure(replaceResult)
        return amount
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

internal fun summarizeHomeTransactions(transactions: List<TransactionData>): HomeMonthlyAmounts =
    transactions.fold(HomeMonthlyAmounts(income = 0, expense = 0)) { summary, transaction ->
        when (transaction.type) {
            TransactionType.INCOME -> summary.copy(income = summary.income + transaction.amount)
            TransactionType.EXPENSE -> summary.copy(expense = summary.expense + transaction.amount)
        }
    }

internal fun recentHomeExpense(transactions: List<TransactionData>): TransactionData? =
    transactions.asSequence().filter { it.type == TransactionType.EXPENSE }
        .maxWithOrNull(compareBy<TransactionData> { it.date }.thenBy { it.time.orEmpty() }.thenBy { it.id })
