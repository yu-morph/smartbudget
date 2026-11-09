package com.yu_morph.smartbudget.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.local.PreferenceStoreFactory
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.SmartBudgetApi
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen

private val SettingsCardBorder = Color(0xFFE8E8E8)
private val SettingsSecondaryText = Color(0xFF737373)
private val SettingsIconColor = Color(0xFF5F5F5F)

private data class SettingsEntry(@param:StringRes val label: Int, val icon: ImageVector)

private val AccountSettings =
    listOf(
        SettingsEntry(R.string.settings_edit_profile, Icons.Outlined.ManageAccounts),
        SettingsEntry(R.string.settings_connect_bank, Icons.Outlined.AccountBalance),
    )

private val AppSettings =
    listOf(
        SettingsEntry(R.string.settings_notifications, Icons.Outlined.Notifications),
        SettingsEntry(R.string.settings_display, Icons.Outlined.DisplaySettings),
        SettingsEntry(R.string.settings_language, Icons.Outlined.Language),
    )

private val InformationSettings =
    listOf(
        SettingsEntry(R.string.settings_open_source_licenses, Icons.Outlined.Description),
        SettingsEntry(R.string.settings_app_info, Icons.Outlined.Info),
    )

@Composable
internal fun SettingsScreen(api: SmartBudgetApi) {
    val context = LocalContext.current
    val username = remember(context) { PreferenceStoreFactory.userIdentity(context).readUsername().orEmpty() }
    var displayName by remember { mutableStateOf("") }

    LaunchedEffect(api) {
        val result = api.auth.getAccount()
        if (result is ApiResult.Success) displayName = result.data?.displayName.orEmpty()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ProfileCard(
                displayName = displayName.ifBlank { stringResource(R.string.settings_profile_default_name) },
                username = username.ifBlank { stringResource(R.string.settings_username_unavailable) },
            )
        }
        settingsSection(R.string.settings_account_section, AccountSettings)
        settingsSection(R.string.settings_app_section, AppSettings)
        settingsSection(R.string.settings_information_section, InformationSettings)
    }
}

private fun LazyListScope.settingsSection(@StringRes title: Int, entries: List<SettingsEntry>) {
    item { SettingsSectionTitle(title) }
    entries.forEach { entry ->
        item { SettingsItemCard(entry) }
    }
}

@Composable
private fun ProfileCard(displayName: String, username: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SettingsCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = SmartBudgetGreen.copy(alpha = 0.12f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp),
                    tint = SmartBudgetGreen,
                )
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = displayName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(text = username, color = SettingsSecondaryText, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        color = SettingsSecondaryText,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SettingsItemCard(entry: SettingsEntry) {
    Card(
        onClick = {
            // 각 설정 화면 연결은 후속 작업에서 구현합니다.
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SettingsCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = SettingsIconColor,
            )
            Text(
                text = stringResource(entry.label),
                modifier = Modifier.padding(start = 14.dp),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = SettingsSecondaryText,
            )
        }
    }
}
