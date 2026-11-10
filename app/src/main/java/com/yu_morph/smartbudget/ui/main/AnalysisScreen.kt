@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen

private val AnalysisSlate = Color(0xFF667386)
private val AnalysisBlue = Color(0xFF8CA2B8)
private val AnalysisOrange = Color(0xFFF0A04B)

@Composable
internal fun AnalysisScreen(bottomPadding: Dp) {
    MainTabContainer(bottomPadding = bottomPadding) {
        AnalysisInsightCard()
        Spacer(Modifier.height(14.dp))
        CategoryCard()
        Spacer(Modifier.height(14.dp))
        TrendCard()
        Spacer(Modifier.height(14.dp))
        PreviousReportsCard()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AnalysisInsightCard() {
    AnalysisCard(containerColor = SmartBudgetGreen.copy(alpha = 0.1f)) {
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
private fun DonutChart(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Butt)
        drawArc(SmartBudgetGreen, -90f, 162f, false, style = stroke)
        drawArc(AnalysisSlate, 72f, 108f, false, style = stroke)
        drawArc(AnalysisBlue, 180f, 90f, false, style = stroke)
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
private fun TrendChart(modifier: Modifier = Modifier) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        val values = listOf(0.62f, 0.48f, 0.56f, 0.35f, 0.44f, 0.22f)
        val points = values.mapIndexed { index, value ->
            Offset(size.width * index / (values.size - 1), size.height * value)
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(SmartBudgetGreen, start, end, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        points.forEach { drawCircle(SmartBudgetGreen, radius = 5.dp.toPx(), center = it) }
    }
}

@Composable
private fun PreviousReportsCard() {
    AnalysisCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.analysis_previous), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.analysis_recent_three_months),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        ReportRow(stringResource(R.string.sample_report_october), stringResource(R.string.sample_report_food), "-3%")
        ReportRow(stringResource(R.string.sample_report_september), stringResource(R.string.sample_report_fixed), "+2%")
        ReportRow(stringResource(R.string.sample_report_august), stringResource(R.string.sample_report_goal), "-6%")
    }
}

@Composable
private fun ReportRow(month: String, description: String, change: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(9.dp).background(AnalysisOrange, CircleShape))
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(month, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Text(change, color = SmartBudgetGreen, fontWeight = FontWeight.Bold)
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
private fun AnalysisCard(
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), content = content)
    }
}
