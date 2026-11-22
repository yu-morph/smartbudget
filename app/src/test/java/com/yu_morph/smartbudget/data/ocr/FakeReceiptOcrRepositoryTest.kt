package com.yu_morph.smartbudget.data.ocr

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeReceiptOcrRepositoryTest {
    @Test
    fun `더미 OCR은 입력 이미지와 관계없이 고정 결과를 반환한다`() = runBlocking {
        val repository = FakeReceiptOcrRepository(delayMillis = 0L)

        val first = repository.recognize(byteArrayOf(1, 2, 3)).getOrThrow()
        val second = repository.recognize(ByteArray(0)).getOrThrow()

        assertEquals(FakeReceiptOcrRepository.DUMMY_RESULT, first)
        assertEquals(first, second)
    }

    @Test
    fun `더미 OCR 결과는 거래 필드 형식에 맞는 값을 가진다`() {
        val result = FakeReceiptOcrRepository.DUMMY_RESULT

        assertEquals("2026-11-21", result.date.toString())
        assertEquals("14:32", result.time.toString())
        assertEquals(12_500, result.amount)
    }
}
