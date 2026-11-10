package com.yu_morph.smartbudget.ui.main

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetCalendarTest {
    @Test
    fun `일요일에 시작하는 달은 첫 칸부터 날짜를 표시한다`() {
        val days = calendarDays(YearMonth.of(2026, 11))

        assertEquals(1, days.first()?.dayOfMonth)
    }

    @Test
    fun `월요일에 시작하는 달은 일요일 칸을 비운다`() {
        val days = calendarDays(YearMonth.of(2026, 6))

        assertNull(days.first())
        assertEquals(1, days[1]?.dayOfMonth)
    }

    @Test
    fun `달력 전체 칸 수는 항상 주 단위이다`() {
        val days = calendarDays(YearMonth.of(2026, 2))

        assertTrue(days.size % 7 == 0)
    }
}
