@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import java.time.YearMonth

private val ReportHeroDarkGreen = Color(0xFF10362C)
private val ReportHeroLightGreen = Color(0xFF19553F)
private val ReportHeroMint = Color(0xFFAEF2D1)

@Composable
internal fun AnalysisResultScreen(modifier: Modifier, period: YearMonth, content: AnalysisReportContent?) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                stringResource(R.string.analysis_result_title, period.year, period.monthValue),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onBackground,
            )
            if (content?.hasReadableSection == true) {
                ReportContent(content)
            } else {
                Text(
                    stringResource(R.string.analysis_error_unreadable_report),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReportContent(content: AnalysisReportContent) {
    content.aWordFromAi?.let { ReportSummary(it) }
    if (content.topSpendingCategories.isNotEmpty()) ReportCategories(content.topSpendingCategories)

    val insights = listOfNotNull(
        content.spendingHealth?.let {
            ReportNote(R.string.analysis_result_health_title, Icons.Outlined.FavoriteBorder, it)
        },
        content.spendingByDayOfWeek?.let {
            ReportNote(R.string.analysis_result_weekday_title, Icons.Outlined.CalendarToday, it)
        },
        content.newsDrivenInsights?.let {
            ReportNote(R.string.analysis_result_news_title, Icons.Outlined.Newspaper, it)
        },
    )
    val advice = listOfNotNull(
        content.tipsForSaving?.let {
            ReportNote(R.string.analysis_result_save_advice, Icons.Outlined.Savings, it)
        },
        content.tipsToKeepItUp?.let {
            ReportNote(R.string.analysis_result_maintain_advice, Icons.Outlined.CheckCircleOutline, it)
        },
        content.tipsForInterests?.let {
            ReportNote(R.string.analysis_result_interest_title, Icons.Outlined.Lightbulb, it)
        },
    )
    if (insights.isNotEmpty()) ReportNoteSection(R.string.analysis_result_insights_heading, insights)
    if (advice.isNotEmpty()) ReportNoteSection(R.string.analysis_result_advice_heading, advice, highlighted = true)
}

@Composable
internal fun ReportSummary(
    body: String,
    caption: String? = null,
    maxLines: Int = Int.MAX_VALUE,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(24.dp)
    val action = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(
            role = Role.Button,
            onClickLabel = stringResource(R.string.analysis_latest_open),
            onClick = onClick,
        )
    }
    Box(
        modifier = Modifier.fillMaxWidth().clip(shape).then(action).background(
            Brush.linearGradient(listOf(ReportHeroDarkGreen, ReportHeroLightGreen)),
        ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * 0.96f, size.height * 0.04f)
            listOf(46.dp, 70.dp, 94.dp).forEach { radius ->
                drawCircle(
                    color = ReportHeroMint.copy(alpha = 0.12f),
                    radius = radius.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = ReportHeroMint)
                Text(
                    stringResource(R.string.analysis_result_ai_title),
                    modifier = Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ReportHeroMint,
                )
                if (onClick != null) Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = ReportHeroMint)
            }
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelMedium, color = ReportHeroMint)
            }
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = Color.White,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ReportCategories(categories: List<AnalysisCategoryInsight>) {
    val maxOccurrence = categories.maxOf { it.occurrence ?: 0 }
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReportBlockTitle(R.string.analysis_result_categories_title)
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(colors.surfaceVariant.copy(alpha = 0.20f), shape)
                .border(1.dp, colors.outlineVariant.copy(alpha = 0.55f), shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            categories.forEachIndexed { index, category ->
                if (index > 0) HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                AnalysisCategoryRow(category, maxOccurrence)
            }
        }
    }
}

@Composable
private fun AnalysisCategoryRow(category: AnalysisCategoryInsight, maxOccurrence: Int) {
    val ui = transactionCategoryUi(reportCategoryCode(category.name))
    val colors = MaterialTheme.colorScheme
    val count = category.occurrence?.takeIf { it >= 0 }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReportIcon(ui.icon, ui.color)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    category.name.ifBlank { stringResource(R.string.transaction_category_unknown) },
                    modifier = Modifier.weight(1f),
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                count?.let {
                    Text(
                        stringResource(R.string.analysis_result_occurrence, it),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                }
            }
            if (count != null && maxOccurrence > 0) {
                val trackShape = RoundedCornerShape(4.dp)
                val fraction = count.toFloat() / maxOccurrence
                Box(
                    Modifier.fillMaxWidth().height(4.dp)
                        .background(colors.outlineVariant.copy(alpha = 0.40f), trackShape),
                ) {
                    Box(Modifier.fillMaxWidth(fraction).height(4.dp).background(ui.color, trackShape))
                }
            }
            if (category.tip.isNotBlank()) {
                Text(
                    category.tip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}

private data class ReportNote(@param:StringRes val title: Int, val icon: ImageVector, val body: String)

@Composable
private fun ReportNoteSection(title: Int, notes: List<ReportNote>, highlighted: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val panelModifier = if (highlighted) {
        Modifier.background(colors.primary.copy(alpha = 0.07f), shape)
            .border(1.dp, colors.primary.copy(alpha = 0.20f), shape)
            .padding(16.dp)
    } else {
        Modifier
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReportBlockTitle(title)
        Column(
            modifier = panelModifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            notes.forEachIndexed { index, note ->
                if (index > 0) HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.50f))
                ReportNoteRow(note)
            }
        }
    }
}

@Composable
private fun ReportNoteRow(note: ReportNote) {
    val colors = MaterialTheme.colorScheme
    val highlights = if (colors.background.luminance() < 0.5f) {
        ReportHighlightColors(Color(0xFFFF9292), Color(0xFF8CBFFF), Color(0xFF72DFAD))
    } else {
        ReportHighlightColors(Color(0xFFB4232C), Color(0xFF175CB0), Color(0xFF087A55))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReportIcon(note.icon, colors.primary)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                stringResource(note.title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
            Text(
                highlightReportText(note.body, highlights),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 23.sp,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReportIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier.size(36.dp).background(tint.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = tint)
    }
}

@Composable
private fun ReportBlockTitle(@StringRes title: Int) {
    Text(
        stringResource(title),
        modifier = Modifier.semantics { heading() }.padding(top = 8.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
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
