package com.yu_morph.smartbudget.ui.main

import com.yu_morph.smartbudget.data.remote.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisFailureTest {
    @Test
    fun distinguishesInvalidRequestFromNoTransactions() {
        assertEquals(AnalysisFailure.INVALID_REQUEST, failure(400, "INVALID_REQUEST"))
        assertEquals(AnalysisFailure.NO_TRANSACTIONS, failure(400, "REPORT_NO_TRANSACTIONS"))
    }

    @Test
    fun distinguishesAuthenticationAndProviderFailures() {
        assertEquals(AnalysisFailure.AUTHENTICATION, failure(401, "AUTHENTICATION_REQUIRED"))
        assertEquals(AnalysisFailure.PROVIDER, failure(502, "REPORT_PROVIDER_ERROR"))
        assertEquals(AnalysisFailure.TIMEOUT, failure(504, "REPORT_PROVIDER_TIMEOUT"))
    }

    private fun failure(status: Int, code: String): AnalysisFailure =
        ApiResult.HttpFailure(status, code, "", null).toAnalysisFailure()
}
