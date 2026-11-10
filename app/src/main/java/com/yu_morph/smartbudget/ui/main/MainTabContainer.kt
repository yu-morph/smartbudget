package com.yu_morph.smartbudget.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val TabGradientGray = Color(0xFFF0F1F3)
private val TabGradientLightGray = Color(0xFFF8F8F7)
private val TabGradientGreen = Color(0xFFF0F8F4)

@Composable
internal fun MainTabContainer(
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors =
        if (isDark) {
            listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.background,
            )
        } else {
            listOf(TabGradientGray, TabGradientLightGray, TabGradientGreen)
        }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors))
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 48.dp, end = 20.dp, bottom = bottomPadding),
        content = content,
    )
}
