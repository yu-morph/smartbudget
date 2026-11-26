package com.yu_morph.smartbudget.ui.main

import com.yu_morph.smartbudget.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisAgeGroupTest {
    @Test
    fun classifiesDecadeBoundaries() {
        assertEquals(R.string.analysis_age_group_early, analysisAgeGroupResource(20))
        assertEquals(R.string.analysis_age_group_early, analysisAgeGroupResource(23))
        assertEquals(R.string.analysis_age_group_mid, analysisAgeGroupResource(24))
        assertEquals(R.string.analysis_age_group_mid, analysisAgeGroupResource(26))
        assertEquals(R.string.analysis_age_group_late, analysisAgeGroupResource(27))
        assertEquals(R.string.analysis_age_group_late, analysisAgeGroupResource(29))
        assertEquals(R.string.analysis_age_group_early, analysisAgeGroupResource(30))
    }
}
