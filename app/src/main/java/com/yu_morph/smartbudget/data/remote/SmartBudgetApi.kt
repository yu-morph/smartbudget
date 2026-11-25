package com.yu_morph.smartbudget.data.remote

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.yu_morph.smartbudget.data.local.PreferenceStoreFactory
import com.yu_morph.smartbudget.data.local.RemoteResourceStore
import com.yu_morph.smartbudget.data.remote.api.AuthApi
import com.yu_morph.smartbudget.data.remote.api.MockTransactionApi
import com.yu_morph.smartbudget.data.remote.api.MonthlyBudgetApi
import com.yu_morph.smartbudget.data.remote.api.OcrApi
import com.yu_morph.smartbudget.data.remote.api.ReportApi
import com.yu_morph.smartbudget.data.remote.api.TransactionApi
import com.yu_morph.smartbudget.data.remote.api.VersionApi
import io.ktor.client.HttpClient

class SmartBudgetApi internal constructor(
    val version: VersionApi,
    val auth: AuthApi,
    val transactions: TransactionApi,
    val monthlyBudgets: MonthlyBudgetApi,
    val mockTransactions: MockTransactionApi,
    val reports: ReportApi,
    val ocr: OcrApi,
    val remoteResources: RemoteResourceStore,
    private val client: HttpClient,
) : AutoCloseable {
    override fun close() = client.close()
}

object SmartBudgetApiFactory {
    private const val API_LOG_TAG = "SmartBudgetAPI"
    private const val LOG_CHUNK_LENGTH = 900
    const val DEVELOPMENT_BASE_URL =
        "https://smartbudget-dev-kc-260925.koreacentral.cloudapp.azure.com"
    const val MOCK_TRANSACTION_BASE_URL =
        "https://smartbudget-mock-api-yoon-20260918.azurewebsites.net"

    fun create(context: Context, baseUrl: String = DEVELOPMENT_BASE_URL): SmartBudgetApi {
        val json = SmartBudgetHttpClientFactory.defaultJson()
        val client = SmartBudgetHttpClientFactory.create(json)
        val tokenStore = PreferenceStoreFactory.authTokens(context)
        val executor =
            ApiRequestExecutor(
                baseUrl = baseUrl,
                client = client,
                tokenStore = tokenStore,
                responseCache = PreferenceStoreFactory.responseCache(context),
                json = json,
                failureLogger = if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                    { message -> message.chunked(LOG_CHUNK_LENGTH).forEach { Log.d(API_LOG_TAG, it) } }
                } else {
                    null
                },
            )
        return SmartBudgetApi(
            version = VersionApi(executor),
            auth = AuthApi(executor, tokenStore),
            transactions = TransactionApi(executor),
            monthlyBudgets = MonthlyBudgetApi(executor),
            mockTransactions = MockTransactionApi(client, json, MOCK_TRANSACTION_BASE_URL),
            reports = ReportApi(executor),
            ocr = OcrApi(executor),
            remoteResources = PreferenceStoreFactory.remoteResources(context),
            client = client,
        )
    }
}
