package com.yu_morph.smartbudget.ui.auth

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

private const val EXAMPLE_URL = "https://example.com"

internal fun openExampleCustomTab(context: Context) {
    CustomTabsIntent
        .Builder()
        .setShowTitle(true)
        .build()
        .launchUrl(context, Uri.parse(EXAMPLE_URL))
}
