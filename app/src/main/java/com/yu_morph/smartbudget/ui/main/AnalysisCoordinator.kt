package com.yu_morph.smartbudget.ui.main

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yu_morph.smartbudget.data.local.AnalysisSurveyStore
import com.yu_morph.smartbudget.data.local.PreferenceStoreFactory
import com.yu_morph.smartbudget.data.local.StoredAnalysisSurvey
import com.yu_morph.smartbudget.data.local.UserIdentityStore
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
    INVALID_REQUEST,
    AUTHENTICATION,
    NO_TRANSACTIONS,
    IN_PROGRESS,
    PROVIDER,
    TIMEOUT,
    SERVICE,
    UNREADABLE_REPORT,
}

internal data class AnalysisUiState(
    val survey: AnalysisSurvey = AnalysisSurvey(),
    val reports: List<ReportSummaryData> = emptyList(),
    val activeReport: ReportDetailData? = null,
    val activeContent: AnalysisReportContent? = null,
    val isLoadingHistory: Boolean = false,
    val hasMoreReports: Boolean = false,
    val historyFailure: AnalysisFailure? = null,
    val reportGoals: Map<Int, String> = emptyMap(),
    val isLoadingReport: Boolean = false,
    val isGenerating: Boolean = false,
    val failure: AnalysisFailure? = null,
)

@Suppress("TooManyFunctions")
internal class AnalysisCoordinator(
    context: Context,
    private val api: SmartBudgetApi,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.system(KOREA_ZONE),
    private val newsRepository: NewsFeedRepository = NewsFeedRepository(context.applicationContext),
    private val userIdentityStore: UserIdentityStore = PreferenceStoreFactory.userIdentity(context.applicationContext),
    private val surveyStore: AnalysisSurveyStore = PreferenceStoreFactory.analysisSurveys(context.applicationContext),
) : Closeable {
    private var historyJob: Job? = null
    private var reportJob: Job? = null
    private val reportGoals = context.applicationContext.getSharedPreferences(
        "analysis_report_goals",
        Context.MODE_PRIVATE,
    )
    private var historyRequested = false
    private var historyPage = MonthlyReportPage()
    private var surveyOwner: String? = null

    var state by mutableStateOf(AnalysisUiState())
        private set

    fun loadHistory(force: Boolean = false) {
        restoreSurvey()
        if (force) historyJob?.cancel()
        if (force || (!historyRequested && historyJob?.isActive != true)) loadHistoryPage(replace = true)
    }

    fun loadMoreHistory() {
        if (state.hasMoreReports && historyJob?.isActive != true) loadHistoryPage(replace = false)
    }

    private fun loadHistoryPage(replace: Boolean) {
        historyJob = scope.launch {
            state = state.copy(isLoadingHistory = true, historyFailure = null)
            try {
                val previousPage = if (replace) MonthlyReportPage() else historyPage
                val page = previousPage.load { query -> api.reports.list(query) }
                historyPage = page
                val existing = if (replace) emptyList() else state.reports
                val reports = (existing + page.visible).distinctBy { it.id }
                historyRequested = true
                state = state.copy(
                    reports = reports,
                    hasMoreReports = page.buffered.isNotEmpty(),
                    reportGoals = readReportGoals(reports),
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ReportHistoryException) {
                state = state.copy(historyFailure = exception.failure)
            } catch (_: Exception) {
                state = state.copy(historyFailure = AnalysisFailure.SERVICE)
            } finally {
                state = state.copy(isLoadingHistory = false)
            }
        }
    }

    private fun readReportGoals(reports: List<ReportSummaryData>): Map<Int, String> {
        val username = userIdentityStore.readUsername() ?: return emptyMap()
        return reports.mapNotNull { report ->
            val goal = reportGoals.getString(reportGoalKey(username, report.id, report.generatedAt), null)
            goal?.let { report.id to it }
        }.toMap()
    }

    private fun rememberReportGoal(report: ReportDetailData, survey: AnalysisSurvey) {
        val username = userIdentityStore.readUsername() ?: return
        reportGoals.edit()
            .putString(reportGoalKey(username, report.id, report.generatedAt), survey.managementDirection)
            .apply()
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
                val result = api.reports.requestReport(request, UUID.randomUUID().toString())
                (result as? ApiResult.Success)?.data?.let { rememberReportGoal(it, survey) }
                applyReportResult(result)
                if (result is ApiResult.Success) loadHistory(force = true)
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

    fun saveSurvey(survey: AnalysisSurvey) {
        state = state.copy(survey = survey)
        userIdentityStore.readUsername()?.let { username ->
            surveyStore.write(username, survey.toStoredSurvey())
            surveyOwner = username
        }
    }

    fun reset() {
        historyJob?.cancel()
        reportJob?.cancel()
        historyRequested = false
        historyPage = MonthlyReportPage()
        surveyOwner = null
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

    private fun restoreSurvey() {
        val username = userIdentityStore.readUsername()
        if (username == surveyOwner) return
        surveyOwner = username
        val survey = username?.let(surveyStore::read)?.toAnalysisSurvey() ?: AnalysisSurvey()
        state = state.copy(survey = survey)
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

    private companion object {
        val KOREA_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}

internal fun ApiResult<*>.toAnalysisFailure(): AnalysisFailure = when (this) {
    is ApiResult.NetworkFailure -> AnalysisFailure.NETWORK

    is ApiResult.DecodeFailure -> AnalysisFailure.RESPONSE

    is ApiResult.NotImplemented -> AnalysisFailure.SERVICE

    is ApiResult.Success -> AnalysisFailure.SERVICE

    is ApiResult.HttpFailure -> when (code) {
        "INVALID_REQUEST" -> AnalysisFailure.INVALID_REQUEST

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

private fun AnalysisSurvey.toStoredSurvey(): StoredAnalysisSurvey = StoredAnalysisSurvey(
    gender = gender,
    birthDate = birthDate,
    job = job,
    workplace = workplace,
    education = education,
    hasLoan = hasLoan,
    loanDetails = loanDetails,
    managementDirection = managementDirection,
    interest = interest,
)

private fun StoredAnalysisSurvey.toAnalysisSurvey(): AnalysisSurvey = AnalysisSurvey(
    gender = gender,
    birthDate = birthDate,
    job = job,
    workplace = workplace,
    education = education,
    hasLoan = hasLoan,
    loanDetails = loanDetails,
    managementDirection = managementDirection,
    interest = interest,
)
