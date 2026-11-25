package com.yu_morph.smartbudget.ui.main

import com.yu_morph.smartbudget.data.remote.news.NewsArticle
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisReportTest {
    @Test
    fun `월간 분석 요청은 선택한 달의 1일을 시작일로 사용한다`() {
        val request = buildMonthlyReportRequest(
            month = YearMonth.of(2026, 11),
            survey = AnalysisSurvey(birthDate = "1995-05-20", managementDirection = "save"),
            newsTitles = listOf("오늘 기사"),
            today = LocalDate.of(2026, 11, 26),
        )

        assertEquals("monthly", request.periodType)
        assertEquals("2026-11-01", request.periodStart)
        assertEquals(31, request.age)
        assertEquals("절약", request.approach)
        assertEquals(listOf("오늘 기사"), request.news)
    }

    @Test
    fun `한국 시간 기준 당일 기사만 중복 없이 선택한다`() {
        val zone = ZoneId.of("Asia/Seoul")
        val articles = listOf(
            NewsArticle("오늘 기사", "https://example.test/1", Instant.parse("2026-11-25T15:30:00Z"), null),
            NewsArticle("오늘 기사", "https://example.test/2", Instant.parse("2026-11-25T16:00:00Z"), null),
            NewsArticle("어제 기사", "https://example.test/3", Instant.parse("2026-11-25T14:59:59Z"), null),
            NewsArticle("날짜 없음", "https://example.test/4", null, null),
        )

        assertEquals(
            listOf("오늘 기사"),
            todayNewsTitles(articles, LocalDate.of(2026, 11, 26), zone),
        )
    }

    @Test
    fun `소비 분석 XML의 지원 섹션과 카테고리를 해석한다`() {
        val report = AnalysisReportParser.parse(
            """
            <report>
              <a_word_from_ai>이번 달도 잘 기록했어요.</a_word_from_ai>
              <top_spending_categories>
                <category name="식비" occurrence="4" tip="주간 한도를 정해보세요."/>
              </top_spending_categories>
              <spending_health>지출이 안정적이에요.</spending_health>
              <tips_for_saving>외식을 한 번 줄여보세요.</tips_for_saving>
            </report>
            """.trimIndent(),
        )

        assertEquals("이번 달도 잘 기록했어요.", report?.aWordFromAi)
        assertEquals("식비", report?.topSpendingCategories?.single()?.name)
        assertEquals(4, report?.topSpendingCategories?.single()?.occurrence)
        assertEquals("지출이 안정적이에요.", report?.spendingHealth)
        assertTrue(report?.hasReadableSection == true)
    }

    @Test
    fun `지원 섹션이 없거나 안전하지 않은 XML은 표시하지 않는다`() {
        assertNull(AnalysisReportParser.parse("<report><unknown>내용</unknown></report>"))
        assertNull(AnalysisReportParser.parse("<!DOCTYPE report><report><a_word_from_ai>내용</a_word_from_ai></report>"))
        assertFalse(AnalysisReportContent().hasReadableSection)
    }
}
