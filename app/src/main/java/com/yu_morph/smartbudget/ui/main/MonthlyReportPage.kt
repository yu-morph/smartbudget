package com.yu_morph.smartbudget.ui.main

import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.api.ReportListQuery
import com.yu_morph.smartbudget.data.remote.model.ReportPageData
import com.yu_morph.smartbudget.data.remote.model.ReportSummaryData

private const val REPORTS_PER_PAGE = 3

internal data class MonthlyReportPage(
    val visible: List<ReportSummaryData> = emptyList(),
    val buffered: List<ReportSummaryData> = emptyList(),
    val cursor: String? = null,
    val exhausted: Boolean = false,
) {
    suspend fun load(fetch: suspend (ReportListQuery) -> ApiResult<ReportPageData>): MonthlyReportPage {
        val reports = buffered.toMutableList()
        val lookaheadLimit = REPORTS_PER_PAGE + 1
        var next = cursor
        var done = exhausted
        while (!done && reports.size < lookaheadLimit) {
            val result = fetch(ReportListQuery(limit = lookaheadLimit - reports.size, cursor = next))
            if (result !is ApiResult.Success) throw ReportHistoryException(result.toAnalysisFailure())
            reports += result.data?.items.orEmpty().filter { it.periodType == "monthly" }
            val following = result.data?.nextCursor
            done = following == null || following == next
            next = following
        }
        return MonthlyReportPage(reports.take(REPORTS_PER_PAGE), reports.drop(REPORTS_PER_PAGE), next, done)
    }
}

internal class ReportHistoryException(val failure: AnalysisFailure) : Exception()

internal fun reportGoalKey(username: String, reportId: Int, generatedAt: String): String =
    "$reportId:$generatedAt:$username"
