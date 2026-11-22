package com.yu_morph.smartbudget.data.remote.api

import com.yu_morph.smartbudget.data.remote.ApiRequestExecutor
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.model.OcrResultData
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod

/** OCR 요청에 첨부할 영수증 파일. */
class OcrUploadFile(val fileName: String, val contentType: String, val bytes: ByteArray)

/**
 * 영수증 OCR 프록시 API.
 *
 * multipart `files` 필드로 영수증을 전송한다. 요청은 멱등성이 보장되지 않으므로 자동으로 재시도하지 않는다.
 */
class OcrApi(private val executor: ApiRequestExecutor) {
    suspend fun recognize(files: List<OcrUploadFile>, idempotencyKey: String? = null): ApiResult<List<OcrResultData>> {
        require(files.size in 1..MAX_FILE_COUNT) { "영수증 파일은 1~$MAX_FILE_COUNT 개만 전송할 수 있습니다." }
        require(files.all { it.bytes.size in 1..MAX_FILE_BYTES }) { "영수증 파일 크기가 허용 범위를 벗어났습니다." }
        val timeoutMillis = requestTimeoutMillis(files.size)
        return executor.execute<List<OcrResultData>>(
            method = HttpMethod.Post,
            path = "/api/v1/proxy/ocr",
            authenticated = true,
        ) {
            // 공통 실행기가 지정한 JSON Content-Type 대신 multipart 본문의 Content-Type을 사용한다.
            headers.remove(HttpHeaders.ContentType)
            setBody(MultiPartFormDataContent(files.toFormData()))
            idempotencyKey?.let { header("Idempotency-Key", it) }
            timeout {
                requestTimeoutMillis = timeoutMillis
                socketTimeoutMillis = timeoutMillis
            }
        }
    }

    private fun List<OcrUploadFile>.toFormData() = formData {
        forEach { file -> append("files", file.bytes, file.partHeaders()) }
    }

    private fun OcrUploadFile.partHeaders(): Headers = Headers.build {
        append(HttpHeaders.ContentType, contentType)
        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
    }

    companion object {
        const val MAX_FILE_COUNT = 10
        const val MAX_FILE_BYTES = 10_000_000

        /** 서버는 파일별 OCR 호출에 60초 제한을 두고 프로세스당 최대 2개를 동시에 처리한다. */
        private const val PER_FILE_OCR_LIMIT_MILLIS = 60_000L
        private const val SERVER_PARALLEL_CALLS = 2
        private const val UPLOAD_MARGIN_MILLIS = 30_000L

        internal fun requestTimeoutMillis(fileCount: Int): Long {
            val rounds = (fileCount + SERVER_PARALLEL_CALLS - 1) / SERVER_PARALLEL_CALLS
            return rounds * PER_FILE_OCR_LIMIT_MILLIS + UPLOAD_MARGIN_MILLIS
        }
    }
}
