package com.yu_morph.smartbudget.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** OCR 파일별 처리 결과. */
@Serializable
enum class OcrFileStatus {
    @SerialName("success")
    SUCCESS,

    @SerialName("failed")
    FAILED,
}

/**
 * `POST /api/v1/proxy/ocr` 응답의 파일별 OCR 결과.
 *
 * 실패한 파일은 인식 필드가 모두 `null`이고 [items]는 빈 배열이다.
 */
@Serializable
data class OcrResultData(
    @SerialName("file_name") val fileName: String,
    val status: OcrFileStatus,
    @SerialName("error_message") val errorMessage: String? = null,
    val items: List<String> = emptyList(),
    @SerialName("file_index") val fileIndex: Int,
    @SerialName("store_name") val storeName: String? = null,
    val date: String? = null,
    val time: String? = null,
    val amount: Int? = null,
)
