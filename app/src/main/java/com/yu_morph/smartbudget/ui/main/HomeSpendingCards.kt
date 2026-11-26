@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yu_morph.smartbudget.R

@Composable
internal fun HomeSpendingCards(state: HomeDashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RecentExpenseCard(state, Modifier.weight(2f).fillMaxHeight())
        SpendingTemperatureCard(state.spendingTemperature, Modifier.weight(3f).fillMaxHeight())
    }
}

@Composable
private fun RecentExpenseCard(state: HomeDashboardUiState, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 12.dp, vertical = 20.dp),
        ) {
            HomeSpendingCardHeading(stringResource(R.string.home_recent_expense))
            Spacer(Modifier.height(8.dp))
            val expense = state.recentExpense
            if (expense != null) {
                val category = transactionCategoryUi(expense.category)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(28.dp)
                            .background(category.color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(category.icon, null, modifier = Modifier.size(17.dp), tint = category.color)
                    }
                    Text(
                        stringResource(category.label),
                        color = category.color,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.weight(1f).height(4.dp))
                Text(
                    formatHomeAmount(expense.amount),
                    modifier = Modifier.align(Alignment.End),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                val message = if (state.isLoading) {
                    R.string.home_recent_expense_loading
                } else {
                    R.string.home_recent_expense_empty
                }
                Text(
                    stringResource(message),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
