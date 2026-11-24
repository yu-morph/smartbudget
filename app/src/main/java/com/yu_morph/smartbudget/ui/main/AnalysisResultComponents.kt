@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen

@Composable
internal fun ResultCard(title: Int, highlighted: Boolean = false, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                colors.primary.copy(alpha = 0.10f).compositeOver(colors.surface)
            } else {
                colors.surface
            },
            contentColor = colors.onSurface,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = if (highlighted) colors.primary else colors.onSurface,
            )
            content()
        }
    }
}

@Composable
internal fun ResultLine(category: Int, count: Int, note: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(category), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(stringResource(count), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Text(
            stringResource(note),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
internal fun HealthScore(modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            drawArc(colors.surfaceVariant, -90f, 360f, false, style = Stroke(8.dp.toPx()))
            drawArc(colors.primary, -90f, 280.8f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
        }
        Text(stringResource(R.string.analysis_result_health_score), fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun WeekdayChart() {
    val colors = MaterialTheme.colorScheme
    val peakDay = 4
    val values = listOf(0.55f, 0.50f, 0.61f, 0.66f, 0.95f, 0.86f, 0.81f)
    val weekdayResources = listOf(
        R.string.weekday_monday,
        R.string.weekday_tuesday,
        R.string.weekday_wednesday,
        R.string.weekday_thursday,
        R.string.weekday_friday,
        R.string.weekday_saturday,
        R.string.weekday_sunday,
    )
    Row(
        Modifier.fillMaxWidth().height(136.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((94 * value).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (index == peakDay) colors.primary else colors.primary.copy(alpha = 0.35f)),
                )
                Text(
                    stringResource(weekdayResources[index]),
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 11.sp,
                    color = if (index == peakDay) colors.primary else colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun AdviceCard(title: Int, advice: List<Int>) {
    ResultCard(title) {
        advice.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(22.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(item), modifier = Modifier.padding(start = 10.dp), lineHeight = 20.sp)
            }
        }
    }
}
