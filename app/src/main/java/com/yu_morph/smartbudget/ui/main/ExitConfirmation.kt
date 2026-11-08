package com.yu_morph.smartbudget.ui.main

internal class ExitConfirmation(private val confirmationWindowMillis: Long = DEFAULT_WINDOW_MILLIS) {
    private var firstBackPressAt: Long? = null

    fun shouldExit(nowMillis: Long): Boolean {
        val previousPress = firstBackPressAt
        val shouldExit = previousPress != null && nowMillis - previousPress <= confirmationWindowMillis
        firstBackPressAt = if (shouldExit) null else nowMillis
        return shouldExit
    }

    private companion object {
        const val DEFAULT_WINDOW_MILLIS = 2_000L
    }
}
