@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.Male
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R

@Composable
internal fun AnalysisSurveySummary(
    survey: AnalysisSurvey,
    displayName: String,
    profileImageUri: String?,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AnalysisPanel(onClick = onEdit) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileImage(imageUri = profileImageUri, size = 42.dp, shape = RoundedCornerShape(14.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (displayName.isNotBlank()) {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    SurveyDemographics(survey)
                }
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(R.string.analysis_edit_survey),
                    tint = colors.onSurfaceVariant,
                )
            }
            SurveyAttributes(survey)
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.2f))
            SurveyGoals(survey)
        }
    }
}

@Composable
private fun SurveyDemographics(survey: AnalysisSurvey) {
    val age = surveyAge(survey.birthDate)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (survey.gender == "male" || survey.gender == "female") {
            val male = survey.gender == "male"
            Icon(
                if (male) Icons.Outlined.Male else Icons.Outlined.Female,
                stringResource(if (male) R.string.analysis_survey_male else R.string.analysis_survey_female),
                modifier = Modifier.size(16.dp),
                tint = if (male) Color(0xFF2879CB) else Color(0xFFD04459),
            )
        }
        if (age != null) {
            Text(
                text = stringResource(analysisAgeGroupResource(age), age / 10 * 10),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

internal fun analysisAgeGroupResource(age: Int): Int = when (age % 10) {
    in 0..3 -> R.string.analysis_age_group_early
    in 4..6 -> R.string.analysis_age_group_mid
    else -> R.string.analysis_age_group_late
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SurveyAttributes(survey: AnalysisSurvey) {
    if (listOf(survey.job, survey.workplace, survey.education).any { it.isNotBlank() }) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SurveyAttribute(Icons.Outlined.WorkOutline, R.string.analysis_survey_job, survey.job)
            SurveyAttribute(Icons.Outlined.Business, R.string.analysis_survey_workplace, survey.workplace)
            SurveyAttribute(Icons.Outlined.School, R.string.analysis_survey_education, survey.education)
        }
    }
}

@Composable
private fun SurveyAttribute(icon: ImageVector, label: Int, value: String) {
    if (value.isNotBlank()) {
        val colors = MaterialTheme.colorScheme
        Row(
            modifier = Modifier.background(colors.surfaceVariant.copy(alpha = 0.32f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, stringResource(label), modifier = Modifier.size(14.dp), tint = colors.onSurfaceVariant)
            Text(
                value,
                modifier = Modifier.widthIn(max = 144.dp),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SurveyGoals(survey: AnalysisSurvey) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SurveyFact(
                R.string.analysis_survey_goal_label,
                stringResource(
                    if (survey.managementDirection == "maintain") {
                        R.string.analysis_survey_maintain
                    } else {
                        R.string.analysis_survey_save
                    },
                ),
                Modifier.weight(1f),
            )
            SurveyFact(
                R.string.analysis_survey_loan,
                stringResource(if (survey.hasLoan) R.string.analysis_survey_yes else R.string.analysis_survey_no),
                Modifier.weight(1f),
            )
        }
        if (survey.interest.isNotBlank()) {
            Text(
                text = stringResource(R.string.analysis_survey_interest_summary, survey.interest),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SurveyFact(label: Int, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(label),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
