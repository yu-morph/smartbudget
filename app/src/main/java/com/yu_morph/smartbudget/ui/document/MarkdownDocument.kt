package com.yu_morph.smartbudget.ui.document

import androidx.annotation.StringRes
import com.yu_morph.smartbudget.R

internal enum class MarkdownDocument(@param:StringRes val titleResource: Int, val assetName: String) {
    TERMS_OF_SERVICE(R.string.settings_terms_of_service, "terms_of_service.md"),
    PRIVACY_POLICY(R.string.settings_privacy_policy, "privacy_policy.md"),
    OPEN_SOURCE_LICENSES(R.string.settings_open_source_licenses, "open_source_licenses.md"),
}
