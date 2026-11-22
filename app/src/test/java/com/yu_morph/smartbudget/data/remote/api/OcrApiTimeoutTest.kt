package com.yu_morph.smartbudget.data.remote.api

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrApiTimeoutTest {
    @Test
    fun `파일 수에 따라 서버 동시 처리 횟수만큼 대기 시간을 늘린다`() {
        assertEquals(90_000L, OcrApi.requestTimeoutMillis(1))
        assertEquals(90_000L, OcrApi.requestTimeoutMillis(2))
        assertEquals(150_000L, OcrApi.requestTimeoutMillis(3))
        assertEquals(330_000L, OcrApi.requestTimeoutMillis(10))
    }
}
