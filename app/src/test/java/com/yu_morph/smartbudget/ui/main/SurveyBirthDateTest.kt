package com.yu_morph.smartbudget.ui.main

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class SurveyBirthDateTest {
    @Test
    fun cursorOffsetsRoundTripAcrossSeparators() {
        for (length in 0..8) {
            val transformed = SurveyBirthDateTransformation.filter(AnnotatedString("20000101".take(length)))
            for (offset in 0..length) {
                val mapped = transformed.offsetMapping.originalToTransformed(offset)
                assertEquals(offset, transformed.offsetMapping.transformedToOriginal(mapped))
            }
            assertEquals(
                transformed.text.length,
                transformed.offsetMapping.originalToTransformed(length),
            )
        }
    }

    @Test
    fun formatsTypedAndPastedDates() {
        assertEquals("2000-01-01", formatSurveyBirthDate("20000101"))
        assertEquals("2000-01-01", formatSurveyBirthDate("2000-01-01"))
        assertEquals("2000-0", formatSurveyBirthDate("20000"))
        assertEquals("", formatSurveyBirthDate(""))
    }
}
