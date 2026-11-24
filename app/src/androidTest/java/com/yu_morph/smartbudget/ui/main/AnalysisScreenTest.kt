package com.yu_morph.smartbudget.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.yu_morph.smartbudget.R
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
    fun sampleReportShowsSpendingAndAdviceInDarkMode() {
        showAnalysis(darkTheme = true)

        text(R.string.sample_report_october).performScrollTo().performClick()
        text(R.string.analysis_sample_data).assertIsDisplayed()
        text(R.string.analysis_result_total).assertIsDisplayed()
        text(R.string.analysis_result_advice_heading).performScrollTo().assertIsDisplayed()
    }

    private fun showAnalysis(darkTheme: Boolean) {
        composeRule.setContent {
            SmartBudgetTheme(darkTheme = darkTheme) { AnalysisScreen(bottomPadding = 0.dp) }
        }
    }

    private fun text(resId: Int) = composeRule.onNodeWithText(
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId),
    )
}
