@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.Male
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

internal val AnalysisSlate = Color(0xFF667386)
internal val AnalysisBlue = Color(0xFF8CA2B8)

@Composable
internal fun AnalysisScreen(
    bottomPadding: Dp,
    state: AnalysisUiState,
    displayName: String,
    onLoad: () -> Unit,
    onEditSurvey: () -> Unit,
    onNewReport: () -> Unit,
    onOpenReport: (com.yu_morph.smartbudget.data.remote.model.ReportSummaryData) -> Unit,
) {
    val survey = state.survey
    LaunchedEffect(Unit) { onLoad() }
    MainTabContainer(bottomPadding = bottomPadding) {
        SurveySummaryCard(survey = survey, displayName = displayName, onClick = onEditSurvey)
        Spacer(Modifier.height(14.dp))
        PreviousReportsCard(
            state = state,
            onNewReport = onNewReport,
            onReportClick = onOpenReport,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SurveySummaryCard(survey: AnalysisSurvey, displayName: String, onClick: () -> Unit) {
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant
    val editLabel = stringResource(R.string.analysis_edit_survey)
    AnalysisCard(
        modifier = Modifier.clip(RoundedCornerShape(24.dp)).clickable(
            role = Role.Button,
            onClickLabel = editLabel,
            onClick = onClick,
        ),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnalysisCardHeading(
                R.string.analysis_survey_summary_title,
                Modifier.weight(1f).padding(start = 8.dp),
                inset = 0.dp,
            )
            Icon(
                Icons.Outlined.ChevronRight,
                modifier = Modifier.padding(end = 8.dp),
                contentDescription = null,
                tint = bodyColor.copy(alpha = 0.65f),
            )
        }
        Spacer(Modifier.height(24.dp))
        CompositionLocalProvider(LocalContentColor provides bodyColor) {
            ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveySummaryContent(survey, displayName)
                }
            }
        }
    }
}

@Composable
private fun SurveySummaryContent(survey: AnalysisSurvey, displayName: String) {
    if (displayName.isNotBlank()) Text(displayName, fontWeight = FontWeight.SemiBold)
    SurveyDemographics(survey)
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    )
    SurveyProfileLine(Icons.Outlined.Person, R.string.analysis_survey_job, survey.job)
    SurveyProfileLine(Icons.Outlined.Business, R.string.analysis_survey_workplace, survey.workplace)
    SurveyProfileLine(Icons.Outlined.School, R.string.analysis_survey_education, survey.education)
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    )
    SurveySummaryRow(
        stringResource(R.string.analysis_survey_loan),
        stringResource(
            if (survey.hasLoan) R.string.analysis_survey_yes else R.string.analysis_survey_no,
        ),
    )
    if (survey.hasLoan && survey.loanDetails.isNotBlank()) {
        Text(
            survey.loanDetails,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    )
    SurveySummaryRow(
        stringResource(R.string.analysis_survey_goal_label),
        stringResource(
            if (survey.managementDirection == "maintain") {
                R.string.analysis_survey_maintain
            } else {
                R.string.analysis_survey_save
            },
        ),
    )
}

@Composable
private fun SurveyDemographics(survey: AnalysisSurvey) {
    val age = surveyAge(survey.birthDate)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            if (age == null) {
                survey.birthDate.ifBlank { stringResource(R.string.analysis_info_missing) }
            } else {
                stringResource(R.string.analysis_age_group, age / 10 * 10, survey.birthDate)
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.95f),
        )
        if (survey.gender == "male" || survey.gender == "female") {
            val male = survey.gender == "male"
            Icon(
                if (male) Icons.Outlined.Male else Icons.Outlined.Female,
                stringResource(
                    if (male) R.string.analysis_survey_male else R.string.analysis_survey_female,
                ),
                modifier = Modifier.size(16.dp),
                tint = if (male) Color(0xFF2879CB) else Color(0xFFD04459),
            )
        }
    }
}

internal fun surveyAge(birthDate: String, today: LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul"))): Int? =
    runCatching { LocalDate.parse(birthDate) }.getOrNull()
        ?.takeUnless { it.isAfter(today) }
        ?.let { Period.between(it, today).years }

@Composable
private fun AnalysisCardHeading(title: Int, modifier: Modifier, inset: Dp = 8.dp) {
    Text(
        stringResource(title),
        modifier = modifier.padding(start = inset).semantics { heading() },
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SurveyProfileLine(icon: ImageVector, label: Int, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, stringResource(label), modifier = Modifier.size(14.dp).alpha(0.7f))
        Text(
            value.ifBlank { stringResource(R.string.analysis_info_missing) },
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun SurveySummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(2f))
        Text(
            value,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(5f),
            fontWeight = FontWeight.SemiBold,
        )
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
            AnalysisCardHeading(R.string.analysis_report_history, Modifier.weight(1f))
            TextButton(
                onClick = onNewReport,
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
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
                fontSize = 13.sp,
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
