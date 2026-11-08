package com.yu_morph.smartbudget.data.local

import java.time.OffsetDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredAuthTokenTest {
    private val now = OffsetDateTime.parse("2026-11-09T12:00:00+09:00")

    @Test
    fun `만료되지 않은 Bearer 토큰은 유효하다`() {
        val token = StoredAuthToken("jwt", "2026-11-10T12:00:00+09:00", "Bearer")

        assertTrue(token.isValidAt(now))
    }

    @Test
    fun `만료되거나 손상된 토큰은 유효하지 않다`() {
        assertFalse(StoredAuthToken("jwt", "2026-11-08T12:00:00+09:00", "Bearer").isValidAt(now))
        assertFalse(StoredAuthToken("jwt", "잘못된 시각", "Bearer").isValidAt(now))
        assertFalse(StoredAuthToken("", "2026-11-10T12:00:00+09:00", "Bearer").isValidAt(now))
        assertFalse(StoredAuthToken("jwt", "2026-11-10T12:00:00+09:00", "Basic").isValidAt(now))
    }
}
