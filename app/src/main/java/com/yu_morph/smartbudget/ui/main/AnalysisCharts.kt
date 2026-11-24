@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen

@Composable
internal fun DonutChart(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Butt)
        drawArc(SmartBudgetGreen, -90f, 162f, false, style = stroke)
        drawArc(AnalysisSlate, 72f, 108f, false, style = stroke)
        drawArc(AnalysisBlue, 180f, 90f, false, style = stroke)
    }
}

@Composable
internal fun TrendChart(modifier: Modifier = Modifier) {
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
