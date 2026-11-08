package com.yu_morph.smartbudget.ui.main

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExitConfirmationTest {
    @Test
    fun `제한 시간 안에 뒤로가기를 두 번 누르면 종료한다`() {
        val confirmation = ExitConfirmation(confirmationWindowMillis = 2_000L)

        assertFalse(confirmation.shouldExit(1_000L))
        assertTrue(confirmation.shouldExit(2_999L))
    }

    @Test
    fun `제한 시간이 지나면 다시 확인을 시작한다`() {
        val confirmation = ExitConfirmation(confirmationWindowMillis = 2_000L)

        assertFalse(confirmation.shouldExit(1_000L))
        assertFalse(confirmation.shouldExit(3_001L))
        assertTrue(confirmation.shouldExit(4_000L))
    }
}
