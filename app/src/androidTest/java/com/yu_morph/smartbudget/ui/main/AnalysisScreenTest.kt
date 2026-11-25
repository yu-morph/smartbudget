package com.yu_morph.smartbudget.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.remote.model.ReportDetailData
import com.yu_morph.smartbudget.data.remote.model.ReportSummaryData
import com.yu_morph.smartbudget.ui.theme.SmartBudgetTheme
import org.junit.Rule
import org.junit.Test

class AnalysisScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun savingSurveyFromProfileReturnsToAnalysisHistory() {
        showAnalysis(darkTheme = false)

        text(R.string.analysis_edit_survey).performClick()
        text(R.string.analysis_survey_title).assertIsDisplayed()
        text(R.string.analysis_survey_save_button).performScrollTo().performClick()

        text(R.string.analysis_report_history).assertIsDisplayed()
    }

    @Test
    fun generatedReportShowsServerAnalysisInDarkMode() {
        showAnalysis(darkTheme = true)

        text(R.string.analysis_report_month, 2026, 10).performScrollTo().performClick()
        composeRule.onNodeWithText("서버가 생성한 분석이에요.").assertIsDisplayed()
        composeRule.onNodeWithText("예산 기록을 이어가세요.").performScrollTo().assertIsDisplayed()
    }

    private fun showAnalysis(darkTheme: Boolean) {
        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    AnalysisUiState(
                        reports =
                            listOf(
                                ReportSummaryData(
                                    id = 1,
                                    generatedAt = "2026-11-01T00:00:00+09:00",
                                    periodType = "monthly",
                                    periodStart = "2026-10-01",
                                    periodEnd = "2026-10-31",
                                    isStale = false,
                                ),
                            ),
                    ),
                )
            }
            SmartBudgetTheme(darkTheme = darkTheme) {
                AnalysisScreen(
                    bottomPadding = 0.dp,
                    state = state,
                    onLoad = {},
                    onRequestReport = { _, _ -> },
                    onOpenReport = {
                        state =
                            state.copy(
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
                                        tipsToKeepItUp = "예산 기록을 이어가세요.",
                                    ),
                            )
                    },
                    onClearFailure = {},
                )
            }
        }
    }

    private fun text(resId: Int, vararg formatArgs: Any) = composeRule.onNodeWithText(
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId, *formatArgs),
    )
}
