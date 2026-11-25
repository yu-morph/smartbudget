@file:Suppress("MagicNumber", "MatchingDeclarationName", "TooManyFunctions", "LongMethod")

package com.yu_morph.smartbudget.ui.main

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blongho.country_data.World
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.local.DisplayMode
import com.yu_morph.smartbudget.ui.auth.AuthTextField
import com.yu_morph.smartbudget.ui.auth.PrimaryAuthButton
import com.yu_morph.smartbudget.ui.auth.SignUpValidation
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SettingsDetailDestination {
    ACCOUNT,
    BANK_CONNECTION,
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
    onSaveAccount: (String, String, String, Int, Boolean, Boolean, () -> Unit) -> Unit,
    onProfileImageChanged: (String) -> Unit,
    onLoadBudget: () -> Unit,
    onSyncBankTransactions: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onNotificationChanged: (NotificationSetting, Boolean) -> Unit,
    onDisplayModeChanged: (DisplayMode) -> Unit,
    onDeleteAccount: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                expandedHeight = 52.dp,
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
            SettingsDetailDestination.ACCOUNT -> AccountSettings(
                state = state,
                modifier = Modifier.padding(innerPadding),
                onLoad = onLoadBudget,
                onSave = onSaveAccount,
                onProfileImageChanged = onProfileImageChanged,
            )

            SettingsDetailDestination.BANK_CONNECTION -> BankConnectionScreen(
                isLoading = state.isBankSyncing,
                modifier = Modifier.padding(innerPadding),
                onConnect = onSyncBankTransactions,
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
                onDeleteAccount = onDeleteAccount,
            )
        }
    }
}

@Composable
private fun AccountSettings(
    state: SettingsUiState,
    modifier: Modifier,
    onLoad: () -> Unit,
    onSave: (String, String, String, Int, Boolean, Boolean, () -> Unit) -> Unit,
    onProfileImageChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    var dialog by rememberSaveable { mutableStateOf<AccountEditDialog?>(null) }
    var displayNameInput by rememberSaveable { mutableStateOf("") }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var passwordConfirmation by rememberSaveable { mutableStateOf("") }
    var budgetInput by rememberSaveable { mutableStateOf("") }
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                onProfileImageChanged(it.toString())
            }
        }
    LaunchedEffect(Unit) { onLoad() }
    SettingsColumn(modifier) {
        SettingsSection(title = stringResource(R.string.settings_basic_information)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProfileImage(
                    imageUri = state.profileImageUri,
                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = state.displayName.ifBlank { stringResource(R.string.settings_profile_default_name) },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = state.userId.ifBlank { stringResource(R.string.settings_user_id_unavailable) },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = {
                        displayNameInput = state.displayName
                        dialog = AccountEditDialog.DISPLAY_NAME
                    },
                ) {
                    Icon(Icons.Outlined.Edit, stringResource(R.string.settings_edit_display_name))
                }
                IconButton(onClick = { dialog = AccountEditDialog.PASSWORD }) {
                    Icon(Icons.Outlined.Lock, stringResource(R.string.settings_change_password))
                }
            }
        }
        SettingsSection(title = stringResource(R.string.settings_monthly_budget_section)) {
            val historyByMonth = state.budgetHistory.associateBy(BudgetHistoryItem::month)
            val historyMonths = (2 downTo 0).map { YearMonth.now().minusMonths(it.toLong()) }
            val screenBackground = MaterialTheme.colorScheme.background
            val historyBackground =
                if (screenBackground.luminance() < 0.5f) {
                    lerp(screenBackground, Color.Black, 0.12f)
                } else {
                    Color(0xFFF4F4F4)
                }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                historyMonths.forEach { month ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = historyBackground,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(month.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(
                                text =
                                    historyByMonth[month]?.let {
                                        formatCompactBudget(it.amount, state.appSettings.languageTag)
                                    }
                                        ?: stringResource(R.string.settings_budget_history_unavailable),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.settings_current_budget_value,
                        NumberFormat.getNumberInstance(Locale.KOREA).format(state.budgetAmount),
                    ),
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                IconButton(
                    onClick = {
                        budgetInput = state.budgetAmount.toString()
                        dialog = AccountEditDialog.BUDGET
                    },
                ) {
                    Icon(Icons.Outlined.Edit, stringResource(R.string.settings_edit_budget))
                }
            }
        }
    }
    when (dialog) {
        AccountEditDialog.DISPLAY_NAME -> DisplayNameDialog(
            value = displayNameInput,
            onValueChange = { displayNameInput = it },
            onDismiss = { dialog = null },
            onSave = {
                onSave(displayNameInput.trim(), "", "", state.budgetAmount, true, false) { dialog = null }
            },
        )

        AccountEditDialog.PASSWORD -> PasswordChangeDialog(
            currentPassword = currentPassword,
            newPassword = newPassword,
            confirmation = passwordConfirmation,
            onCurrentPasswordChange = { currentPassword = it },
            onNewPasswordChange = { newPassword = it },
            onConfirmationChange = { passwordConfirmation = it },
            onDismiss = { dialog = null },
            onSave = {
                onSave(state.displayName, currentPassword, newPassword, state.budgetAmount, true, false) {
                    dialog = null
                }
            },
        )

        AccountEditDialog.BUDGET -> BudgetEditDialog(
            value = budgetInput,
            onValueChange = { budgetInput = it.filter(Char::isDigit).take(9) },
            onDismiss = { dialog = null },
            onSave = { amount ->
                onSave(state.displayName, "", "", amount, false, true) { dialog = null }
            },
        )

        null -> Unit
    }
}

private enum class AccountEditDialog { DISPLAY_NAME, PASSWORD, BUDGET }

@Composable
internal fun ProfileImage(imageUri: String?, size: Dp = 72.dp, onClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, imageUri) {
        value =
            withContext(Dispatchers.IO) {
                imageUri?.let { storedUri ->
                    runCatching {
                        context.contentResolver.openInputStream(Uri.parse(storedUri))?.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    }.getOrNull()
                }
            }
    }
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(SettingsAvatarBackground)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = stringResource(R.string.settings_profile_image),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = stringResource(R.string.settings_profile_image),
                modifier = Modifier.size(size * 0.48f),
                tint = SettingsAvatarForeground,
            )
        }
    }
}

@Composable
private fun DisplayNameDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_edit_display_name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AuthTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = stringResource(R.string.display_name_hint),
                )
                Text(
                    stringResource(R.string.display_name_condition_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = SignUpValidation.isValidDisplayName(value)) {
                Text(stringResource(R.string.settings_save_action))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_action)) } },
    )
}

@Composable
private fun PasswordChangeDialog(
    currentPassword: String,
    newPassword: String,
    confirmation: String,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.settings_password_dialog_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PasswordField(currentPassword, onCurrentPasswordChange, R.string.settings_current_password)
                PasswordField(newPassword, onNewPasswordChange, R.string.settings_new_password)
                PasswordField(confirmation, onConfirmationChange, R.string.password_confirm_hint)
                Text(
                    stringResource(R.string.password_condition_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = isPasswordUpdateValid(currentPassword, newPassword, confirmation),
            ) {
                Text(stringResource(R.string.settings_change_password))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_action)) } },
    )
}

@Composable
private fun BudgetEditDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    val amount = value.toIntOrNull()
    val updateAmount: (Int) -> Unit = { changed -> onValueChange(changed.coerceIn(0, 999_999_999).toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_edit_budget)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorField(stringResource(R.string.settings_budget_amount)) {
                    EditorTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = true,
                        placeholder = stringResource(R.string.budget_book_editor_income_amount_hint),
                        suffix = stringResource(R.string.settings_won),
                        keyboardType = KeyboardType.Number,
                        visualTransformation = ThousandsSeparatorTransformation,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    val presets = listOf(500_000, 1_000_000, 2_000_000)
                    presets.forEachIndexed { index, preset ->
                        BudgetSegmentedButton(
                            text = stringResource(R.string.settings_budget_quick_amount, preset / 10_000),
                            onClick = { updateAmount(preset) },
                            index = index,
                            count = presets.size,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    val adjustments = listOf(-1_000_000, -100_000, -10_000, 10_000, 100_000, 1_000_000)
                    adjustments.forEachIndexed { index, delta ->
                        BudgetSegmentedButton(
                            text = budgetAdjustmentLabel(delta),
                            onClick = { updateAmount((amount ?: 0) + delta) },
                            index = index,
                            count = adjustments.size,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { amount?.let(onSave) }, enabled = amount != null) {
                Text(stringResource(R.string.settings_save_action))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_action)) } },
    )
}

@Composable
private fun BudgetSegmentedButton(
    text: String,
    onClick: () -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val shape =
        when (index) {
            0 -> RoundedCornerShape(topStart = 9.dp, bottomStart = 9.dp)
            count - 1 -> RoundedCornerShape(topEnd = 9.dp, bottomEnd = 9.dp)
            else -> RoundedCornerShape(0.dp)
        }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Button(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = shape,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color(0xFF363636) else Color(0xFFECEDEF),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        contentPadding = PaddingValues(horizontal = 1.dp, vertical = 0.dp),
    ) {
        Text(text = text, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun budgetAdjustmentLabel(delta: Int): String = stringResource(
    if (delta < 0) R.string.settings_budget_adjust_decrease else R.string.settings_budget_adjust_increase,
    kotlin.math.abs(delta) / 10_000,
)

internal fun formatCompactBudget(amount: Int, languageTag: String = Locale.getDefault().language): String {
    val formatter = DecimalFormat("0.#", DecimalFormatSymbols(Locale.US))
    return if (Locale.forLanguageTag(languageTag).language == Locale.KOREAN.language) {
        when {
            amount >= 100_000_000 -> "${formatter.format(amount / 100_000_000.0)}억원"
            amount >= 10_000 -> "${formatter.format(amount / 10_000.0)}만원"
            else -> "${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"
        }
    } else {
        when {
            amount >= 1_000_000 -> "${formatter.format(amount / 1_000_000.0)}M￦"
            amount >= 1_000 -> "${formatter.format(amount / 1_000.0)}K￦"
            else -> "$amount￦"
        }
    }
}

private fun isPasswordUpdateValid(currentPassword: String, newPassword: String, confirmation: String): Boolean =
    currentPassword.isNotBlank() && SignUpValidation.isValidPassword(newPassword) && newPassword == confirmation

@Composable
private fun LanguageSettings(selectedLanguage: String, modifier: Modifier, onSelected: (String) -> Unit) {
    val languages =
        listOf(
            LanguageOption("en-US", "English", "us", R.string.settings_english_flag),
            LanguageOption("ko", "한국어", "kr", R.string.settings_korean_flag),
        ).sortedByDescending { it.tag }
    SettingsColumn(modifier) {
        languages.forEach { language ->
            SelectionRow(
                title = language.nativeName,
                selected = selectedLanguage == language.tag,
                onClick = { onSelected(language.tag) },
                leadingContent = {
                    Image(
                        painter = painterResource(World.getFlagOf(language.countryCode)),
                        contentDescription = stringResource(language.flagDescriptionResource),
                        modifier = Modifier.size(width = 34.dp, height = 24.dp).clip(RoundedCornerShape(5.dp)),
                    )
                },
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
        SettingsSection(title = stringResource(R.string.settings_notification_categories)) {
            SwitchRow(
                R.string.settings_budget_approaching_notification,
                state.appSettings.budgetApproachingNotification,
            ) { onChanged(NotificationSetting.BUDGET_APPROACHING, it) }
            SwitchRow(
                R.string.settings_budget_exceeded_notification,
                state.appSettings.budgetExceededNotification,
            ) { onChanged(NotificationSetting.BUDGET_EXCEEDED, it) }
        }
        SettingsSection(title = stringResource(R.string.settings_analysis_section)) {
            SwitchRow(R.string.settings_analysis_notification, state.appSettings.analysisNotification) {
                onChanged(NotificationSetting.ANALYSIS, it)
            }
        }
    }
}

@Composable
private fun DisplaySettings(selectedMode: DisplayMode, modifier: Modifier, onSelected: (DisplayMode) -> Unit) {
    SettingsColumn(modifier) {
        DisplayMode.entries.forEach { mode ->
            SelectionRow(
                title = stringResource(mode.labelResource()),
                selected = selectedMode == mode,
                onClick = { onSelected(mode) },
                leadingContent = {
                    Icon(
                        imageVector = mode.icon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun WithdrawalSettings(modifier: Modifier, onDeleteAccount: () -> Unit) {
    var showConfirmation by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(top = 20.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_withdraw_confirm_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.settings_withdraw_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.account_withdrawal_robot),
                contentDescription = stringResource(R.string.settings_withdraw_robot_description),
                modifier = Modifier.size(210.dp),
            )
        }
        PrimaryAuthButton(
            text = stringResource(R.string.settings_withdraw_action),
            onClick = { showConfirmation = true },
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text(stringResource(R.string.settings_withdraw_confirm_title)) },
            text = { Text(stringResource(R.string.settings_withdraw_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = onDeleteAccount,
                    colors = ButtonDefaults.textButtonColors(contentColor = SmartBudgetGreen),
                ) {
                    Text(stringResource(R.string.settings_withdraw_confirm_action))
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
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
internal fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        content()
    }
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: Int) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = stringResource(label),
        isPassword = true,
    )
}

@Composable
private fun SelectionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent?.let {
            it()
            Spacer(Modifier.size(12.dp))
        }
        Text(title, modifier = Modifier.weight(1f), fontSize = 16.sp)
        androidx.compose.foundation.layout.Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .indication(interactionSource, ripple(bounded = true, radius = 18.dp))
                    .background(if (selected) SmartBudgetGreen else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(label: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onCheckedChange(!checked) },
                )
                .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(label), modifier = Modifier.weight(1f), fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.graphicsLayer(scaleX = 0.82f, scaleY = 0.82f),
        )
    }
}

private fun SettingsDetailDestination.titleResource(): Int = when (this) {
    SettingsDetailDestination.ACCOUNT -> R.string.settings_account_settings
    SettingsDetailDestination.BANK_CONNECTION -> R.string.settings_connect_bank
    SettingsDetailDestination.LANGUAGE -> R.string.settings_language
    SettingsDetailDestination.NOTIFICATIONS -> R.string.settings_notifications
    SettingsDetailDestination.DISPLAY -> R.string.settings_display
    SettingsDetailDestination.WITHDRAWAL -> R.string.settings_withdraw
}

private data class LanguageOption(
    val tag: String,
    val nativeName: String,
    val countryCode: String,
    val flagDescriptionResource: Int,
)

private fun DisplayMode.labelResource(): Int = when (this) {
    DisplayMode.LIGHT -> R.string.settings_theme_light
    DisplayMode.DARK -> R.string.settings_theme_dark
    DisplayMode.SYSTEM -> R.string.settings_theme_system
}

private fun DisplayMode.icon() = when (this) {
    DisplayMode.LIGHT -> Icons.Outlined.LightMode
    DisplayMode.DARK -> Icons.Outlined.DarkMode
    DisplayMode.SYSTEM -> Icons.Outlined.SettingsBrightness
}
