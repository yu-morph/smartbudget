package com.yu_morph.smartbudget.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.yu_morph.smartbudget.R

val PretendardFontFamily = FontFamily(Font(R.font.pretendard_variable))
val OutfitFontFamily = FontFamily(Font(R.font.outfit_variable))

private val DefaultTypography = Typography()

val Typography =
    Typography(
        displayLarge = DefaultTypography.displayLarge.copy(fontFamily = PretendardFontFamily),
        displayMedium = DefaultTypography.displayMedium.copy(fontFamily = PretendardFontFamily),
        displaySmall = DefaultTypography.displaySmall.copy(fontFamily = PretendardFontFamily),
        headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = PretendardFontFamily),
        headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = PretendardFontFamily),
        headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = PretendardFontFamily),
        titleLarge = DefaultTypography.titleLarge.copy(fontFamily = PretendardFontFamily),
        titleMedium = DefaultTypography.titleMedium.copy(fontFamily = PretendardFontFamily),
        titleSmall = DefaultTypography.titleSmall.copy(fontFamily = PretendardFontFamily),
        bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = PretendardFontFamily),
        bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = PretendardFontFamily),
        bodySmall = DefaultTypography.bodySmall.copy(fontFamily = PretendardFontFamily),
        labelLarge = DefaultTypography.labelLarge.copy(fontFamily = PretendardFontFamily),
        labelMedium = DefaultTypography.labelMedium.copy(fontFamily = PretendardFontFamily),
        labelSmall = DefaultTypography.labelSmall.copy(fontFamily = PretendardFontFamily),
    )
