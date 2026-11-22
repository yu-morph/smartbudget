package com.yu_morph.smartbudget.ui.main

import com.yu_morph.smartbudget.data.ocr.ReceiptOcrResult
import com.yu_morph.smartbudget.data.remote.model.TransactionType
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionEditorReceiptTest {
    private val fullResult =
        ReceiptOcrResult(
            storeName = "GS25 영남대점",
            date = LocalDate.of(2026, 11, 21),
            time = LocalTime.of(14, 32),
            amount = 12_500,
            items = listOf("삼각김밥", "바나나우유"),
        )

    private fun current(
        type: TransactionType = TransactionType.EXPENSE,
        category: String = expenseTransactionCategoryCodes.first(),
        storeName: String = "",
        note: String = "",
        amountText: String = "",
    ) = ReceiptEditableFields(type, category, storeName, note, amountText)

    @Test
    fun `OCR 결과의 매장명과 금액을 입력 필드에 채운다`() {
        val category = expenseTransactionCategoryCodes.last()

        val filled = applyReceiptOcrResult(fullResult, current(category = category))

        assertEquals(TransactionType.EXPENSE, filled.type)
        assertEquals(category, filled.category)
        assertEquals("GS25 영남대점", filled.storeName)
        assertEquals("12500", filled.amountText)
    }

    @Test
    fun `인식한 품목을 쉼표로 이어 메모에 넣는다`() {
        val filled = applyReceiptOcrResult(fullResult, current(note = "기존 메모"))

        assertEquals("삼각김밥, 바나나우유", filled.note)
    }

    @Test
    fun `메모는 서버 최대 길이인 2000자를 넘지 않는다`() {
        val longItems = List(500) { "품목명$it" }

        val filled = applyReceiptOcrResult(fullResult.copy(items = longItems), current())

        assertEquals(2_000, filled.note.length)
    }

    @Test
    fun `수입 입력 중 영수증을 반영하면 지출과 기본 지출 카테고리로 바뀐다`() {
        val filled =
            applyReceiptOcrResult(
                fullResult,
                current(type = TransactionType.INCOME, category = incomeTransactionCategoryCodes.first()),
            )

        assertEquals(TransactionType.EXPENSE, filled.type)
        assertEquals(expenseTransactionCategoryCodes.first(), filled.category)
    }

    @Test
    fun `인식하지 못한 필드는 기존 입력값을 유지한다`() {
        val emptyResult = ReceiptOcrResult(storeName = " ", date = null, time = null, amount = null)

        val filled =
            applyReceiptOcrResult(
                emptyResult,
                current(storeName = "기존 매장", note = "기존 메모", amountText = "3000"),
            )

        assertEquals("기존 매장", filled.storeName)
        assertEquals("기존 메모", filled.note)
        assertEquals("3000", filled.amountText)
    }

    @Test
    fun `금액은 세 자리마다 쉼표로 구분한다`() {
        assertEquals("", formatThousands(""))
        assertEquals("500", formatThousands("500"))
        assertEquals("1,000", formatThousands("1000"))
        assertEquals("24,800,000", formatThousands("24800000"))
    }
}
