package com.yu_morph.smartbudget.ui.main

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportColorMarkupTest {
    private val colors = ReportHighlightColors(Color.Red, Color.Blue, Color.Green)

    @Test
    fun `지원하는 태그는 제거하고 지정된 범위에 색상을 적용한다`() {
        val result = highlightReportText("금액 [!color red]100원[/] [!color blue]50원[/] [!color green]중요[/]", colors)

        assertEquals("금액 100원 50원 중요", result.text)
        assertEquals(listOf(Color.Red, Color.Blue, Color.Green), result.spanStyles.map { it.item.color })
        assertEquals(listOf("100원", "50원", "중요"), result.spanStyles.map { result.text.substring(it.start, it.end) })
    }

    @Test
    fun `미지원 색상과 닫히지 않은 태그는 그대로 남긴다`() {
        val value = "[!color yellow]기타[/] [!color red]미완성"

        assertEquals(value, highlightReportText(value, colors).text)
    }
}
