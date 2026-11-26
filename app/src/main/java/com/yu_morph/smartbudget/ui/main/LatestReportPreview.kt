package com.yu_morph.smartbudget.ui.main

import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.model.ReportDetailData

internal sealed interface LatestReportPreview {
    data object Loading : LatestReportPreview
    data class Ready(val summary: String?) : LatestReportPreview
    data class Failed(val failure: AnalysisFailure) : LatestReportPreview
}

internal fun ApiResult<ReportDetailData>.toLatestReportPreview(expectedId: Int): LatestReportPreview = when (this) {
    is ApiResult.Success -> {
        val report = data
        if (report == null || report.id != expectedId) {
            LatestReportPreview.Failed(AnalysisFailure.RESPONSE)
        } else {
            LatestReportPreview.Ready(AnalysisReportParser.parse(report.fullReport)?.aWordFromAi)
        }
    }

    else -> LatestReportPreview.Failed(toAnalysisFailure())
}
