package com.yu_morph.smartbudget.data.remote.api

import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.model.OcrResultData

class OcrApi {
    @Suppress("UNUSED_PARAMETER")
    suspend fun recognize(images: List<ByteArray>): ApiResult<List<OcrResultData>> = ApiResult.NotImplemented("영수증 OCR")
}
