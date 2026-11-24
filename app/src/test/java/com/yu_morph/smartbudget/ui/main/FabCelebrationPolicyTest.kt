package com.yu_morph.smartbudget.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FabCelebrationPolicyTest {
    @Test
    fun `실행 중인 축하 애니메이션은 다시 시작하지 않는다`() {
        assertTrue(canStartFabCelebration(isCelebrating = false))
        assertFalse(canStartFabCelebration(isCelebrating = true))
    }

    @Test
    fun `로봇은 내비게이션 중앙에서 올라갔다 같은 위치로 돌아온다`() {
        assertEquals(112f, fabCelebrationVerticalOffset(0f), 0.001f)
        assertEquals(-110f, fabCelebrationVerticalOffset(0.5f), 0.001f)
        assertEquals(112f, fabCelebrationVerticalOffset(1f), 0.001f)
    }

    @Test
    fun `confetti는 중앙을 기준으로 좌우 대칭 속도를 가진다`() {
        val velocities = confettiHorizontalVelocities()

        assertEquals(32, velocities.size)
        velocities.chunked(2).forEach { pair ->
            assertEquals(0f, pair.sum(), 0.001f)
        }
    }
}
