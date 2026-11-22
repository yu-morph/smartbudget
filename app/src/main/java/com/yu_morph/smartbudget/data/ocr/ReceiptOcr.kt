package com.yu_morph.smartbudget.data.ocr

import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.api.OcrApi
import com.yu_morph.smartbudget.data.remote.api.OcrUploadFile
import com.yu_morph.smartbudget.data.remote.model.OcrFileStatus
import com.yu_morph.smartbudget.data.remote.model.OcrResultData
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.delay

/**
 * 영수증 OCR 추출 결과.
 *
 * OpenAPI `POST /api/v1/proxy/ocr` 응답의 `store_name`, `items`, `date`, `time`, `amount` 필드에 대응한다.
 * 인식하지 못한 필드는 `null`(품목은 빈 목록)이다.
 */
data class ReceiptOcrResult(
    val storeName: String?,
    val date: LocalDate?,
    val time: LocalTime?,
    val amount: Int?,
    val items: List<String> = emptyList(),
)

/** 사용자에게 안내할 OCR 실패 유형. */
enum class ReceiptOcrFailureReason {
    /** 네트워크 연결 실패 또는 응답 시간 초과. */
    NETWORK,

    /** 로그인 토큰이 없거나 만료됨. */
    AUTHENTICATION,

    /** 서버가 영수증을 인식하지 못했거나 이미지가 요청 조건에 맞지 않음. */
    UNRECOGNIZED,

    /** 서버 오류, 중복 요청 충돌, 응답 해석 실패 등 그 밖의 오류. */
    SERVER,
}

/** OCR 실패를 나타내는 예외. 응답 본문이나 이미지 내용은 담지 않는다. */
class ReceiptOcrException(val reason: ReceiptOcrFailureReason) : Exception("영수증 OCR 실패: $reason")

/**
 * 영수증 이미지를 OCR 처리하는 외부 경계 인터페이스.
 *
 * OCR 결과는 사용자 확인용으로만 사용하며 거래를 자동 저장하지 않는다.
 * 실패하면 [ReceiptOcrException]을 담은 실패 결과를 반환한다.
 */
fun interface ReceiptOcrRepository {
    /** JPEG 형식의 영수증 이미지를 OCR 처리한다. */
    suspend fun recognize(jpegBytes: ByteArray): Result<ReceiptOcrResult>
}

/** OCR 프록시 API(`/api/v1/proxy/ocr`)를 호출하는 구현. */
class RemoteReceiptOcrRepository(private val ocrApi: OcrApi) : ReceiptOcrRepository {
    override suspend fun recognize(jpegBytes: ByteArray): Result<ReceiptOcrResult> {
        if (jpegBytes.isEmpty() || jpegBytes.size > OcrApi.MAX_FILE_BYTES) {
            return Result.failure(ReceiptOcrException(ReceiptOcrFailureReason.UNRECOGNIZED))
        }
        val file = OcrUploadFile(fileName = UPLOAD_FILE_NAME, contentType = JPEG_CONTENT_TYPE, bytes = jpegBytes)
        return ocrApi.recognize(listOf(file)).toReceiptOcrResult()
    }

    private companion object {
        const val UPLOAD_FILE_NAME = "receipt.jpg"
        const val JPEG_CONTENT_TYPE = "image/jpeg"
    }
}

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401

/** 단일 영수증 요청의 API 결과를 화면에서 사용할 결과로 변환한다. */
internal fun ApiResult<List<OcrResultData>>.toReceiptOcrResult(): Result<ReceiptOcrResult> = when (this) {
    is ApiResult.Success -> data?.firstOrNull()?.toReceiptOcrResult() ?: failure(ReceiptOcrFailureReason.SERVER)

    is ApiResult.HttpFailure ->
        when (status) {
            HTTP_UNAUTHORIZED -> failure(ReceiptOcrFailureReason.AUTHENTICATION)
            HTTP_BAD_REQUEST -> failure(ReceiptOcrFailureReason.UNRECOGNIZED)
            else -> failure(ReceiptOcrFailureReason.SERVER)
        }

    is ApiResult.NetworkFailure -> failure(ReceiptOcrFailureReason.NETWORK)

    is ApiResult.DecodeFailure, is ApiResult.NotImplemented -> failure(ReceiptOcrFailureReason.SERVER)
}

/** 파일별 OCR 결과를 변환한다. 형식이 맞지 않는 날짜·시각은 인식하지 못한 값으로 취급한다. */
internal fun OcrResultData.toReceiptOcrResult(): Result<ReceiptOcrResult> = when (status) {
    OcrFileStatus.FAILED -> failure(ReceiptOcrFailureReason.UNRECOGNIZED)

    OcrFileStatus.SUCCESS ->
        Result.success(
            ReceiptOcrResult(
                storeName = storeName?.takeIf(String::isNotBlank),
                date = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                time = time?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
                amount = amount?.takeIf { it > 0 },
                items = items.map(String::trim).filter(String::isNotEmpty),
            ),
        )
}

private fun failure(reason: ReceiptOcrFailureReason): Result<ReceiptOcrResult> =
    Result.failure(ReceiptOcrException(reason))

/**
 * 고정 더미 결과를 반환하는 구현. 미리보기와 테스트에서 사용한다.
 *
 * 전달받은 이미지는 전송하거나 저장하지 않고 항상 [DUMMY_RESULT]를 반환한다.
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
                items = listOf("삼각김밥", "바나나우유"),
            )
    }
}
