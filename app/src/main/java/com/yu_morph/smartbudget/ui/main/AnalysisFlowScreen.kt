@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.auth.PrimaryAuthButton
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import kotlinx.coroutines.delay

internal enum class AnalysisFlowStep { SURVEY, REQUEST, LOADING, RESULT }

internal data class AnalysisSurvey(
    val gender: String = "",
    val birthDate: String = "",
    val job: String = "",
    val workplace: String = "",
    val education: String = "",
    val hasLoan: Boolean = false,
    val loanDetails: String = "",
    val managementDirection: String = "save",
    val interest: String = "",
)

private data class AnalysisRequestInput(val year: Int, val month: Int, val survey: AnalysisSurvey)

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun AnalysisFlowScreen(
    bottomPadding: Dp,
    initialSurvey: AnalysisSurvey,
    initialStep: AnalysisFlowStep,
    initialYear: Int,
    initialMonth: Int,
    continueAfterSurvey: Boolean,
    onBack: () -> Unit,
    onSurveySaved: (AnalysisSurvey) -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(initialStep) }
    var gender by rememberSaveable { mutableStateOf(initialSurvey.gender) }
    var birthDate by rememberSaveable { mutableStateOf(initialSurvey.birthDate) }
    var job by rememberSaveable { mutableStateOf(initialSurvey.job) }
    var workplace by rememberSaveable { mutableStateOf(initialSurvey.workplace) }
    var education by rememberSaveable { mutableStateOf(initialSurvey.education) }
    var hasLoan by rememberSaveable { mutableStateOf(initialSurvey.hasLoan) }
    var loanDetails by rememberSaveable { mutableStateOf(initialSurvey.loanDetails) }
    var direction by rememberSaveable { mutableStateOf(initialSurvey.managementDirection.ifBlank { "save" }) }
    var interest by rememberSaveable { mutableStateOf(initialSurvey.interest) }
    var year by rememberSaveable { mutableStateOf(initialYear) }
    var month by rememberSaveable { mutableStateOf(initialMonth) }
    var submittedRequest by remember { mutableStateOf<AnalysisRequestInput?>(null) }

    BackHandler {
        step = when (step) {
            AnalysisFlowStep.REQUEST -> AnalysisFlowStep.SURVEY

            AnalysisFlowStep.LOADING -> AnalysisFlowStep.REQUEST

            AnalysisFlowStep.RESULT -> if (initialStep == AnalysisFlowStep.RESULT) {
                onBack()
                AnalysisFlowStep.RESULT
            } else {
                AnalysisFlowStep.REQUEST
            }

            AnalysisFlowStep.SURVEY -> {
                onBack()
                AnalysisFlowStep.SURVEY
            }
        }
    }
    LaunchedEffect(step) {
        if (step == AnalysisFlowStep.LOADING) {
            delay(1_800)
            step = AnalysisFlowStep.RESULT
        }
    }

    when (step) {
        AnalysisFlowStep.SURVEY -> SurveyScreen(
            bottomPadding = bottomPadding,
            survey = AnalysisSurvey(
                gender, birthDate, job, workplace, education, hasLoan, loanDetails, direction, interest,
            ),
            onGenderChange = { gender = it },
            onBirthDateChange = { birthDate = it },
            onJobChange = { job = it },
            onWorkplaceChange = { workplace = it },
            onEducationChange = { education = it },
            onLoanChange = { hasLoan = it },
            onLoanDetailsChange = { loanDetails = it },
            onDirectionChange = { direction = it },
            onInterestChange = { interest = it },
            onBack = onBack,
            onSave = {
                val completedSurvey = AnalysisSurvey(
                    gender, birthDate, job, workplace, education, hasLoan, loanDetails, direction, interest,
                )
                onSurveySaved(completedSurvey)
                if (continueAfterSurvey) {
                    step = AnalysisFlowStep.REQUEST
                } else {
                    onBack()
                }
            },
        )

        AnalysisFlowStep.REQUEST -> AnalysisRequestScreen(
            bottomPadding = bottomPadding,
            year = year,
            month = month,
            onYearMonthChange = { newYear, newMonth ->
                year = newYear
                month = newMonth
            },
            onBack = { step = AnalysisFlowStep.SURVEY },
            onStart = {
                submittedRequest =
                    AnalysisRequestInput(
                        year = year,
                        month = month,
                        survey = AnalysisSurvey(
                            gender, birthDate, job, workplace, education, hasLoan, loanDetails, direction, interest,
                        ),
                    )
                step = AnalysisFlowStep.LOADING
            },
        )

        AnalysisFlowStep.LOADING -> AnalysisLoadingScreen(bottomPadding)

        AnalysisFlowStep.RESULT -> AnalysisResultScreen(
            bottomPadding = bottomPadding,
            year = submittedRequest?.year ?: year,
            month = submittedRequest?.month ?: month,
            interest = submittedRequest?.survey?.interest.orEmpty(),
            onBack = {
                if (initialStep == AnalysisFlowStep.RESULT) onBack() else step = AnalysisFlowStep.REQUEST
            },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun SurveyScreen(
    bottomPadding: Dp,
    survey: AnalysisSurvey,
    onGenderChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onJobChange: (String) -> Unit,
    onWorkplaceChange: (String) -> Unit,
    onEducationChange: (String) -> Unit,
    onLoanChange: (Boolean) -> Unit,
    onLoanDetailsChange: (String) -> Unit,
    onDirectionChange: (String) -> Unit,
    onInterestChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    MainTabContainer(bottomPadding) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
            Text(stringResource(R.string.analysis_custom_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.analysis_survey_heading), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.analysis_survey_privacy), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        SurveyLabel(R.string.analysis_survey_gender)
        ChoiceRow(
            labels = listOf(R.string.analysis_survey_male, R.string.analysis_survey_female),
            selected = survey.gender,
            values = listOf("male", "female"),
            onSelect = onGenderChange,
        )
        SurveyField(
            R.string.analysis_survey_birth_date,
            survey.birthDate,
            onBirthDateChange,
            R.string.analysis_survey_birth_hint,
        )
        SurveyField(R.string.analysis_survey_job, survey.job, onJobChange, R.string.analysis_survey_job_hint)
        SurveyField(
            R.string.analysis_survey_workplace,
            survey.workplace,
            onWorkplaceChange,
            R.string.analysis_survey_workplace_hint,
        )
        EducationField(survey.education, onEducationChange)
        SurveyLabel(R.string.analysis_survey_loan)
        ChoiceRow(
            labels = listOf(R.string.analysis_survey_yes, R.string.analysis_survey_no),
            selected = if (survey.hasLoan) "yes" else "no",
            values = listOf("yes", "no"),
            onSelect = { onLoanChange(it == "yes") },
        )
        if (survey.hasLoan) {
            SurveyField(
                R.string.analysis_survey_loan_details,
                survey.loanDetails,
                onLoanDetailsChange,
                R.string.analysis_survey_loan_hint,
                keyboardType = KeyboardType.Number,
            )
        }
        SurveyLabel(R.string.analysis_survey_direction)
        ChoiceRow(
            labels = listOf(R.string.analysis_survey_save, R.string.analysis_survey_maintain),
            selected = survey.managementDirection,
            values = listOf("save", "maintain"),
            onSelect = onDirectionChange,
        )
        SurveyField(
            R.string.analysis_survey_interest,
            survey.interest,
            onInterestChange,
            R.string.analysis_survey_interest_hint,
            minLines = 3,
        )
        Spacer(Modifier.height(12.dp))
        PrimaryAuthButton(text = stringResource(R.string.analysis_survey_save_button), onClick = onSave)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SurveyField(
    label: Int,
    value: String,
    onValueChange: (String) -> Unit,
    hint: Int,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SurveyLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            minLines = minLines,
            shape = RoundedCornerShape(14.dp),
        )
    }
}

@Composable
private fun SurveyLabel(label: Int) {
    Text(stringResource(label), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ChoiceRow(labels: List<Int>, values: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val isSelected = selected == values[index]
            Box(
                modifier = Modifier.weight(1f).height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) SmartBudgetGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    )
                    .border(
                        1.dp,
                        if (isSelected) SmartBudgetGreen else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(values[index]) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(label),
                    color = if (isSelected) SmartBudgetGreen else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun EducationField(value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        SurveyLabel(R.string.analysis_survey_education)
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                readOnly = true,
                placeholder = { Text(stringResource(R.string.analysis_survey_education_hint)) },
                trailingIcon = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
            )
            Box(Modifier.matchParentSize().clickable { expanded = true })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf(
                    stringResource(R.string.analysis_education_high_school),
                    stringResource(R.string.analysis_education_college),
                    stringResource(R.string.analysis_education_bachelor),
                    stringResource(R.string.analysis_education_master),
                    stringResource(R.string.analysis_education_doctorate),
                    stringResource(R.string.analysis_education_other),
                ).forEach { item ->
                    DropdownMenuItem(text = { Text(item) }, onClick = {
                        onChange(item)
                        expanded = false
                    })
                }
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun AnalysisRequestScreen(
    bottomPadding: Dp,
    year: Int,
    month: Int,
    onYearMonthChange: (Int, Int) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    MainTabContainer(bottomPadding) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
        }
        Text(stringResource(R.string.analysis_request_title), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.analysis_request_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Image(
            painterResource(R.drawable.home_greeting_robot),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(220.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.analysis_request_month), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = {
                    onYearMonthChange(
                        if (month ==
                            1
                        ) {
                            year - 1
                        } else {
                            year
                        },
                        if (month == 1) 12 else month - 1,
                    )
                }) {
                    Icon(
                        Icons.Outlined.ChevronLeft,
                        contentDescription = stringResource(R.string.analysis_previous_month),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = SmartBudgetGreen)
                    Text(
                        stringResource(R.string.year_month, year, month),
                        modifier = Modifier.padding(start = 10.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(onClick = {
                    onYearMonthChange(
                        if (month ==
                            12
                        ) {
                            year + 1
                        } else {
                            year
                        },
                        if (month == 12) 1 else month + 1,
                    )
                }) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.analysis_next_month))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        PrimaryAuthButton(text = stringResource(R.string.analysis_start), onClick = onStart)
    }
}

@Composable
private fun AnalysisLoadingScreen(bottomPadding: Dp) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.bank_sync_robot), contentDescription = null, modifier = Modifier.size(220.dp))
        Text(
            stringResource(R.string.analysis_loading_title),
            modifier = Modifier.padding(top = 22.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.analysis_loading_subtitle),
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            modifier = Modifier.padding(top = 24.dp).fillMaxWidth(0.72f).height(6.dp).clip(CircleShape),
            color = SmartBudgetGreen,
            trackColor = SmartBudgetGreen.copy(alpha = 0.16f),
        )
        Text(
            stringResource(R.string.analysis_loading_wait),
            modifier = Modifier.padding(top = 18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun AnalysisResultScreen(bottomPadding: Dp, year: Int, month: Int, interest: String, onBack: () -> Unit) {
    MainTabContainer(bottomPadding) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
            Text(
                stringResource(R.string.analysis_result_title, year, month),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        ResultCard(R.string.analysis_result_ai_title) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.analysis_result_ai_body),
                    modifier = Modifier.weight(1f),
                    lineHeight = 22.sp,
                )
                Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = SmartBudgetGreen)
            }
        }
        ResultCard(R.string.analysis_result_total_title) {
            Text(stringResource(R.string.analysis_result_total), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.analysis_result_change),
                color = Color(0xFFE2533F),
                fontWeight = FontWeight.Bold,
            )
        }
        ResultCard(R.string.analysis_result_categories_title) {
            ResultLine(
                R.string.analysis_result_food_category,
                R.string.analysis_result_food_count,
                R.string.analysis_result_food_note,
            )
            ResultLine(
                R.string.analysis_result_transport_category,
                R.string.analysis_result_transport_count,
                R.string.analysis_result_transport_note,
            )
            ResultLine(
                R.string.analysis_result_living_category,
                R.string.analysis_result_living_count,
                R.string.analysis_result_living_note,
            )
        }
        ResultCard(R.string.analysis_result_health_title) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HealthScore(modifier = Modifier.size(116.dp))
                Column(Modifier.padding(start = 20.dp)) {
                    Text(
                        stringResource(R.string.analysis_result_health_grade),
                        color = SmartBudgetGreen,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.analysis_result_health_body),
                        modifier = Modifier.padding(top = 8.dp),
                        lineHeight = 20.sp,
                    )
                }
            }
        }
        ResultCard(R.string.analysis_result_weekday_title) {
            WeekdayChart()
            Text(
                stringResource(R.string.analysis_result_weekday_note),
                modifier = Modifier.padding(top = 12.dp),
                color = SmartBudgetGreen,
                lineHeight = 20.sp,
            )
        }
        AdviceCard(
            R.string.analysis_result_save_advice,
            listOf(
                R.string.analysis_result_save_1,
                R.string.analysis_result_save_2,
                R.string.analysis_result_save_3,
            ),
        )
        AdviceCard(
            R.string.analysis_result_maintain_advice,
            listOf(
                R.string.analysis_result_maintain_1,
                R.string.analysis_result_maintain_2,
            ),
        )
        ResultCard(R.string.analysis_result_interest_title) {
            Text(
                if (interest.isBlank()) {
                    stringResource(R.string.analysis_result_interest_body)
                } else {
                    stringResource(R.string.analysis_result_interest_custom_body, interest)
                },
                lineHeight = 21.sp,
            )
        }
        ResultCard(R.string.analysis_result_news_title) {
            Text(stringResource(R.string.analysis_result_news_body), lineHeight = 21.sp)
        }
        Spacer(Modifier.height(16.dp))
    }
}
