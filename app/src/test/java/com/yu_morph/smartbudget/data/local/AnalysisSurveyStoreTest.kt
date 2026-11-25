package com.yu_morph.smartbudget.data.local

import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalysisSurveyStoreTest {
    @Test
    fun `설문 저장 형식은 모든 응답을 복원한다`() {
        val json = analysisSurveyJson()
        val survey =
            StoredAnalysisSurvey(
                gender = "female",
                birthDate = "1995-05-20",
                job = "개발자",
                workplace = "스마트버짓",
                education = "대학교 졸업",
                hasLoan = true,
                loanDetails = "학자금 대출 5,000,000원",
                managementDirection = "save",
                interest = "식비 절약",
            )

        val restored = decodeAnalysisSurvey(json.encodeToString(survey), json)

        assertEquals(survey, restored)
    }

    @Test
    fun `알 수 없는 필드는 무시하고 손상된 설문은 사용하지 않는다`() {
        val rawValue =
            """{"gender":"male","managementDirection":"maintain","futureField":"value"}"""

        assertEquals("male", decodeAnalysisSurvey(rawValue)?.gender)
        assertEquals("maintain", decodeAnalysisSurvey(rawValue)?.managementDirection)
        assertNull(decodeAnalysisSurvey("{broken"))
    }

    @Test
    fun `계정마다 서로 다른 설문 저장 키를 사용한다`() {
        assertNotEquals(analysisSurveyKey("first-user"), analysisSurveyKey("second-user"))
    }
}
