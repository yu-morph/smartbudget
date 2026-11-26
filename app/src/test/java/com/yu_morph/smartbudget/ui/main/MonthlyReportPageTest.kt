package com.yu_morph.smartbudget.ui.main

import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.ResponseSource
import com.yu_morph.smartbudget.data.remote.model.ReportPageData
import com.yu_morph.smartbudget.data.remote.model.ReportSummaryData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyReportPageTest {
    @Test
    fun showsThreeMonthlyReportsPerPageAndKeepsOneAsLookahead() = runBlocking {
        val source = (1..8).map { report(it, if (it == 2) "daily" else "monthly") }
        val first = MonthlyReportPage().load { page(source, it.cursor, requireNotNull(it.limit)) }
        val second = first.load { page(source, it.cursor, requireNotNull(it.limit)) }
        val third = second.load { page(source, it.cursor, requireNotNull(it.limit)) }

        assertEquals(listOf(1, 3, 4), first.visible.map { it.id })
        assertEquals(listOf(5, 6, 7), second.visible.map { it.id })
        assertEquals(listOf(8), third.visible.map { it.id })
        assertTrue(third.buffered.isEmpty())
    }

    @Test
    fun exactlyThreeMonthlyReportsHaveNoNextPage() = runBlocking {
        val source = listOf(report(1), report(2), report(3), report(4, "daily"))
        val page = MonthlyReportPage().load { page(source, it.cursor, requireNotNull(it.limit)) }
        assertEquals(3, page.visible.size)
        assertTrue(page.buffered.isEmpty())
    }

    private fun page(source: List<ReportSummaryData>, cursor: String?, limit: Int): ApiResult<ReportPageData> {
        val offset = cursor?.toInt() ?: 0
        val items = source.drop(offset).take(limit)
        val next = (offset + items.size).takeIf { it < source.size }
        return ApiResult.Success(ReportPageData(items, next?.toString()), 200, "SUCCESS", "", ResponseSource.NETWORK)
    }

    private fun report(id: Int, type: String = "monthly") =
        ReportSummaryData(id, "2026-11-30T10:00:00+09:00", type, "2026-11-01", "2026-11-30", false)
}
