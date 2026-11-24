package com.yu_morph.smartbudget.data.remote.model

import com.yu_morph.smartbudget.data.remote.SmartBudgetHttpClientFactory
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchAndAccountContractTest {
    private val json = SmartBudgetHttpClientFactory.defaultJson()

    @Test
    fun `거래 일괄 생성 요청은 items 배열과 snake case 필드를 사용한다`() {
        val request =
            BatchCreateTransactionsRequest(
                items =
                    listOf(
                        CreateTransactionRequest(
                            date = "2026-11-24",
                            type = TransactionType.EXPENSE,
                            source = TransactionSource.FINANCIAL_SYNC,
                            category = "food",
                            storeName = "편의점",
                            amount = 10_000,
                        ),
                    ),
            )

        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains("\"items\""))
        assertTrue(encoded.contains("\"store_name\":\"편의점\""))
        assertTrue(encoded.contains("\"financial_sync\""))
    }

    @Test
    fun `거래 일괄 삭제 응답과 계정 프로필 이미지를 해석한다`() {
        val deleted =
            json.decodeFromString<BatchDeleteTransactionsData>(
                """{"deleted_count":12,"next_cursor":"next"}""",
            )
        val account =
            json.decodeFromString<AccountData>(
                """{"username":"user","display_name":"사용자","profile_image":"cG5n"}""",
            )

        assertEquals(12, deleted.deletedCount)
        assertEquals("next", deleted.nextCursor)
        assertEquals("cG5n", account.profileImage)
    }
}
