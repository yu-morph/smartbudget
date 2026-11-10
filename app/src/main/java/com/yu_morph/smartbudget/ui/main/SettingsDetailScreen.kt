@file:Suppress("MagicNumber", "MatchingDeclarationName", "TooManyFunctions")

package com.yu_morph.smartbudget.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.local.DisplayMode
import com.yu_morph.smartbudget.ui.auth.SignUpValidation
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class SettingsDetailDestination {
    PROFILE,
    MONTHLY_BUDGET,
    LANGUAGE,
    NOTIFICATIONS,
    DISPLAY,
    WITHDRAWAL,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsDetailScreen(
    destination: SettingsDetailDestination,
    state: SettingsUiState,
    onBack: () -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onUpdatePassword: (String, String) -> Unit,
    onLoadBudget: () -> Unit,
    onSaveBudget: (Int) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onNotificationChanged: (NotificationSetting, Boolean) -> Unit,
    onDisplayModeChanged: (DisplayMode) -> Unit,
    onVerifyWithdrawal: (String, () -> Unit) -> Unit,
    onDeleteAccount: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(destination.titleResource()),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.navigate_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        when (destination) {
            SettingsDetailDestination.PROFILE -> ProfileSettings(
                state = state,
                modifier = Modifier.padding(innerPadding),
                onUpdateDisplayName = onUpdateDisplayName,
                onUpdatePassword = onUpdatePassword,
            )

            SettingsDetailDestination.MONTHLY_BUDGET -> BudgetSettings(
                state = state,
                modifier = Modifier.padding(innerPadding),
                onLoad = onLoadBudget,
                onSave = onSaveBudget,
            )

            SettingsDetailDestination.LANGUAGE -> LanguageSettings(
                selectedLanguage = state.appSettings.languageTag,
                modifier = Modifier.padding(innerPadding),
                onSelected = onLanguageSelected,
            )

            SettingsDetailDestination.NOTIFICATIONS -> NotificationSettings(
                state = state,
                modifier = Modifier.padding(innerPadding),
                onChanged = onNotificationChanged,
            )

            SettingsDetailDestination.DISPLAY -> DisplaySettings(
                selectedMode = state.appSettings.displayMode,
                modifier = Modifier.padding(innerPadding),
                onSelected = onDisplayModeChanged,
            )

            SettingsDetailDestination.WITHDRAWAL -> WithdrawalSettings(
                modifier = Modifier.padding(innerPadding),
                onVerify = onVerifyWithdrawal,
                onDeleteAccount = onDeleteAccount,
            )
        }
    }
}

@Composable
private fun ProfileSettings(
    state: SettingsUiState,
    modifier: Modifier,
    onUpdateDisplayName: (String) -> Unit,
    onUpdatePassword: (String, String) -> Unit,
) {
    var displayName by rememberSaveable(state.displayName) { mutableStateOf(state.displayName) }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var passwordConfirmation by rememberSaveable { mutableStateOf("") }
    SettingsColumn(modifier) {
        SettingsCard(title = stringResource(R.string.settings_display_name_section)) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.display_name_hint)) },
                singleLine = true,
            )
            Button(
                onClick = { onUpdateDisplayName(displayName.trim()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = SignUpValidation.isValidDisplayName(displayName),
            ) { Text(stringResource(R.string.settings_save_display_name)) }
        }
        SettingsCard(title = stringResource(R.string.settings_password_section)) {
            PasswordField(currentPassword, { currentPassword = it }, R.string.settings_current_password)
            PasswordField(newPassword, { newPassword = it }, R.string.settings_new_password)
            PasswordField(passwordConfirmation, { passwordConfirmation = it }, R.string.password_confirm_hint)
            Text(
                text = stringResource(R.string.password_condition_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Button(
                onClick = { onUpdatePassword(currentPassword, newPassword) },
                modifier = Modifier.fillMaxWidth(),
                enabled =
                    currentPassword.isNotBlank() &&
                        SignUpValidation.isValidPassword(newPassword) &&
                        newPassword == passwordConfirmation,
            ) { Text(stringResource(R.string.settings_change_password)) }
        }
    }
}

@Composable
private fun BudgetSettings(state: SettingsUiState, modifier: Modifier, onLoad: () -> Unit, onSave: (Int) -> Unit) {
    var amountText by rememberSaveable(state.budgetAmount) { mutableStateOf(state.budgetAmount.toString()) }
    LaunchedEffect(Unit) { onLoad() }
    SettingsColumn(modifier) {
        SettingsCard(title = stringResource(R.string.settings_current_month_budget)) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { value -> amountText = value.filter(Char::isDigit).take(8) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_budget_amount)) },
                suffix = { Text(stringResource(R.string.settings_won)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(500_000, 1_000_000, 2_000_000).forEach { amount ->
                    OutlinedButton(onClick = { amountText = amount.toString() }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_budget_quick_amount, amount / 10_000))
                    }
                }
            }
            Button(
                onClick = { amountText.toIntOrNull()?.let(onSave) },
                modifier = Modifier.fillMaxWidth(),
                enabled = amountText.toIntOrNull() in 0..10_000_000,
            ) { Text(stringResource(R.string.settings_save_budget)) }
        }
        SettingsCard(title = stringResource(R.string.settings_budget_history)) {
            if (state.budgetHistory.isEmpty()) {
                Text(
                    stringResource(R.string.settings_budget_history_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.budgetHistory.forEach { item ->
                    SettingValueRow(
                        label = item.month.format(DateTimeFormatter.ofPattern("yyyy년 M월")),
                        value = stringResource(
                            R.string.settings_budget_history_amount,
                            NumberFormat.getNumberInstance(Locale.KOREA).format(item.amount),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageSettings(selectedLanguage: String, modifier: Modifier, onSelected: (String) -> Unit) {
    SettingsColumn(modifier) {
        SettingsCard(title = stringResource(R.string.settings_available_languages)) {
            SelectableRow(
                title = stringResource(R.string.settings_korean),
                subtitle = "🇰🇷",
                selected = selectedLanguage == "ko",
                onClick = { onSelected("ko") },
            )
        }
    }
}

@Composable
private fun NotificationSettings(
    state: SettingsUiState,
    modifier: Modifier,
    onChanged: (NotificationSetting, Boolean) -> Unit,
) {
    SettingsColumn(modifier) {
        SettingsCard(title = stringResource(R.string.settings_notification_categories)) {
            SwitchRow(
                R.string.settings_budget_approaching_notification,
                state.appSettings.budgetApproachingNotification,
            ) { onChanged(NotificationSetting.BUDGET_APPROACHING, it) }
            SwitchRow(
                R.string.settings_budget_exceeded_notification,
                state.appSettings.budgetExceededNotification,
            ) { onChanged(NotificationSetting.BUDGET_EXCEEDED, it) }
            SwitchRow(R.string.settings_analysis_notification, state.appSettings.analysisNotification) {
                onChanged(NotificationSetting.ANALYSIS, it)
            }
        }
    }
}

@Composable
private fun DisplaySettings(selectedMode: DisplayMode, modifier: Modifier, onSelected: (DisplayMode) -> Unit) {
    SettingsColumn(modifier) {
        SettingsCard(title = stringResource(R.string.settings_theme)) {
            DisplayMode.entries.forEach { mode ->
                SelectableRow(
                    title = stringResource(mode.labelResource()),
                    selected = selectedMode == mode,
                    onClick = { onSelected(mode) },
                )
            }
        }
    }
}

@Composable
private fun WithdrawalSettings(
    modifier: Modifier,
    onVerify: (String, () -> Unit) -> Unit,
    onDeleteAccount: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var showConfirmation by rememberSaveable { mutableStateOf(false) }
    SettingsColumn(modifier) {
        SettingsCard(title = stringResource(R.string.settings_withdraw)) {
            Text(
                stringResource(R.string.settings_withdraw_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PasswordField(password, { password = it }, R.string.settings_current_password)
            Button(
                onClick = { onVerify(password) { showConfirmation = true } },
                modifier = Modifier.fillMaxWidth(),
                enabled = password.isNotBlank(),
            ) { Text(stringResource(R.string.settings_verify_password)) }
        }
    }
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text(stringResource(R.string.settings_withdraw_confirm_title)) },
            text = { Text(stringResource(R.string.settings_withdraw_confirm_message)) },
            confirmButton = {
                TextButton(onClick = onDeleteAccount) {
                    Text(
                        stringResource(R.string.settings_withdraw_confirm_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) { Text(stringResource(R.string.close_action)) }
            },
        )
    }
}

@Composable
private fun SettingsColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
    )
}

@Composable
private fun SelectableRow(title: String, selected: Boolean, onClick: () -> Unit, subtitle: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        subtitle?.let { Text(it, modifier = Modifier.padding(end = 12.dp), fontSize = 24.sp) }
        Text(title, modifier = Modifier.weight(1f), fontSize = 16.sp)
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun SwitchRow(label: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(label), modifier = Modifier.weight(1f), fontSize = 16.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun SettingsDetailDestination.titleResource(): Int = when (this) {
    SettingsDetailDestination.PROFILE -> R.string.settings_edit_profile
    SettingsDetailDestination.MONTHLY_BUDGET -> R.string.settings_monthly_budget
    SettingsDetailDestination.LANGUAGE -> R.string.settings_language
    SettingsDetailDestination.NOTIFICATIONS -> R.string.settings_notifications
    SettingsDetailDestination.DISPLAY -> R.string.settings_display
    SettingsDetailDestination.WITHDRAWAL -> R.string.settings_withdraw
}

private fun DisplayMode.labelResource(): Int = when (this) {
    DisplayMode.LIGHT -> R.string.settings_theme_light
    DisplayMode.DARK -> R.string.settings_theme_dark
    DisplayMode.SYSTEM -> R.string.settings_theme_system
}
