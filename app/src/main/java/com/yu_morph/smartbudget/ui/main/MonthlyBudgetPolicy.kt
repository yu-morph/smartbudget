package com.yu_morph.smartbudget.ui.main

internal const val DEFAULT_MONTHLY_BUDGET = 1_000_000

internal fun resolveMonthlyBudget(currentAmount: Int?, previousAmount: Int?): Int =
    currentAmount ?: previousAmount ?: DEFAULT_MONTHLY_BUDGET
