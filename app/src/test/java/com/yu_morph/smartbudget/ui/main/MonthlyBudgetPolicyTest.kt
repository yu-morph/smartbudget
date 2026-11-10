package com.yu_morph.smartbudget.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class MonthlyBudgetPolicyTest {
    @Test
    fun `이번 달 예산이 있으면 해당 값을 사용한다`() {
        assertEquals(800_000, resolveMonthlyBudget(800_000, 700_000))
    }

    @Test
    fun `이번 달 예산이 없으면 이전 달 값을 사용한다`() {
        assertEquals(700_000, resolveMonthlyBudget(null, 700_000))
    }

    @Test
    fun `이번 달과 이전 달 예산이 없으면 기본값을 사용한다`() {
        assertEquals(DEFAULT_MONTHLY_BUDGET, resolveMonthlyBudget(null, null))
    }
}
