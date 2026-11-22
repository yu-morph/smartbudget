package com.yu_morph.smartbudget.data.ocr

import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.ResponseSource
import com.yu_morph.smartbudget.data.remote.model.OcrFileStatus
import com.yu_morph.smartbudget.data.remote.model.OcrResultData
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteReceiptOcrMappingTest {
    private fun success(data: List<OcrResultData>?) = ApiResult.Success(
        data = data,
        status = 200,
        code = "SUCCESS",
        message = "The request has been accepted and processed.",
        source = ResponseSource.NETWORK,
    )

    private val recognized =
        OcrResultData(
            fileName = "receipt.jpg",
            status = OcrFileStatus.SUCCESS,
            items = listOf("광어회"),
            fileIndex = 0,
            storeName = "대박수산",
            date = "2026-09-12",
            time = "16:50:11",
            amount = 40_500,
        )

    private fun Result<ReceiptOcrResult>.failureReason() = (exceptionOrNull() as ReceiptOcrException).reason

    @Test
    fun `성공한 파일 결과를 화면용 결과로 변환한다`() {
        val result = success(listOf(recognized)).toReceiptOcrResult().getOrThrow()

        assertEquals("대박수산", result.storeName)
        assertEquals(LocalDate.of(2026, 9, 12), result.date)
        assertEquals(LocalTime.of(16, 50, 11), result.time)
        assertEquals(40_500, result.amount)
        assertEquals(listOf("광어회"), result.items)
    }

    @Test
    fun `품목의 앞뒤 공백을 지우고 빈 품목은 제외한다`() {
        val result =
            success(listOf(recognized.copy(items = listOf(" 상차림비 ", "", "  ", "매운탕추가"))))
                .toReceiptOcrResult()
                .getOrThrow()

        assertEquals(listOf("상차림비", "매운탕추가"), result.items)
    }

    @Test
    fun `형식이 잘못된 날짜와 시각 및 빈 상호는 인식하지 못한 값으로 처리한다`() {
        val result =
            success(listOf(recognized.copy(storeName = " ", date = "2026/09/12", time = "25:00:00", amount = null)))
                .toReceiptOcrResult()
                .getOrThrow()

        assertNull(result.storeName)
        assertNull(result.date)
        assertNull(result.time)
        assertNull(result.amount)
    }

    @Test
    fun `실패한 파일은 영수증 인식 실패로 변환한다`() {
        val failed =
            OcrResultData(
                fileName = "receipt.jpg",
                status = OcrFileStatus.FAILED,
                errorMessage = "영수증 내용을 인식할 수 없습니다.",
                fileIndex = 0,
            )

        assertEquals(ReceiptOcrFailureReason.UNRECOGNIZED, success(listOf(failed)).toReceiptOcrResult().failureReason())
    }

    @Test
    fun `비어 있는 성공 응답은 서버 오류로 변환한다`() {
        assertEquals(ReceiptOcrFailureReason.SERVER, success(emptyList()).toReceiptOcrResult().failureReason())
        assertEquals(ReceiptOcrFailureReason.SERVER, success(null).toReceiptOcrResult().failureReason())
    }

    @Test
    fun `HTTP 오류 상태를 실패 유형으로 변환한다`() {
        fun http(status: Int, code: String) = ApiResult.HttpFailure(status, code, "message", null).toReceiptOcrResult()

        assertEquals(ReceiptOcrFailureReason.AUTHENTICATION, http(401, "AUTHENTICATION_REQUIRED").failureReason())
        assertEquals(ReceiptOcrFailureReason.UNRECOGNIZED, http(400, "INVALID_REQUEST").failureReason())
        assertEquals(ReceiptOcrFailureReason.SERVER, http(409, "IDEMPOTENCY_KEY_CONFLICT").failureReason())
        assertEquals(ReceiptOcrFailureReason.SERVER, http(500, "INTERNAL_ERROR").failureReason())
    }

    @Test
    fun `네트워크 실패와 응답 해석 실패를 구분한다`() {
        assertEquals(
            ReceiptOcrFailureReason.NETWORK,
            ApiResult.NetworkFailure(IOException("timeout")).toReceiptOcrResult().failureReason(),
        )
        assertEquals(
            ReceiptOcrFailureReason.SERVER,
            ApiResult.DecodeFailure("{}", IllegalArgumentException()).toReceiptOcrResult().failureReason(),
        )
    }
}
