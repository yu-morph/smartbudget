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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen

@Composable
internal fun ResultCard(title: Int, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
internal fun ResultLine(category: Int, count: Int, note: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(category), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(stringResource(count), color = SmartBudgetGreen, fontWeight = FontWeight.Bold)
        Text(
            stringResource(note),
            modifier = Modifier.weight(1.5f).padding(start = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
internal fun HealthScore(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            drawArc(Color(0xFFDCE5E9), -90f, 360f, false, style = Stroke(14.dp.toPx()))
            drawArc(SmartBudgetGreen, -90f, 280.8f, false, style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
        }
        Text(stringResource(R.string.analysis_result_health_score), fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun WeekdayChart() {
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
                        .background(if (index == 4) SmartBudgetGreen else SmartBudgetGreen.copy(alpha = 0.25f)),
                )
                Text(
                    stringResource(weekdayResources[index]),
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 11.sp,
                    color = if (index == 4) SmartBudgetGreen else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Modifier.size(22.dp).background(SmartBudgetGreen, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(item), modifier = Modifier.padding(start = 10.dp), lineHeight = 20.sp)
            }
        }
    }
}
