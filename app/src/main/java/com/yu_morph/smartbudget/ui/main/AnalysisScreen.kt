@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

internal val AnalysisSlate = Color(0xFF667386)
internal val AnalysisBlue = Color(0xFF8CA2B8)

@Composable
internal fun AnalysisScreen(
    bottomPadding: Dp,
    state: AnalysisUiState,
    onLoad: () -> Unit,
    onSurveySaved: (AnalysisSurvey) -> Unit,
    onRequestReport: (YearMonth, AnalysisSurvey) -> Unit,
    onOpenReport: (Int) -> Unit,
    onClearFailure: () -> Unit,
) {
    var flow by rememberSaveable { mutableStateOf<AnalysisFlowStep?>(null) }
    val survey = state.survey
    var requestAfterSurvey by rememberSaveable { mutableStateOf(false) }
    var closeAfterResult by rememberSaveable { mutableStateOf(false) }
    val currentMonth = remember { YearMonth.now(analysisZone) }
    var selectedYear by rememberSaveable { mutableStateOf(currentMonth.year) }
    var selectedMonth by rememberSaveable { mutableStateOf(currentMonth.monthValue) }
    LaunchedEffect(Unit) { onLoad() }
    if (flow != null) {
        AnalysisFlowScreen(
            bottomPadding = bottomPadding,
            analysisState = state,
            initialSurvey = survey,
            initialStep = flow ?: AnalysisFlowStep.SURVEY,
            initialYear = selectedYear,
            initialMonth = selectedMonth,
            continueAfterSurvey = requestAfterSurvey,
            closeAfterResult = closeAfterResult,
            onBack = { flow = null },
            onSurveySaved = onSurveySaved,
            onRequestReport = onRequestReport,
            onClearFailure = onClearFailure,
        )
        return
    }
    MainTabContainer(bottomPadding = bottomPadding) {
        SurveySummaryCard(survey = survey, onClick = {
            requestAfterSurvey = false
            closeAfterResult = false
            flow = AnalysisFlowStep.SURVEY
        })
        Spacer(Modifier.height(14.dp))
        PreviousReportsCard(
            state = state,
            onNewReport = {
                requestAfterSurvey = true
                closeAfterResult = false
                flow = AnalysisFlowStep.SURVEY
            },
            onReportClick = { report ->
                runCatching { LocalDate.parse(report.periodStart) }.getOrNull()?.let { date ->
                    selectedYear = date.year
                    selectedMonth = date.monthValue
                }
                requestAfterSurvey = false
                closeAfterResult = true
                onOpenReport(report.id)
                flow = AnalysisFlowStep.LOADING
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SurveySummaryCard(survey: AnalysisSurvey, onClick: () -> Unit) {
    AnalysisCard {
        Text(
            stringResource(R.string.analysis_survey_summary_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(12.dp))
        SurveySummaryRow(
            label = stringResource(R.string.analysis_survey_goal_label),
            value = if (survey.managementDirection == "maintain") {
                stringResource(R.string.analysis_survey_direction_maintain)
            } else {
                stringResource(R.string.analysis_survey_direction_save)
            },
        )
        Spacer(Modifier.height(10.dp))
        SurveySummaryRow(
            label = stringResource(R.string.analysis_survey_focus_label),
            value = survey.interest.ifBlank { stringResource(R.string.analysis_survey_focus_value) },
        )
        TextButton(onClick = onClick, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.analysis_edit_survey))
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun SurveySummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun AnalysisInsightCard() {
    AnalysisCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(SmartBudgetGreen, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White)
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    stringResource(R.string.analysis_insight_title),
                    color = SmartBudgetGreen,
                    fontWeight = FontWeight.Bold,
                )
                Text(stringResource(R.string.analysis_insight_summary), fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.analysis_insight_body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun CategoryCard() {
    AnalysisCard {
        Text(stringResource(R.string.analysis_categories), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(modifier = Modifier.size(132.dp))
            Column(
                modifier = Modifier.padding(start = 22.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryLegend(SmartBudgetGreen, stringResource(R.string.analysis_food), "45%")
                CategoryLegend(AnalysisSlate, stringResource(R.string.analysis_transport), "30%")
                CategoryLegend(AnalysisBlue, stringResource(R.string.analysis_living), "25%")
            }
        }
    }
}

@Composable
private fun TrendCard() {
    AnalysisCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.analysis_trend), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.analysis_unit),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        TrendChart(modifier = Modifier.fillMaxWidth().height(150.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(6, 7, 8, 9, 10, 11).forEach {
                val label = stringResource(R.string.month_short, it)
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PreviousReportsCard(
    state: AnalysisUiState,
    onNewReport: () -> Unit,
    onReportClick: (com.yu_morph.smartbudget.data.remote.model.ReportSummaryData) -> Unit,
) {
    AnalysisCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.analysis_report_history),
                modifier = Modifier.weight(1f).semantics {
                    heading()
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Button(onClick = onNewReport, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.analysis_new_report), modifier = Modifier.padding(start = 4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        when {
            state.isLoadingHistory -> Text(
                stringResource(R.string.analysis_history_loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.failure != null && state.reports.isEmpty() -> Text(
                analysisFailureMessage(state.failure),
                color = MaterialTheme.colorScheme.error,
            )

            state.reports.isEmpty() -> Text(
                stringResource(R.string.analysis_history_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> state.reports.forEachIndexed { index, report ->
                ReportRow(report = report) { onReportClick(report) }
                if (index != state.reports.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ReportRow(report: com.yu_morph.smartbudget.data.remote.model.ReportSummaryData, onClick: () -> Unit) {
    val month = runCatching { LocalDate.parse(report.periodStart) }.getOrNull()
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                stringResource(R.string.analysis_report_month, month?.year ?: 0, month?.monthValue ?: 0),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (report.isStale) {
                    stringResource(R.string.analysis_report_stale)
                } else {
                    stringResource(R.string.analysis_report_ready)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CategoryLegend(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(label, modifier = Modifier.padding(start = 8.dp).weight(1f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun AnalysisCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), content = content)
    }
}

private val analysisZone: ZoneId = ZoneId.of("Asia/Seoul")
