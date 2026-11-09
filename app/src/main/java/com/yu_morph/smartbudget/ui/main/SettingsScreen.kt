package com.yu_morph.smartbudget.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.auth.openExampleCustomTab
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen

private val SettingsGradientGray = Color(0xFFF0F1F3)
private val SettingsGradientLightGray = Color(0xFFF8F8F7)
private val SettingsGradientGreen = Color(0xFFF0F8F4)
private val SettingsPrimaryText = Color(0xFF404247)
private val SettingsSecondaryText = Color(0xFF8A8D92)
private val SettingsChevronColor = Color(0xFFB3B7BC)
private val SettingsAvatarBackground = Color(0xFFE2E5E9)
private val SettingsAvatarForeground = Color(0xFFB8BEC7)

private data class SettingsEntry(@param:StringRes val label: Int, @param:StringRes val value: Int? = null)

private data class SettingsGroup(@param:StringRes val title: Int, val entries: List<SettingsEntry>)

private val SettingsGroups =
    listOf(
        SettingsGroup(
            title = R.string.settings_account_section,
            entries =
                listOf(
                    SettingsEntry(R.string.settings_edit_profile),
                    SettingsEntry(R.string.settings_connect_bank),
                ),
        ),
        SettingsGroup(
            title = R.string.settings_app_section,
            entries =
                listOf(
                    SettingsEntry(R.string.settings_language, R.string.settings_language_value),
                    SettingsEntry(R.string.settings_notifications),
                    SettingsEntry(R.string.settings_display),
                ),
        ),
        SettingsGroup(
            title = R.string.settings_legal_section,
            entries =
                listOf(
                    SettingsEntry(R.string.settings_terms_of_service),
                    SettingsEntry(R.string.settings_privacy_policy),
                ),
        ),
    )

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    bottomContentPadding: Dp,
    onLoad: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) { onLoad() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(SettingsGradientGray, SettingsGradientLightGray, SettingsGradientGreen),
                    ),
                ).verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 48.dp, end = 20.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProfileHeader(
            displayName = state.displayName.ifBlank { stringResource(R.string.settings_profile_default_name) },
            userId = state.userId,
            onLogout = onLogout,
        )
        SettingsGroups.forEach { group ->
            SettingsGroupCard(
                group = group,
                onEntryClick = { entry ->
                    if (entry.label == R.string.settings_terms_of_service ||
                        entry.label == R.string.settings_privacy_policy
                    ) {
                        openExampleCustomTab(context)
                    }
                },
            )
        }
        WithdrawalCard()
        SettingsFooter(
            versionName = state.versionName,
            onOpenSourceClick = {
                // 오픈소스 라이선스 화면 연결은 후속 작업에서 구현합니다.
            },
        )
    }
}

@Composable
private fun ProfileHeader(displayName: String, userId: String, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = SettingsAvatarBackground,
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier.padding(17.dp),
                tint = SettingsAvatarForeground,
            )
        }
        Column(
            modifier = Modifier.padding(start = 18.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                text =
                    if (userId.isBlank()) {
                        stringResource(R.string.settings_user_id_unavailable)
                    } else {
                        userId
                    },
                color = SettingsSecondaryText,
                fontSize = 14.sp,
            )
        }
        IconButton(onClick = onLogout) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = stringResource(R.string.settings_logout),
                tint = SettingsPrimaryText,
            )
        }
    }
}

@Composable
private fun SettingsGroupCard(group: SettingsGroup, onEntryClick: (SettingsEntry) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)) {
            Text(
                text = stringResource(group.title),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 4.dp),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            group.entries.forEach { entry -> SettingsRow(entry, onClick = { onEntryClick(entry) }) }
        }
    }
}

@Composable
private fun SettingsRow(entry: SettingsEntry, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(entry.label),
            color = SettingsPrimaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.weight(1f))
        entry.value?.let { value ->
            Text(
                text = stringResource(value),
                modifier = Modifier.padding(end = 10.dp),
                color = SmartBudgetGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        SettingsChevron()
    }
}

@Composable
private fun WithdrawalCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        SettingsRow(
            entry = SettingsEntry(R.string.settings_withdraw),
            onClick = {
                // 회원 탈퇴 화면 연결은 후속 작업에서 구현합니다.
            },
        )
    }
}

@Composable
private fun SettingsFooter(versionName: String, onOpenSourceClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_app_version, versionName),
            color = SettingsSecondaryText,
            fontSize = 13.sp,
        )
        Text(
            text = stringResource(R.string.settings_view_open_source_licenses),
            modifier = Modifier.clickable(onClick = onOpenSourceClick),
            color = SettingsPrimaryText,
            fontSize = 13.sp,
            textDecoration = TextDecoration.Underline,
        )
    }
}

@Composable
private fun SettingsChevron() {
    Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = SettingsChevronColor,
    )
}
