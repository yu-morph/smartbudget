package com.yu_morph.smartbudget.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
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

private val SettingsBackground = Color(0xFFF4F5F7)
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
                    SettingsEntry(R.string.settings_notifications),
                    SettingsEntry(R.string.settings_display),
                    SettingsEntry(R.string.settings_language, R.string.settings_language_value),
                ),
        ),
        SettingsGroup(
            title = R.string.settings_information_section,
            entries =
                listOf(
                    SettingsEntry(R.string.settings_open_source_licenses),
                    SettingsEntry(R.string.settings_app_info),
                ),
        ),
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
        modifier = Modifier.fillMaxSize().background(SettingsBackground),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ProfileHeader(
                displayName = displayName.ifBlank { stringResource(R.string.settings_profile_default_name) },
                username = username.ifBlank { stringResource(R.string.settings_username_unavailable) },
            )
        }
        SettingsGroups.forEach { group ->
            item { SettingsGroupCard(group) }
        }
    }
}

@Composable
private fun ProfileHeader(displayName: String, username: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    // 개인정보 수정 화면 연결은 후속 작업에서 구현합니다.
                }.padding(horizontal = 4.dp, vertical = 16.dp),
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
            modifier = Modifier.padding(start = 18.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = username, color = SettingsSecondaryText, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        SettingsChevron()
    }
}

@Composable
private fun SettingsGroupCard(group: SettingsGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 14.dp)) {
            Text(
                text = stringResource(group.title),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 4.dp),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            group.entries.forEach { entry -> SettingsRow(entry) }
        }
    }
}

@Composable
private fun SettingsRow(entry: SettingsEntry) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    // 각 설정 화면 연결은 후속 작업에서 구현합니다.
                }.padding(horizontal = 20.dp, vertical = 16.dp),
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
private fun SettingsChevron() {
    Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = SettingsChevronColor,
    )
}
