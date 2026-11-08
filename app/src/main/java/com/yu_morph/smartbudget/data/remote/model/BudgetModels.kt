package com.yu_morph.smartbudget.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class MonthlyBudgetData(val month: String, val amount: Int? = null)

@Serializable
data class ReplaceMonthlyBudgetRequest(val amount: Int? = null)
