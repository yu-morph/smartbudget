package com.yu_morph.smartbudget.notification

import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisReminderPolicyTest {
    @Test
    fun schedulesActualLastDayOfEachMonth() {
        assertNext("2024-02-01T12:00+09:00", "2024-02-29T19:00+09:00")
        assertNext("2026-02-01T12:00+09:00", "2026-02-28T19:00+09:00")
        assertNext("2026-11-30T19:00+09:00", "2026-12-31T19:00+09:00")
        assertNext("2026-12-31T20:00+09:00", "2027-01-31T19:00+09:00")
    }

    @Test
    fun delayedNotificationIsOnlyDueOnTheLastDay() {
        assertTrue(isAnalysisReminderDue(ZonedDateTime.parse("2026-11-30T21:00+09:00")))
        assertFalse(isAnalysisReminderDue(ZonedDateTime.parse("2026-11-30T18:59+09:00")))
        assertFalse(isAnalysisReminderDue(ZonedDateTime.parse("2026-12-01T09:00+09:00")))
    }

    private fun assertNext(now: String, expected: String) {
        assertEquals(ZonedDateTime.parse(expected), analysisReminderTime(ZonedDateTime.parse(now)))
    }
}
