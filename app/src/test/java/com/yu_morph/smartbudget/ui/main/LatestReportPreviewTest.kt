package com.yu_morph.smartbudget.ui.main

import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.ResponseSource
import com.yu_morph.smartbudget.data.remote.model.ReportDetailData
import org.junit.Assert.assertEquals
import org.junit.Test

class LatestReportPreviewTest {
    @Test
    fun readsOnlyAiSummaryFromRequestedReport() {
        val response = result(
            "<report><a_word_from_ai>요약입니다.</a_word_from_ai><spending_health>건전성입니다.</spending_health></report>",
        )

        assertEquals(LatestReportPreview.Ready("요약입니다."), response.toLatestReportPreview(expectedId = 42))
        assertEquals(
            LatestReportPreview.Failed(AnalysisFailure.RESPONSE),
            response.toLatestReportPreview(expectedId = 41),
        )
    }

    @Test
    fun reportWithoutAiSummaryHasEmptyPreview() {
        val response = result("<report><spending_health>건전성입니다.</spending_health></report>")

        assertEquals(LatestReportPreview.Ready(null), response.toLatestReportPreview(expectedId = 42))
    }

    @Test
    fun authenticationFailureIsNotRenderedAsReportContent() {
        val response = ApiResult.HttpFailure(401, "AUTHENTICATION_REQUIRED", "", null)

        assertEquals(
            LatestReportPreview.Failed(AnalysisFailure.AUTHENTICATION),
            response.toLatestReportPreview(expectedId = 42),
        )
    }

    private fun result(xml: String): ApiResult<ReportDetailData> = ApiResult.Success(
        ReportDetailData(42, "2026-11-26T11:00:00+09:00", "monthly", "2026-11-01", "2026-11-30", false, xml),
        200,
        "SUCCESS",
        "",
        ResponseSource.NETWORK,
    )
}
