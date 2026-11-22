package com.yu_morph.smartbudget.data.ocr

import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.delay

/**
 * 영수증 OCR 추출 결과.
 *
 * OpenAPI `POST /api/v1/proxy/ocr` 응답의 `store_name`, `date`, `time`, `amount` 필드에 대응한다.
 * 인식하지 못한 필드는 `null`이다.
 */
data class ReceiptOcrResult(val storeName: String?, val date: LocalDate?, val time: LocalTime?, val amount: Int?)

/**
 * 영수증 이미지를 OCR 처리하는 외부 경계 인터페이스.
 *
 * OCR 결과는 사용자 확인용으로만 사용하며 거래를 자동 저장하지 않는다.
 */
fun interface ReceiptOcrRepository {
    /** JPEG 형식의 영수증 이미지를 OCR 처리한다. */
    suspend fun recognize(jpegBytes: ByteArray): Result<ReceiptOcrResult>
}

/**
 * OCR API 구현 전까지 사용하는 고정 더미 구현.
 *
 * 전달받은 이미지는 전송하거나 저장하지 않고 항상 [DUMMY_RESULT]를 반환한다.
 * 실제 API가 준비되면 `/api/v1/proxy/ocr`을 호출하는 구현으로 교체한다.
 */
class FakeReceiptOcrRepository(private val delayMillis: Long = DEFAULT_DELAY_MILLIS) : ReceiptOcrRepository {
    override suspend fun recognize(jpegBytes: ByteArray): Result<ReceiptOcrResult> {
        // 실제 네트워크 요청처럼 로딩 상태를 확인할 수 있도록 잠시 대기한다.
        delay(delayMillis)
        return Result.success(DUMMY_RESULT)
    }

    companion object {
        private const val DEFAULT_DELAY_MILLIS = 1_500L

        /** 임시로 사용하는 고정 OCR 결과. */
        val DUMMY_RESULT =
            ReceiptOcrResult(
                storeName = "GS25 영남대점",
                date = LocalDate.of(2026, 11, 21),
                time = LocalTime.of(14, 32, 0),
                amount = 12_500,
            )
    }
}
