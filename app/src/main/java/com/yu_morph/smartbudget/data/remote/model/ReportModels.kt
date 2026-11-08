package com.yu_morph.smartbudget.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportSummaryData(
    val id: Int,
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("period_type") val periodType: String,
    @SerialName("period_start") val periodStart: String,
    @SerialName("period_end") val periodEnd: String,
    @SerialName("is_stale") val isStale: Boolean,
)

@Serializable
data class ReportPageData(
    val items: List<ReportSummaryData>,
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class ReportDetailData(
    val id: Int,
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("period_type") val periodType: String,
    @SerialName("period_start") val periodStart: String,
    @SerialName("period_end") val periodEnd: String,
    @SerialName("is_stale") val isStale: Boolean,
    @SerialName("full_report") val fullReport: String,
)

@Serializable
data class RequestReportRequest(
    @SerialName("period_type") val periodType: String,
    @SerialName("period_start") val periodStart: String,
)
