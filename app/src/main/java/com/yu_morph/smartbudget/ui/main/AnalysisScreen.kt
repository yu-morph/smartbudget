@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.remote.model.ReportSummaryData
import java.time.LocalDate

internal val AnalysisSlate = Color(0xFF667386)
internal val AnalysisBlue = Color(0xFF8CA2B8)

@Composable
internal fun AnalysisScreen(
    bottomPadding: Dp,
    state: AnalysisUiState,
    displayName: String,
    onLoad: () -> Unit,
    onLoadMore: () -> Unit,
    onEditSurvey: () -> Unit,
    onNewReport: () -> Unit,
    onOpenReport: (ReportSummaryData) -> Unit,
) {
    LaunchedEffect(Unit) { onLoad() }
    MainTabContainer(bottomPadding = bottomPadding + 20.dp) {
        Column(
            modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 640.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AnalysisSurveySummary(state.survey, displayName, onEditSurvey)
            LatestAnalysisSummary(state, onLoad, onOpenReport)
            AnalysisHistory(state, onLoad, onLoadMore, onNewReport, onOpenReport)
        }
    }
}

@Composable
private fun LatestAnalysisSummary(
    state: AnalysisUiState,
    onRetry: () -> Unit,
    onOpenReport: (ReportSummaryData) -> Unit,
) {
    val latest = state.reports.firstOrNull()
    val month = latest?.periodStart?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val caption = month?.let { stringResource(R.string.analysis_latest_month, it.year, it.monthValue) }
    val preview = state.latestPreview
    val body = when (preview) {
        is LatestReportPreview.Ready -> preview.summary ?: stringResource(R.string.analysis_latest_no_summary)

        is LatestReportPreview.Failed -> stringResource(R.string.analysis_latest_failed)

        LatestReportPreview.Loading -> stringResource(R.string.analysis_latest_loading)

        null -> when {
            state.isLoadingHistory -> stringResource(R.string.analysis_latest_loading)
            state.historyFailure != null -> stringResource(R.string.analysis_latest_failed)
            else -> stringResource(R.string.analysis_latest_empty)
        }
    }
    Column {
        ReportSummary(
            body = body,
            caption = caption,
            maxLines = 3,
            onClick = latest?.let { { onOpenReport(it) } },
        )
        if (preview is LatestReportPreview.Failed) {
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.analysis_retry))
            }
        }
    }
}

@Composable
internal fun AnalysisPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val panelContent: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), content = content)
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = colors.surfaceContainerLow,
            content = panelContent,
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = colors.surfaceContainerLow,
            content = panelContent,
        )
    }
}
