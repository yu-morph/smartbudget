package com.yu_morph.smartbudget.ui.main

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

internal data class ReportHighlightColors(val red: Color, val blue: Color, val green: Color)

private val ReportColorTag =
    Regex("""\[!color (red|blue|green)]((?:(?!\[!color |\[/]).)*+)\[/]""", RegexOption.DOT_MATCHES_ALL)

internal fun highlightReportText(text: String, colors: ReportHighlightColors): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    ReportColorTag.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val (name, body) = match.destructured
        val color = when (name) {
            "red" -> colors.red
            "blue" -> colors.blue
            else -> colors.green
        }
        withStyle(SpanStyle(color = color)) { append(body) }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}
