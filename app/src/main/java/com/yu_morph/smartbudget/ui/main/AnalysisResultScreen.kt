@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import java.time.YearMonth

@Composable
internal fun AnalysisResultScreen(modifier: Modifier, period: YearMonth, content: AnalysisReportContent?) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxSize().background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            stringResource(R.string.analysis_result_title, period.year, period.monthValue),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp).semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        if (content != null) ReportSections(content)
    }
}

@Composable
private fun ReportSections(content: AnalysisReportContent) {
    val categories = content.topSpendingCategories
    val sections = listOf(
        R.string.analysis_result_ai_title to content.aWordFromAi,
        R.string.analysis_result_categories_title to null,
        R.string.analysis_result_health_title to content.spendingHealth,
        R.string.analysis_result_weekday_title to content.spendingByDayOfWeek,
        R.string.analysis_result_news_title to content.newsDrivenInsights,
        R.string.analysis_result_save_advice to content.tipsForSaving,
        R.string.analysis_result_maintain_advice to content.tipsToKeepItUp,
        R.string.analysis_result_interest_title to content.tipsForInterests,
    ).filter { (title, body) ->
        body != null || (title == R.string.analysis_result_categories_title && categories.isNotEmpty())
    }
    sections.forEach { (title, body) ->
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReportSectionHeading(title)
            if (body != null) {
                Text(body, style = MaterialTheme.typography.bodyMedium, lineHeight = 24.sp)
            } else {
                categories.forEach { AnalysisCategoryRow(it) }
            }
        }
    }
}

@Composable
private fun ReportSectionHeading(title: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(4.dp, 19.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
        Text(
            stringResource(title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AnalysisCategoryRow(category: AnalysisCategoryInsight) {
    val ui = transactionCategoryUi(reportCategoryCode(category.name))
    val shape = RoundedCornerShape(10.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(40.dp).background(ui.color.copy(alpha = 0.16f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ui.icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = ui.color)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(category.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                category.occurrence?.let {
                    Text(
                        stringResource(R.string.analysis_result_occurrence, it),
                        modifier = Modifier.background(ui.color.copy(alpha = 0.13f), shape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            if (category.tip.isNotBlank()) {
                Text(category.tip, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 21.sp)
            }
        }
    }
}

internal fun reportCategoryCode(name: String): String {
    val normalized = name.trim().lowercase()
    return when (normalized) {
        "식비", "음식", "food and dining" -> "food"
        "교통", "교통비", "transportation" -> "transport"
        "쇼핑" -> "shopping"
        "주거·공과금", "주거 및 공과금", "housing & utilities" -> "housing_utilities"
        "건강", "의료", "의료비", "healthcare" -> "health"
        "여가", "문화·여가", "entertainment" -> "leisure"
        "교육", "교육비" -> "education"
        "기타", "기타 지출", "other expenses" -> "other_expense"
        else -> normalized
    }
}
