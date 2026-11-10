@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

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
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ExpenseRed = Color(0xFFEA5A5A)
private val IncomeBlue = Color(0xFF4B88D8)

@Composable
internal fun HomeScreen(bottomPadding: Dp, displayName: String) {
    val name = displayName.ifBlank { stringResource(R.string.settings_profile_default_name) }
    MainTabContainer(bottomPadding = bottomPadding) {
        Text(
            text = stringResource(R.string.home_greeting, name),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.home_greeting_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(20.dp))
        MonthlyOverviewCard()
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(stringResource(R.string.home_monthly_income), 3_200_000, IncomeBlue, Modifier.weight(1f))
            SummaryCard(stringResource(R.string.home_monthly_expense), 1_250_000, ExpenseRed, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        InsightCard()
        Spacer(Modifier.height(14.dp))
        RecentTransactionsCard()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MonthlyOverviewCard() {
    DashboardCard {
        Text(
            stringResource(R.string.home_monthly_spending, LocalDate.now().monthValue),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.home_sample_spending), fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.home_budget_progress), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { 0.625f },
            modifier = Modifier.fillMaxWidth().height(9.dp),
            color = SmartBudgetGreen,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.home_remaining_budget), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.home_sample_remaining), color = SmartBudgetGreen, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SummaryCard(label: String, amount: Int, color: Color, modifier: Modifier = Modifier) {
    DashboardCard(modifier) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.height(10.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(
            stringResource(
                R.string.settings_budget_history_amount,
                NumberFormat.getNumberInstance(Locale.KOREA).format(amount),
            ),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun InsightCard() {
    DashboardCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).background(SmartBudgetGreen.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = SmartBudgetGreen)
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(stringResource(R.string.home_insight_title), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.home_insight_summary), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.home_insight_body),
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun RecentTransactionsCard() {
    DashboardCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.home_recent_transactions), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.home_this_week), color = SmartBudgetGreen, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        TransactionRow(
            Icons.Outlined.ShoppingBag,
            stringResource(R.string.sample_mart),
            stringResource(R.string.sample_grocery),
            "-45,000원",
        )
        TransactionRow(
            Icons.Outlined.Coffee,
            stringResource(R.string.sample_cafe_name),
            stringResource(R.string.sample_cafe),
            "-6,500원",
        )
        TransactionRow(
            Icons.Outlined.DirectionsBus,
            stringResource(R.string.sample_transit),
            stringResource(R.string.sample_transport),
            "-2,500원",
        )
    }
}

@Composable
private fun TransactionRow(icon: ImageVector, title: String, category: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(category, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Text(amount, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DashboardCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), content = content)
    }
}
