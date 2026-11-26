package com.yu_morph.smartbudget.ui.main

import com.yu_morph.smartbudget.data.remote.model.RequestReportRequest
import com.yu_morph.smartbudget.data.remote.news.NewsArticle
import java.io.StringReader
import java.time.DateTimeException
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.ZoneId
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

internal data class AnalysisCategoryInsight(val name: String, val occurrence: Int?, val tip: String)

internal data class AnalysisReportContent(
    val aWordFromAi: String? = null,
    val topSpendingCategories: List<AnalysisCategoryInsight> = emptyList(),
    val spendingHealth: String? = null,
    val spendingByDayOfWeek: String? = null,
    val tipsForSaving: String? = null,
    val tipsToKeepItUp: String? = null,
    val tipsForInterests: String? = null,
    val newsDrivenInsights: String? = null,
) {
    val hasReadableSection: Boolean
        get() =
            aWordFromAi != null ||
                topSpendingCategories.isNotEmpty() ||
                spendingHealth != null ||
                spendingByDayOfWeek != null ||
                tipsForSaving != null ||
                tipsToKeepItUp != null ||
                tipsForInterests != null ||
                newsDrivenInsights != null
}

internal object AnalysisReportParser {
    fun parse(rawReport: String): AnalysisReportContent? = runCatching {
        require(!rawReport.contains("<!DOCTYPE", ignoreCase = true))
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            setFeatureIfSupported("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeatureIfSupported("http://xml.org/sax/features/external-general-entities", false)
            setFeatureIfSupported("http://xml.org/sax/features/external-parameter-entities", false)
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(rawReport)))
        val report = document.documentElement.takeIf { (it.localName ?: it.nodeName) == "report" }
            ?: return null
        AnalysisReportContent(
            aWordFromAi = report.directChildText("a_word_from_ai"),
            topSpendingCategories = report.categoryInsights(),
            spendingHealth = report.directChildText("spending_health"),
            spendingByDayOfWeek = report.directChildText("spending_by_day_of_the_week"),
            tipsForSaving = report.directChildText("tips_for_saving"),
            tipsToKeepItUp = report.directChildText("tips_to_keep_it_up"),
            tipsForInterests = report.directChildText("tips_for_your_interests"),
            newsDrivenInsights = report.directChildText("news_driven_insights"),
        ).takeIf(AnalysisReportContent::hasReadableSection)
    }.getOrNull()

    private fun Element.categoryInsights(): List<AnalysisCategoryInsight> {
        val container = directChild("top_spending_categories") ?: return emptyList()
        val categories = container.childNodes
        return (0 until categories.length).asSequence()
            .mapNotNull { categories.item(it) as? Element }
            .filter { (it.localName ?: it.nodeName) == "category" }
            .mapNotNull { category ->
                val name = category.getAttribute("name").trim()
                val tip = category.getAttribute("tip").trim()
                if (name.isNotBlank() || tip.isNotBlank()) {
                    AnalysisCategoryInsight(
                        name = name,
                        occurrence = category.getAttribute("occurrence").toIntOrNull(),
                        tip = tip,
                    )
                } else {
                    null
                }
            }
            .take(MAX_CATEGORY_COUNT)
            .toList()
    }

    private fun Element.directChildText(name: String): String? =
        directChild(name)?.textContent?.replace(Regex("\\s+"), " ")?.trim()?.takeIf(String::isNotBlank)

    private fun Element.directChild(name: String): Element? {
        val children = childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child.nodeType == Node.ELEMENT_NODE && (child.localName ?: child.nodeName) == name) {
                return child as Element
            }
        }
        return null
    }

    private fun DocumentBuilderFactory.setFeatureIfSupported(feature: String, enabled: Boolean) {
        // Android 버전에 따라 일부 XML 보안 기능을 지원하지 않으므로 원문 검사와 함께 적용한다.
        runCatching { setFeature(feature, enabled) }
    }

    private const val MAX_CATEGORY_COUNT = 3
}

internal fun buildMonthlyReportRequest(
    month: YearMonth,
    survey: AnalysisSurvey,
    newsTitles: List<String>,
    today: LocalDate,
): RequestReportRequest = RequestReportRequest(
    periodType = "monthly",
    periodStart = month.atDay(1).toString(),
    gender = when (survey.gender) {
        "male" -> "남성"
        "female" -> "여성"
        else -> survey.gender.trim().ifBlank { null }
    },
    age = ageAt(survey.birthDate, today),
    job = survey.job.trim().ifBlank { null },
    company = survey.workplace.trim().ifBlank { null },
    education = survey.education.trim().ifBlank { null },
    loan = if (survey.hasLoan) survey.loanDetails.trim().ifBlank { null } else "없음",
    approach = if (survey.managementDirection == "maintain") "유지" else "절약",
    interest = survey.interest.trim().ifBlank { null },
    news = newsTitles.takeIf(List<String>::isNotEmpty),
)

internal fun ageAt(birthDate: String, today: LocalDate): Int? = try {
    val age = Period.between(LocalDate.parse(birthDate.trim()), today).years
    age.takeIf { it in MIN_ANALYSIS_AGE..MAX_ANALYSIS_AGE }
} catch (_: DateTimeException) {
    null
}

internal fun todayNewsTitles(
    articles: List<NewsArticle>,
    today: LocalDate,
    zoneId: ZoneId = ZoneId.of("Asia/Seoul"),
): List<String> = articles.asSequence()
    .filter { article -> article.publishedAt?.atZone(zoneId)?.toLocalDate() == today }
    .map(NewsArticle::title)
    .map(String::trim)
    .filter(String::isNotBlank)
    .distinct()
    .toList()

private const val MIN_ANALYSIS_AGE = 10
private const val MAX_ANALYSIS_AGE = 150

internal fun surveyAge(birthDate: String, today: LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul"))): Int? =
    runCatching { LocalDate.parse(birthDate) }.getOrNull()
        ?.takeUnless { it.isAfter(today) }
        ?.let { Period.between(it, today).years }
