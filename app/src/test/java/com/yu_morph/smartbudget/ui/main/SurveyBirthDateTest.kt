package com.yu_morph.smartbudget.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class SurveyBirthDateTest {
    @Test
    fun formatsTypedAndPastedDates() {
        assertEquals("2000-01-01", formatSurveyBirthDate("20000101"))
        assertEquals("2000-01-01", formatSurveyBirthDate("2000-01-01"))
        assertEquals("2000-0", formatSurveyBirthDate("20000"))
        assertEquals("", formatSurveyBirthDate(""))
    }
}
