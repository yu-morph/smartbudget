package com.yu_morph.smartbudget.data.remote

import android.content.Context
import com.yu_morph.smartbudget.data.local.PreferenceStoreFactory
import com.yu_morph.smartbudget.data.local.RemoteResourceStore
import com.yu_morph.smartbudget.data.remote.api.AuthApi
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
    val reports: ReportApi,
    val ocr: OcrApi,
    val remoteResources: RemoteResourceStore,
    private val client: HttpClient,
) : AutoCloseable {
    override fun close() = client.close()
}

object SmartBudgetApiFactory {
    const val DEVELOPMENT_BASE_URL =
        "https://smartbudget-dev-kc-260925.koreacentral.cloudapp.azure.com"

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
            )
        return SmartBudgetApi(
            version = VersionApi(executor),
            auth = AuthApi(executor, tokenStore),
            transactions = TransactionApi(executor),
            monthlyBudgets = MonthlyBudgetApi(executor),
            reports = ReportApi(executor),
            ocr = OcrApi(),
            remoteResources = PreferenceStoreFactory.remoteResources(context),
            client = client,
        )
    }
}
