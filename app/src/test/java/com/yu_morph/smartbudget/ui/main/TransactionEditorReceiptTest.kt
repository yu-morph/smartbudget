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
        )

    @Test
    fun `OCR 결과의 매장명과 금액을 입력 필드에 채운다`() {
        val category = expenseTransactionCategoryCodes.last()

        val filled = applyReceiptOcrResult(fullResult, TransactionType.EXPENSE, category, "", "")

        assertEquals(TransactionType.EXPENSE, filled.type)
        assertEquals(category, filled.category)
        assertEquals("GS25 영남대점", filled.storeName)
        assertEquals("12500", filled.amountText)
    }

    @Test
    fun `수입 입력 중 영수증을 반영하면 지출과 기본 지출 카테고리로 바뀐다`() {
        val filled =
            applyReceiptOcrResult(
                fullResult,
                TransactionType.INCOME,
                incomeTransactionCategoryCodes.first(),
                "",
                "",
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
                TransactionType.EXPENSE,
                expenseTransactionCategoryCodes.first(),
                "기존 매장",
                "3000",
            )

        assertEquals("기존 매장", filled.storeName)
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
