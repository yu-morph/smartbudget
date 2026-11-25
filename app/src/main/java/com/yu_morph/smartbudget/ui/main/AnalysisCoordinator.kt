package com.yu_morph.smartbudget.ui.main

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.SmartBudgetApi
import com.yu_morph.smartbudget.data.remote.model.ReportDetailData
import com.yu_morph.smartbudget.data.remote.model.ReportSummaryData
import com.yu_morph.smartbudget.data.remote.news.NewsArticle
import com.yu_morph.smartbudget.data.remote.news.NewsFeedRepository
import java.io.Closeable
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal enum class AnalysisFailure {
    NETWORK,
    RESPONSE,
    AUTHENTICATION,
    NO_TRANSACTIONS,
    IN_PROGRESS,
    PROVIDER,
    TIMEOUT,
    SERVICE,
    UNREADABLE_REPORT,
}

internal data class AnalysisUiState(
    val reports: List<ReportSummaryData> = emptyList(),
    val activeReport: ReportDetailData? = null,
    val activeContent: AnalysisReportContent? = null,
    val isLoadingHistory: Boolean = false,
    val isLoadingReport: Boolean = false,
    val isGenerating: Boolean = false,
    val failure: AnalysisFailure? = null,
)

internal class AnalysisCoordinator(
    context: Context,
    private val api: SmartBudgetApi,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.system(KOREA_ZONE),
    private val newsRepository: NewsFeedRepository = NewsFeedRepository(context.applicationContext),
) : Closeable {
    private var historyJob: Job? = null
    private var reportJob: Job? = null
    private var historyRequested = false

    var state by mutableStateOf(AnalysisUiState())
        private set

    fun loadHistory(force: Boolean = false) {
        if (force) {
            historyJob?.cancel()
        } else if (historyRequested || historyJob?.isActive == true) {
            return
        }
        historyRequested = true
        historyJob = scope.launch {
            state = state.copy(isLoadingHistory = true, failure = null)
            try {
                when (val result = api.reports.list()) {
                    is ApiResult.Success -> state = state.copy(
                        reports = result.data?.items.orEmpty().filter { it.periodType == MONTHLY_PERIOD_TYPE },
                    )

                    else -> state = state.copy(failure = result.toAnalysisFailure())
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                state = state.copy(failure = AnalysisFailure.SERVICE)
            } finally {
                state = state.copy(isLoadingHistory = false)
            }
        }
    }

    fun generate(month: YearMonth, survey: AnalysisSurvey) {
        if (state.isGenerating || state.isLoadingReport) return
        state = state.copy(
            activeReport = null,
            activeContent = null,
            isGenerating = true,
            failure = null,
        )
        reportJob = scope.launch {
            try {
                val today = LocalDate.now(clock)
                val news = loadTodayNews(today)
                val request = buildMonthlyReportRequest(month, survey, news, today)
                applyReportResult(api.reports.requestReport(request, UUID.randomUUID().toString()))
                if (state.activeReport != null) loadHistory(force = true)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                state = state.copy(failure = AnalysisFailure.SERVICE)
            } finally {
                state = state.copy(isGenerating = false)
            }
        }
    }

    fun openReport(id: Int) {
        if (state.isGenerating || state.isLoadingReport) return
        state = state.copy(
            activeReport = null,
            activeContent = null,
            isLoadingReport = true,
            failure = null,
        )
        reportJob = scope.launch {
            try {
                applyReportResult(api.reports.get(id))
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                state = state.copy(failure = AnalysisFailure.SERVICE)
            } finally {
                state = state.copy(isLoadingReport = false)
            }
        }
    }

    fun clearFailure() {
        state = state.copy(failure = null)
    }

    fun reset() {
        historyJob?.cancel()
        reportJob?.cancel()
        historyRequested = false
        api.reports.invalidateCache()
        state = AnalysisUiState()
    }

    override fun close() {
        historyJob?.cancel()
        reportJob?.cancel()
        newsRepository.close()
    }

    private fun applyReportResult(result: ApiResult<ReportDetailData>) {
        when (result) {
            is ApiResult.Success -> {
                val report = result.data
                val content = report?.fullReport?.let(AnalysisReportParser::parse)
                state = if (report != null && content != null) {
                    state.copy(activeReport = report, activeContent = content, failure = null)
                } else {
                    state.copy(failure = AnalysisFailure.UNREADABLE_REPORT)
                }
            }

            else -> state = state.copy(failure = result.toAnalysisFailure())
        }
    }

    private suspend fun loadTodayNews(today: LocalDate): List<String> {
        val articles = supervisorScope {
            val breaking = async { loadNewsSafely(newsRepository::loadBreakingNews) }
            val economy = async { loadNewsSafely(newsRepository::loadEconomyNews) }
            breaking.await() + economy.await()
        }
        return todayNewsTitles(articles, today, KOREA_ZONE)
    }

    private suspend fun loadNewsSafely(request: suspend () -> List<NewsArticle>): List<NewsArticle> = try {
        request()
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        emptyList()
    }

    private fun ApiResult<*>.toAnalysisFailure(): AnalysisFailure = when (this) {
        is ApiResult.NetworkFailure -> AnalysisFailure.NETWORK

        is ApiResult.DecodeFailure -> AnalysisFailure.RESPONSE

        is ApiResult.NotImplemented -> AnalysisFailure.SERVICE

        is ApiResult.Success -> AnalysisFailure.SERVICE

        is ApiResult.HttpFailure -> when (code) {
            "AUTHENTICATION_REQUIRED" -> AnalysisFailure.AUTHENTICATION

            "REPORT_NO_TRANSACTIONS" -> AnalysisFailure.NO_TRANSACTIONS

            "IDEMPOTENCY_REQUEST_IN_PROGRESS",
            "REPORT_GENERATION_IN_PROGRESS",
            -> AnalysisFailure.IN_PROGRESS

            "REPORT_PROVIDER_TIMEOUT" -> AnalysisFailure.TIMEOUT

            "REPORT_PROVIDER_ERROR" -> AnalysisFailure.PROVIDER

            else -> AnalysisFailure.SERVICE
        }
    }

    private companion object {
        val KOREA_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        const val MONTHLY_PERIOD_TYPE = "monthly"
    }
}
