package com.yu_morph.smartbudget.ui.main

import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.api.ReportListQuery
import com.yu_morph.smartbudget.data.remote.model.ReportPageData
import com.yu_morph.smartbudget.data.remote.model.ReportSummaryData

private const val REPORTS_PER_PAGE = 3
private const val REPORT_QUERY_LIMIT = 100

internal data class MonthlyReportPage(
    val visible: List<ReportSummaryData> = emptyList(),
    val buffered: List<ReportSummaryData> = emptyList(),
    val exhausted: Boolean = false,
) {
    suspend fun load(fetch: suspend (ReportListQuery) -> ApiResult<ReportPageData>): MonthlyReportPage {
        val reports = buffered.toMutableList()
        var next: String? = null
        val seenCursors = mutableSetOf<String>()
        var done = exhausted
        while (!done) {
            val result = fetch(ReportListQuery(limit = REPORT_QUERY_LIMIT, cursor = next))
            if (result !is ApiResult.Success) throw ReportHistoryException(result.toAnalysisFailure())
            reports += result.data?.items.orEmpty().filter { it.periodType == "monthly" }
            val following = result.data?.nextCursor
            if (following != null && !seenCursors.add(following)) throw ReportHistoryException(AnalysisFailure.RESPONSE)
            done = following == null
            next = following
        }
        val unique = reports.distinctBy { it.id }
        return MonthlyReportPage(unique.take(REPORTS_PER_PAGE), unique.drop(REPORTS_PER_PAGE), exhausted = true)
    }
}

internal class ReportHistoryException(val failure: AnalysisFailure) : Exception()

internal fun reportGoalKey(username: String, reportId: Int, generatedAt: String): String =
    "$reportId:$generatedAt:$username"
