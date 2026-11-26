package com.yu_morph.smartbudget.ui.main

import androidx.compose.ui.graphics.Color
import com.yu_morph.smartbudget.data.remote.model.TransactionData
import com.yu_morph.smartbudget.data.remote.model.TransactionSource
import com.yu_morph.smartbudget.data.remote.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDashboardCoordinatorTest {
    @Test
    fun `최근 지출은 수입을 제외하고 거래 날짜와 시각으로 선택한다`() {
        val old = transaction(90, TransactionType.EXPENSE, 1000, date = "2026-11-16")
        val latest = transaction(2, TransactionType.EXPENSE, 2000).copy(time = "20:00:00")
        val morning = transaction(3, TransactionType.EXPENSE, 3000).copy(time = "08:00:00")
        val income = transaction(4, TransactionType.INCOME, 4000, date = "2026-11-18")

        assertEquals(latest, recentHomeExpense(listOf(old, latest, morning, income)))
        assertEquals(null, recentHomeExpense(listOf(income)))
    }

    @Test
    fun `수입과 지출을 각각 합산한다`() {
        val transactions =
            listOf(
                transaction(id = 1, type = TransactionType.INCOME, amount = 3_000_000),
                transaction(id = 2, type = TransactionType.EXPENSE, amount = 450_000),
                transaction(id = 3, type = TransactionType.EXPENSE, amount = 50_000),
            )

        assertEquals(HomeMonthlyAmounts(income = 3_000_000, expense = 500_000), summarizeHomeTransactions(transactions))
    }

    @Test
    fun `예산 사용률 구간에 맞는 색상을 반환한다`() {
        assertEquals(Color(0xFF17A673), budgetUsageColor(0.5f))
        assertEquals(Color(0xFFF2C94C), budgetUsageColor(0.51f))
        assertEquals(Color(0xFFF2994A), budgetUsageColor(0.71f))
        assertEquals(Color(0xFFEB5757), budgetUsageColor(0.91f))
        assertEquals(Color(0xFFEB5757), budgetUsageColor(1.2f))
    }

    @Test
    fun `예산 초과와 불필요 카테고리 소비가 많으면 소비 온도가 낮아진다`() {
        val month = YearMonth.of(2026, 11)
        val healthy =
            calculateSpendingTemperature(
                transactions =
                    listOf(
                        transaction(
                            id = 1,
                            type = TransactionType.EXPENSE,
                            amount = 90_000,
                            category = "housing_utilities",
                            date = "2026-11-03",
                        ),
                    ),
                budget = 1_000_000,
                month = month,
                today = LocalDate.of(2026, 11, 15),
            )
        val unhealthy =
            calculateSpendingTemperature(
                transactions =
                    listOf(
                        transaction(2, TransactionType.EXPENSE, 600_000, "shopping", "2026-11-01"),
                        transaction(3, TransactionType.EXPENSE, 700_000, "leisure", "2026-11-02"),
                        transaction(4, TransactionType.EXPENSE, 500_000, "other_expense", "2026-11-03"),
                    ),
                budget = 1_000_000,
                month = month,
                today = LocalDate.of(2026, 11, 15),
            )

        assertEquals(SpendingTemperatureLevel.GOOD, spendingTemperatureLevel(healthy))
        assertEquals(SpendingTemperatureLevel.BAD, spendingTemperatureLevel(unhealthy))
    }

    @Test
    fun `소비 온도 경계값을 상태로 변환한다`() {
        assertEquals(SpendingTemperatureLevel.BAD, spendingTemperatureLevel(32.0))
        assertEquals(SpendingTemperatureLevel.NOT_GOOD, spendingTemperatureLevel(32.1))
        assertEquals(SpendingTemperatureLevel.OKAY, spendingTemperatureLevel(35.1))
        assertEquals(SpendingTemperatureLevel.GOOD, spendingTemperatureLevel(37.1))
        assertEquals(SpendingTemperatureLevel.AMAZING, spendingTemperatureLevel(39.1))
    }

    @Test
    fun `소비 온도 표시 바는 정상 체온을 중앙에 배치한다`() {
        assertEquals(0f, spendingTemperaturePosition(28.0), 0.0001f)
        assertEquals(0.5f, spendingTemperaturePosition(36.5), 0.0001f)
        assertEquals(1f, spendingTemperaturePosition(42.0), 0.0001f)
    }

    private fun transaction(
        id: Int,
        type: TransactionType,
        amount: Int,
        category: String = "food",
        date: String = "2026-11-17",
    ) = TransactionData(
        id = id,
        date = date,
        type = type,
        source = TransactionSource.MANUAL,
        category = category,
        amount = amount,
        createdAt = "2026-11-17T10:00:00+09:00",
        updatedAt = "2026-11-17T10:00:00+09:00",
    )
}
