@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.remote.model.ReportSummaryData
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import java.time.LocalDate

@Composable
internal fun AnalysisHistory(
    state: AnalysisUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onNewReport: () -> Unit,
    onOpenReport: (ReportSummaryData) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.analysis_report_history),
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = onNewReport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmartBudgetGreen,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.analysis_new_report), modifier = Modifier.padding(start = 4.dp))
            }
        }
        AnalysisPanel {
            if (state.reports.isEmpty()) {
                EmptyHistory(state, onRetry)
            } else {
                HistoryEntries(state, onRetry, onLoadMore, onOpenReport)
            }
        }
    }
}

@Composable
private fun EmptyHistory(state: AnalysisUiState, onRetry: () -> Unit) {
    val failure = state.historyFailure
    val message = when {
        state.isLoadingHistory -> stringResource(R.string.analysis_history_loading)
        failure != null -> analysisFailureMessage(failure)
        else -> stringResource(R.string.analysis_history_empty)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (failure != null && !state.isLoadingHistory) {
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.analysis_retry))
            }
        }
    }
}

@Composable
private fun HistoryEntries(
    state: AnalysisUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenReport: (ReportSummaryData) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column {
        state.reports.forEachIndexed { index, report ->
            if (index > 0) HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f))
            HistoryRow(report, number = index + 1, goal = state.reportGoals[report.id]) { onOpenReport(report) }
        }
        state.historyFailure?.let {
            Text(
                analysisFailureMessage(it),
                modifier = Modifier.padding(vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
            )
        }
        if (state.hasMoreReports) {
            TextButton(
                onClick = onLoadMore,
                enabled = !state.isLoadingHistory,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                val label = if (state.isLoadingHistory) {
                    R.string.analysis_history_loading
                } else {
                    R.string.analysis_report_more
                }
                Text(stringResource(label))
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        } else if (state.historyFailure != null) {
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.analysis_retry))
            }
        }
    }
}

@Composable
private fun HistoryRow(report: ReportSummaryData, number: Int, goal: String?, onClick: () -> Unit) {
    val start = runCatching { LocalDate.parse(report.periodStart) }.getOrNull()
    val end = runCatching { LocalDate.parse(report.periodEnd) }.getOrNull()
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.background(colors.surfaceVariant.copy(alpha = 0.36f), RoundedCornerShape(8.dp))
                .padding(horizontal = 7.dp, vertical = 5.dp),
        ) {
            Text(
                stringResource(R.string.analysis_report_number, number),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                start?.let { stringResource(R.string.analysis_report_month, it.year, it.monthValue) }
                    ?: stringResource(R.string.analysis_report_history),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                historySubtitle(start, end, goal),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            if (report.isStale) {
                Text(
                    stringResource(R.string.analysis_report_stale),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

@Composable
private fun historySubtitle(start: LocalDate?, end: LocalDate?, goal: String?): String {
    val period = if (start != null && end != null) {
        stringResource(
            R.string.analysis_report_period,
            start.monthValue,
            start.dayOfMonth,
            end.monthValue,
            end.dayOfMonth,
        )
    } else {
        stringResource(R.string.analysis_report_period_unknown)
    }
    val goalId = when (goal) {
        "save" -> R.string.analysis_report_goal_save
        "maintain" -> R.string.analysis_report_goal_maintain
        else -> null
    }
    return goalId?.let { stringResource(R.string.analysis_report_goal_period, stringResource(it), period) } ?: period
}
