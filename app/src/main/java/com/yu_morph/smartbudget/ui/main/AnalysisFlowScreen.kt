@file:Suppress("MagicNumber", "TooManyFunctions")

package com.yu_morph.smartbudget.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.Male
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.auth.PrimaryAuthButton
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import java.time.YearMonth
import java.time.ZoneId

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

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun AnalysisFlowScreen(
    analysisState: AnalysisUiState,
    initialSurvey: AnalysisSurvey,
    initialStep: AnalysisFlowStep,
    initialYear: Int,
    initialMonth: Int,
    continueAfterSurvey: Boolean,
    closeAfterResult: Boolean,
    onBack: () -> Unit,
    onSurveySaved: (AnalysisSurvey) -> Unit,
    onRequestReport: (YearMonth, AnalysisSurvey) -> Unit,
    onClearFailure: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(initialStep) }
    var gender by rememberSaveable { mutableStateOf(initialSurvey.gender.ifBlank { "male" }) }
    var birthDate by rememberSaveable { mutableStateOf(surveyBirthDateDigits(initialSurvey.birthDate)) }
    var job by rememberSaveable { mutableStateOf(initialSurvey.job) }
    var workplace by rememberSaveable { mutableStateOf(initialSurvey.workplace) }
    var education by rememberSaveable { mutableStateOf(initialSurvey.education) }
    var hasLoan by rememberSaveable { mutableStateOf(initialSurvey.hasLoan) }
    var loanDetails by rememberSaveable { mutableStateOf(initialSurvey.loanDetails) }
    var direction by rememberSaveable { mutableStateOf(initialSurvey.managementDirection.ifBlank { "save" }) }
    var interest by rememberSaveable { mutableStateOf(initialSurvey.interest) }
    var year by rememberSaveable { mutableStateOf(initialYear) }
    var month by rememberSaveable { mutableStateOf(initialMonth) }

    val handleBack: () -> Unit = {
        step = when (step) {
            AnalysisFlowStep.REQUEST -> AnalysisFlowStep.SURVEY

            AnalysisFlowStep.LOADING -> if (closeAfterResult) {
                onBack()
                AnalysisFlowStep.LOADING
            } else {
                AnalysisFlowStep.REQUEST
            }

            AnalysisFlowStep.RESULT -> if (closeAfterResult) {
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
    BackHandler(onBack = handleBack)
    LaunchedEffect(
        step,
        analysisState.isGenerating,
        analysisState.isLoadingReport,
        analysisState.activeReport,
        analysisState.failure,
    ) {
        if (step == AnalysisFlowStep.LOADING && !analysisState.isGenerating && !analysisState.isLoadingReport) {
            if (analysisState.activeReport != null && analysisState.activeContent != null) {
                step = AnalysisFlowStep.RESULT
            } else if (closeAfterResult) {
                onBack()
            } else {
                step = AnalysisFlowStep.REQUEST
            }
        }
    }

    val reportPeriod = analysisState.activeReport?.periodStart?.let {
        runCatching { YearMonth.parse(it.take(7)) }.getOrNull()
    }
    val title = when (step) {
        AnalysisFlowStep.SURVEY -> stringResource(R.string.analysis_survey_title)

        AnalysisFlowStep.RESULT -> stringResource(
            R.string.analysis_result_title,
            reportPeriod?.year ?: year,
            reportPeriod?.monthValue ?: month,
        )

        AnalysisFlowStep.REQUEST, AnalysisFlowStep.LOADING -> stringResource(R.string.analysis_title)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                expandedHeight = 52.dp,
                title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.navigate_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        when (step) {
            AnalysisFlowStep.SURVEY -> SurveyScreen(
                modifier = Modifier.padding(innerPadding),
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
                onSave = {
                    val completedSurvey = AnalysisSurvey(
                        gender,
                        formatSurveyBirthDate(
                            birthDate,
                        ),
                        job, workplace, education, hasLoan, loanDetails, direction, interest,
                    )
                    onSurveySaved(completedSurvey)
                    if (continueAfterSurvey) step = AnalysisFlowStep.REQUEST else onBack()
                },
            )

            AnalysisFlowStep.REQUEST -> AnalysisRequestScreen(
                modifier = Modifier.padding(innerPadding),
                year = year,
                month = month,
                onYearMonthChange = { newYear, newMonth ->
                    year = newYear
                    month = newMonth
                },
                failure = analysisState.failure,
                onClearFailure = onClearFailure,
                onStart = {
                    val requestSurvey = AnalysisSurvey(
                        gender,
                        formatSurveyBirthDate(
                            birthDate,
                        ),
                        job, workplace, education, hasLoan, loanDetails, direction, interest,
                    )
                    onRequestReport(YearMonth.of(year, month), requestSurvey)
                    step = AnalysisFlowStep.LOADING
                },
            )

            AnalysisFlowStep.LOADING -> AnalysisLoadingScreen(Modifier.padding(innerPadding))

            AnalysisFlowStep.RESULT -> AnalysisResultScreen(
                modifier = Modifier.padding(innerPadding),
                content = analysisState.activeContent,
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun SurveyScreen(
    modifier: Modifier,
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
    onSave: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        SurveySection(R.string.analysis_survey_profile_section) {
            EditorField(stringResource(R.string.analysis_survey_gender)) {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GenderOption(
                        survey.gender,
                        "male",
                        R.string.analysis_survey_male,
                        Icons.Outlined.Male,
                        Color(0xFF2879CB),
                        onGenderChange,
                    )
                    GenderOption(
                        survey.gender,
                        "female",
                        R.string.analysis_survey_female,
                        Icons.Outlined.Female,
                        Color(0xFFD04459),
                        onGenderChange,
                    )
                    EditorTextField(
                        value = survey.birthDate,
                        onValueChange = { onBirthDateChange(surveyBirthDateDigits(it)) },
                        visualTransformation = SurveyBirthDateTransformation,
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        placeholder = stringResource(R.string.analysis_survey_birth_hint),
                        keyboardType = KeyboardType.Number,
                    )
                }
            }
            SurveyField(R.string.analysis_survey_job, survey.job, onJobChange, R.string.analysis_survey_job_hint)
            SurveyField(
                R.string.analysis_survey_workplace,
                survey.workplace,
                onWorkplaceChange,
                R.string.analysis_survey_workplace_hint,
            )
            EditorField(stringResource(R.string.analysis_survey_education)) {
                EducationField(survey.education, onEducationChange)
            }
        }
        Spacer(Modifier.height(40.dp))
        SurveySection(R.string.analysis_survey_finance_section) {
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
                )
            }
        }
        Spacer(Modifier.height(40.dp))
        SurveySection(R.string.analysis_survey_goal_section) {
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
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.analysis_survey_privacy),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(32.dp))
        SaveButton(enabled = true, isSaving = false, onClick = onSave)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun SurveySection(title: Int, content: @Composable () -> Unit) {
    SettingsSection(stringResource(title)) { content() }
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
    EditorField(stringResource(label)) {
        EditorTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = true,
            placeholder = stringResource(hint),
            keyboardType = keyboardType,
            minLines = minLines,
        )
    }
}

private fun surveyBirthDateDigits(value: String): String = value.filter { it in '0'..'9' }.take(8)

internal object SurveyBirthDateTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = formatSurveyBirthDate(text.text)
        return TransformedText(
            AnnotatedString(formatted),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int =
                    offset + (if (offset > 4) 1 else 0) + (if (offset > 6) 1 else 0)

                override fun transformedToOriginal(offset: Int): Int = formatted.take(offset).count { it != '-' }
            },
        )
    }
}

internal fun formatSurveyBirthDate(value: String): String {
    val digits = surveyBirthDateDigits(value)
    return listOf(digits.take(4), digits.drop(4).take(2), digits.drop(6))
        .filter(String::isNotEmpty)
        .joinToString("-")
}

@Composable
private fun GenderOption(
    selected: String,
    value: String,
    label: Int,
    icon: ImageVector,
    tint: Color,
    onSelect: (String) -> Unit,
) {
    val isSelected = selected == value
    Box(
        modifier = Modifier.width(48.dp).fillMaxHeight().clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) tint.copy(alpha = 0.18f) else editorMutedBackground())
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) }),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, stringResource(label), tint = tint)
    }
}

@Composable
private fun ChoiceRow(labels: List<Int>, values: List<String>, selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().selectableGroup()) {
            labels.forEachIndexed { index, label ->
                val isSelected = selected == values[index]
                Box(
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) colors.primary.copy(alpha = 0.12f) else editorMutedBackground(),
                        )
                        .border(
                            1.dp,
                            if (isSelected) colors.primary else colors.outlineVariant,
                            RoundedCornerShape(14.dp),
                        )
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = {
                            onSelect(values[index])
                        })
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(label),
                        color = if (isSelected) colors.primary else colors.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun EducationField(value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Box {
            EditorTextField(
                value = value,
                onValueChange = {},
                enabled = true,
                readOnly = true,
                placeholder = stringResource(R.string.analysis_survey_education_hint),
            )
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(14.dp)).clickable { expanded = true })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf(
                    stringResource(R.string.analysis_education_middle_school),
                    stringResource(R.string.analysis_education_high_school),
                    stringResource(R.string.analysis_education_university_student),
                    stringResource(R.string.analysis_education_bachelor),
                    stringResource(R.string.analysis_education_graduate_student),
                    stringResource(R.string.analysis_education_master),
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
    modifier: Modifier,
    year: Int,
    month: Int,
    onYearMonthChange: (Int, Int) -> Unit,
    failure: AnalysisFailure?,
    onClearFailure: () -> Unit,
    onStart: () -> Unit,
) {
    val selectedMonth = YearMonth.of(year, month)
    val currentMonth = YearMonth.now(analysisZone)
    MainTabContainer(bottomPadding = 0.dp, modifier = modifier) {
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
                    onClearFailure()
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
                IconButton(
                    enabled = selectedMonth < currentMonth,
                    onClick = {
                        onClearFailure()
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
                    },
                ) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.analysis_next_month))
                }
            }
        }
        failure?.let {
            Text(
                text = analysisFailureMessage(it),
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(28.dp))
        PrimaryAuthButton(text = stringResource(R.string.analysis_start), onClick = onStart)
    }
}

@Composable
private fun AnalysisLoadingScreen(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
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

@Composable
private fun AnalysisResultScreen(modifier: Modifier, content: AnalysisReportContent?) {
    MainTabContainer(bottomPadding = 0.dp, modifier = modifier) {
        content?.aWordFromAi?.let { body ->
            ResultCard(R.string.analysis_result_ai_title, highlighted = true) {
                Text(body, style = MaterialTheme.typography.bodyLarge, lineHeight = 22.sp)
            }
        }
        if (content?.topSpendingCategories?.isNotEmpty() == true) {
            ResultCard(R.string.analysis_result_categories_title) {
                content.topSpendingCategories.forEach { category ->
                    AnalysisCategoryRow(category)
                }
            }
        }
        content?.spendingHealth?.let { AnalysisTextResultCard(R.string.analysis_result_health_title, it) }
        content?.spendingByDayOfWeek?.let { AnalysisTextResultCard(R.string.analysis_result_weekday_title, it) }
        content?.tipsForSaving?.let { AnalysisTextResultCard(R.string.analysis_result_save_advice, it) }
        content?.tipsToKeepItUp?.let { AnalysisTextResultCard(R.string.analysis_result_maintain_advice, it) }
        content?.tipsForInterests?.let { AnalysisTextResultCard(R.string.analysis_result_interest_title, it) }
        content?.newsDrivenInsights?.let { AnalysisTextResultCard(R.string.analysis_result_news_title, it) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AnalysisCategoryRow(category: AnalysisCategoryInsight) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category.name, fontWeight = FontWeight.Bold)
            category.occurrence?.let { Text(stringResource(R.string.analysis_result_occurrence, it)) }
        }
        if (category.tip.isNotBlank()) {
            Text(
                category.tip,
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun AnalysisTextResultCard(title: Int, body: String) {
    ResultCard(title) { Text(body, lineHeight = 21.sp) }
}

@Composable
internal fun analysisFailureMessage(failure: AnalysisFailure): String = stringResource(
    when (failure) {
        AnalysisFailure.NETWORK -> R.string.network_error
        AnalysisFailure.RESPONSE -> R.string.response_error
        AnalysisFailure.AUTHENTICATION -> R.string.analysis_error_authentication
        AnalysisFailure.NO_TRANSACTIONS -> R.string.analysis_error_no_transactions
        AnalysisFailure.IN_PROGRESS -> R.string.analysis_error_in_progress
        AnalysisFailure.PROVIDER -> R.string.analysis_error_provider
        AnalysisFailure.TIMEOUT -> R.string.analysis_error_timeout
        AnalysisFailure.SERVICE -> R.string.service_unavailable_error
        AnalysisFailure.UNREADABLE_REPORT -> R.string.analysis_error_unreadable_report
    },
)

private val analysisZone: ZoneId = ZoneId.of("Asia/Seoul")
