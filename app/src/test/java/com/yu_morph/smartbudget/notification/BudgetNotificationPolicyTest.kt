package com.yu_morph.smartbudget.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetNotificationPolicyTest {
    @Test
    fun `예산 사용률이 80퍼센트를 초과하면 임박 알림을 선택한다`() {
        assertNull(budgetAlertType(1_000, 800, exceededAlreadyNotified = false))
        assertEquals(BudgetAlertType.APPROACHING, budgetAlertType(1_000, 801, false))
        assertEquals(BudgetAlertType.APPROACHING, budgetAlertType(1_000, 1_000, false))
    }

    @Test
    fun `예산을 초과하면 월별 최초 한 번만 초과 알림을 선택한다`() {
        assertEquals(BudgetAlertType.EXCEEDED, budgetAlertType(1_000, 1_001, false))
        assertNull(budgetAlertType(1_000, 1_001, exceededAlreadyNotified = true))
    }

    @Test
    fun `예산이 설정되지 않으면 알림을 선택하지 않는다`() {
        assertNull(budgetAlertType(null, 900, false))
        assertNull(budgetAlertType(0, 900, false))
    }
}
