package com.yu_morph.smartbudget.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.remote.model.ReportDetailData
import com.yu_morph.smartbudget.ui.theme.SmartBudgetTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnalysisScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editingSurveyFromStartKeepsSelectedMonth() {
        showAnalysisFlow(initialStep = AnalysisFlowStep.REQUEST, returnToRequest = true)

        text(R.string.analysis_title).assertIsDisplayed()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithContentDescription(context.getString(R.string.analysis_previous_month)).performClick()
        text(R.string.analysis_request_edit_survey).performClick()
        text(R.string.analysis_survey_title).assertIsDisplayed()
        text(R.string.analysis_survey_save_button).performScrollTo().performClick()

        text(R.string.year_month, 2026, 9).assertIsDisplayed()
        text(R.string.analysis_start).assertIsDisplayed()
    }

    @Test
    fun tappingProfileCardOpensSurvey() {
        var opened = false
        composeRule.setContent {
            SmartBudgetTheme {
                AnalysisScreen(
                    bottomPadding = 0.dp,
                    state = AnalysisUiState(),
                    displayName = "테스트 사용자",
                    onLoad = {},
                    onLoadMore = {},
                    onEditSurvey = { opened = true },
                    onNewReport = {},
                    onOpenReport = {},
                )
            }
        }

        composeRule.onNodeWithText("테스트 사용자").assertIsDisplayed()
        text(R.string.analysis_survey_summary_title).performClick()
        composeRule.runOnIdle { assertTrue(opened) }
    }

    @Test
    fun surveyScreenShowsAppBarAndSaves() {
        showAnalysisFlow(initialStep = AnalysisFlowStep.SURVEY)
        text(R.string.analysis_survey_title).assertIsDisplayed()
        text(R.string.analysis_survey_save_button).performScrollTo().performClick()
    }

    @Test
    fun generatedReportShowsServerAnalysisInDarkMode() {
        showAnalysisFlow(initialStep = AnalysisFlowStep.RESULT, darkTheme = true)

        text(R.string.analysis_result_title, 2026, 10).assertIsDisplayed()
        composeRule.onNodeWithText("서버가 생성한 분석이에요.").assertIsDisplayed()
        val weekday = text(R.string.analysis_result_weekday_title).fetchSemanticsNode()
        val news = text(R.string.analysis_result_news_title).fetchSemanticsNode()
        val advice = text(R.string.analysis_result_maintain_advice).fetchSemanticsNode()
        assertTrue(weekday.positionInRoot.y < news.positionInRoot.y)
        assertTrue(news.positionInRoot.y < advice.positionInRoot.y)
        composeRule.onNodeWithText("예산 기록을 이어가세요.").performScrollTo().assertIsDisplayed()
    }

    private fun showAnalysisFlow(
        initialStep: AnalysisFlowStep,
        darkTheme: Boolean = false,
        returnToRequest: Boolean = false,
    ) {
        composeRule.setContent {
            val state =
                AnalysisUiState(
                    activeReport =
                        ReportDetailData(
                            id = 1,
                            generatedAt = "2026-11-01T00:00:00+09:00",
                            periodType = "monthly",
                            periodStart = "2026-10-01",
                            periodEnd = "2026-10-31",
                            isStale = false,
                            fullReport = "<report/>",
                        ),
                    activeContent =
                        AnalysisReportContent(
                            aWordFromAi = "서버가 생성한 분석이에요.",
                            spendingByDayOfWeek = "금요일의 지출이 가장 많았어요.",
                            newsDrivenInsights = "대중교통 요금 변동을 확인해 보세요.",
                            tipsToKeepItUp = "예산 기록을 이어가세요.",
                        ),
                )
            SmartBudgetTheme(darkTheme = darkTheme) {
                AnalysisFlowScreen(
                    analysisState = state,
                    initialSurvey = AnalysisSurvey(),
                    initialStep = initialStep,
                    initialYear = 2026,
                    initialMonth = 10,
                    continueAfterSurvey = returnToRequest,
                    closeAfterResult = true,
                    onBack = {},
                    onSurveySaved = {},
                    onRequestReport = { _, _ -> },
                    onClearFailure = {},
                )
            }
        }
    }

    private fun text(resId: Int, vararg formatArgs: Any) = composeRule.onNodeWithText(
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId, *formatArgs),
    )
}
