package com.yu_morph.smartbudget.ui.auth

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent

private const val EXAMPLE_URL = "https://example.com"
private const val OPEN_SOURCE_LICENSE_ASSET = "open_source_licenses.html"

internal fun openExampleCustomTab(context: Context) {
    openCustomTab(context, Uri.parse(EXAMPLE_URL))
}

internal fun openOpenSourceLicensesCustomTab(context: Context): Boolean = runCatching {
    val html =
        context.assets
            .open(OPEN_SOURCE_LICENSE_ASSET)
            .bufferedReader()
            .use { it.readText() }
    val encodedHtml = Base64.encodeToString(html.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    openCustomTab(context, Uri.parse("data:text/html;charset=utf-8;base64,$encodedHtml"))
}.isSuccess

private fun openCustomTab(context: Context, uri: Uri) {
    CustomTabsIntent
        .Builder()
        .setShowTitle(true)
        .build()
        .launchUrl(context, uri)
}
