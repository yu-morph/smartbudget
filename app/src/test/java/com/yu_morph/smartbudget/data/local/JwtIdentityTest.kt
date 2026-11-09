package com.yu_morph.smartbudget.data.local

import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JwtIdentityTest {
    @Test
    fun `JWT의 subject를 사용자 ID로 읽는다`() {
        val token = jwtWithPayload("""{"sub":"42","exp":9999999999}""")

        assertEquals("42", readSubjectFromJwt(token))
    }

    @Test
    fun `subject가 없거나 손상된 JWT는 사용자 ID를 반환하지 않는다`() {
        assertNull(readSubjectFromJwt(jwtWithPayload("""{"exp":9999999999}""")))
        assertNull(readSubjectFromJwt("손상된.JWT"))
        assertNull(readSubjectFromJwt("header.@@@.signature"))
    }

    private fun jwtWithPayload(payload: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"none"}""".toByteArray(StandardCharsets.UTF_8))
        val encodedPayload = encoder.encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
        return "$header.$encodedPayload.signature"
    }
}
