package com.yu_morph.smartbudget.ui.main

import com.yu_morph.smartbudget.data.remote.model.TransactionData
import com.yu_morph.smartbudget.data.remote.model.TransactionSource
import com.yu_morph.smartbudget.data.remote.model.TransactionType
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetBookOverviewTest {
    @Test
    fun `거래 내역을 년월별로 분류한다`() {
        val transactions =
            listOf(
                transaction(id = 1, date = "2026-10-31", type = TransactionType.EXPENSE, amount = 10_000),
                transaction(id = 2, date = "2026-11-01", type = TransactionType.INCOME, amount = 20_000),
            )

        val grouped = groupTransactionsByMonth(transactions)

        assertEquals(listOf(1), grouped.getValue(YearMonth.of(2026, 10)).map(TransactionData::id))
        assertEquals(listOf(2), grouped.getValue(YearMonth.of(2026, 11)).map(TransactionData::id))
    }

    @Test
    fun `월별 수입과 지출을 각각 합산한다`() {
        val transactions =
            listOf(
                transaction(id = 1, type = TransactionType.INCOME, amount = 3_000_000),
                transaction(id = 2, type = TransactionType.INCOME, amount = 200_000),
                transaction(id = 3, type = TransactionType.EXPENSE, amount = 1_100_000),
            )

        assertEquals(
            MonthlyTransactionAmounts(income = 3_200_000, expense = 1_100_000),
            monthlyTransactionAmounts(transactions),
        )
    }

    @Test
    fun `이전 월 통계는 지출 금액이 큰 카테고리를 순위별로 집계한다`() {
        val transactions =
            listOf(
                transaction(id = 1, type = TransactionType.INCOME, amount = 100_000),
                transaction(id = 2, type = TransactionType.EXPENSE, category = "food", amount = 10_000),
                transaction(id = 3, type = TransactionType.EXPENSE, category = "food", amount = 20_000),
                transaction(id = 4, type = TransactionType.EXPENSE, category = "transport", amount = 50_000),
                transaction(id = 5, type = TransactionType.EXPENSE, category = "shopping", amount = 5_000),
            )
        val previous =
            listOf(transaction(id = 6, type = TransactionType.EXPENSE, category = "food", amount = 60_000))

        val stats = monthlyLedgerStats(transactions, previous)

        assertEquals(85, stats.expenseRatioPercent)
        assertEquals(25_000, stats.expenseChange)
        assertEquals(CategoryAmountStat("transport", 50_000), stats.largestExpenseCategories.first())
    }

    private fun transaction(
        id: Int,
        date: String = "2026-11-01",
        type: TransactionType,
        category: String = if (type == TransactionType.INCOME) "salary" else "food",
        amount: Int,
    ) = TransactionData(
        id = id,
        date = date,
        type = type,
        source = TransactionSource.MANUAL,
        category = category,
        amount = amount,
        createdAt = "2026-11-01T00:00:00+09:00",
        updatedAt = "2026-11-01T00:00:00+09:00",
    )
}
