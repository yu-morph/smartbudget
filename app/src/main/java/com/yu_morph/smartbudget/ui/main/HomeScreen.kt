@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.remote.news.NewsArticle
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import java.text.NumberFormat
import kotlin.math.roundToInt

internal val ExpenseRed = Color(0xFFEA5A5A)
internal val IncomeBlue = Color(0xFF4B88D8)
private val BudgetYellow = Color(0xFFF2C94C)
private val BudgetOrange = Color(0xFFF2994A)
private val BudgetRed = Color(0xFFEB5757)

@Composable
internal fun HomeScreen(
    bottomPadding: Dp,
    dashboardState: HomeDashboardUiState,
    displayName: String,
    newsState: HomeNewsUiState,
    onDashboardLoad: () -> Unit,
    onProfileLoad: () -> Unit,
    onNewsLoad: () -> Unit,
    onPreviousNewsPage: () -> Unit,
    onNextNewsPage: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
) {
    LaunchedEffect(Unit) {
        onDashboardLoad()
        onProfileLoad()
        onNewsLoad()
    }
    MainTabContainer(bottomPadding = bottomPadding) {
        FinancialOverviewCard(
            state = dashboardState,
            displayName = displayName.ifBlank { stringResource(R.string.settings_profile_default_name) },
        )
        Spacer(Modifier.height(14.dp))
        HomeSpendingCards(dashboardState)
        Spacer(Modifier.height(14.dp))
        NewsCard(
            state = newsState,
            onPreviousPage = onPreviousNewsPage,
            onNextPage = onNextNewsPage,
            onArticleClick = onArticleClick,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FinancialOverviewCard(state: HomeDashboardUiState, displayName: String) {
    val progress = if (state.budget > 0) state.expense.toFloat() / state.budget else 0f
    val progressColor = budgetUsageColor(progress)
    val remainingBudget = (state.budget - state.expense).coerceAtLeast(0)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            GreetingSection(displayName)
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            )
            MonthlyOverviewHeader(state)
            MonthlyCashFlowSection(state)
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            )
            MonthlyBudgetSection(state, remainingBudget, progress, progressColor)
        }
    }
}

@Composable
internal fun SpendingTemperatureCard(temperature: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        SpendingTemperatureRow(temperature = temperature, horizontalPadding = 12.dp)
    }
}

@Composable
private fun MonthlyOverviewHeader(state: HomeDashboardUiState) {
    Text(
        text = stringResource(R.string.home_monthly_overview, state.month.monthValue),
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, end = 20.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun GreetingSection(displayName: String) {
    Box(modifier = Modifier.fillMaxWidth().height(80.dp).clipToBounds()) {
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 128.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = stringResource(R.string.home_greeting, displayName),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.home_greeting_subtitle),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Image(
            painter = painterResource(R.drawable.home_greeting_robot),
            contentDescription = stringResource(R.string.home_greeting_robot_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.BottomEnd).size(160.dp).offset(x = 25.dp, y = 25.dp),
        )
    }
}

@Composable
private fun MonthlyBudgetSection(
    state: HomeDashboardUiState,
    remainingBudget: Int,
    progress: Float,
    progressColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.home_remaining_budget_amount, formatHomeAmount(remainingBudget)),
                fontSize = 14.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.home_total_budget_amount, formatHomeAmount(state.budget)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
        BudgetUsageIndicator(
            progress = progress,
            progressColor = progressColor,
        )
    }
}

@Composable
private fun MonthlyCashFlowSection(state: HomeDashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CashFlowAmount(
            label = stringResource(R.string.budget_book_income),
            amount = state.income,
            color = ExpenseRed,
            modifier = Modifier.weight(1f),
        )
        CashFlowAmount(
            label = stringResource(R.string.budget_book_expense),
            amount = state.expense,
            color = IncomeBlue,
            modifier = Modifier.weight(1f),
        )
        CashFlowAmount(
            label = stringResource(R.string.budget_book_total),
            amount = state.income - state.expense,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun CashFlowAmount(label: String, amount: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = formatHomeAmount(amount),
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
internal fun BudgetUsageIndicator(progress: Float, progressColor: Color, label: String? = null) {
    val boundedProgress = progress.coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = label ?: stringResource(R.string.home_budget_usage_percent, (progress * 100f).roundToInt()),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier =
                Modifier
                    .progressSemantics(boundedProgress)
                    .width(92.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
        ) {
            if (boundedProgress > 0f) {
                Box(Modifier.weight(boundedProgress).fillMaxHeight().background(progressColor))
            }
            if (boundedProgress < 1f) {
                Box(
                    Modifier
                        .weight(1f - boundedProgress)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }
    }
}

internal fun budgetUsageColor(progress: Float): Color = when {
    progress <= 0.5f -> SmartBudgetGreen
    progress <= 0.7f -> BudgetYellow
    progress <= 0.9f -> BudgetOrange
    else -> BudgetRed
}

@Composable
internal fun formatHomeAmount(amount: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    return stringResource(
        R.string.settings_budget_history_amount,
        NumberFormat.getNumberInstance(locale).format(amount),
    )
}
