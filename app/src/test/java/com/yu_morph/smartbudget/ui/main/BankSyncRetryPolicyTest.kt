package com.yu_morph.smartbudget.ui.main

import com.yu_morph.smartbudget.data.remote.ApiResult
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BankSyncRetryPolicyTest {
    @Test
    fun `재연동 중 일시적인 실패는 재시도한다`() {
        assertTrue(ApiResult.NetworkFailure(IOException()).isTransientBankSyncFailure())
        assertTrue(ApiResult.DecodeFailure("", IllegalArgumentException()).isTransientBankSyncFailure())
        assertTrue(httpFailure(503).isTransientBankSyncFailure())
        assertTrue(httpFailure(409, "IDEMPOTENCY_REQUEST_IN_PROGRESS").isTransientBankSyncFailure())
    }

    @Test
    fun `입력 오류와 인증 오류는 재시도하지 않는다`() {
        assertFalse(httpFailure(400).isTransientBankSyncFailure())
        assertFalse(httpFailure(401).isTransientBankSyncFailure())
    }

    @Test
    fun `은행 연동 성공 화면은 시작 후 최소 5초가 지난 뒤 종료한다`() {
        assertEquals(5_000L, remainingBankSyncLoadingMillis(elapsedMillis = 0L))
        assertEquals(3_750L, remainingBankSyncLoadingMillis(elapsedMillis = 1_250L))
        assertEquals(0L, remainingBankSyncLoadingMillis(elapsedMillis = 5_000L))
        assertEquals(0L, remainingBankSyncLoadingMillis(elapsedMillis = 7_000L))
    }

    private fun httpFailure(status: Int, code: String = "ERROR") =
        ApiResult.HttpFailure(status, code, "error", retryAfterSeconds = null)
}
