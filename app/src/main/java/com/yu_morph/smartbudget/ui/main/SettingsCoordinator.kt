package com.yu_morph.smartbudget.ui.main

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yu_morph.smartbudget.data.local.PreferenceStoreFactory
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.SmartBudgetApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class SettingsUiState(val displayName: String = "", val userId: String = "", val versionName: String = "")

class SettingsCoordinator(context: Context, private val api: SmartBudgetApi, private val scope: CoroutineScope) {
    private val applicationContext = context.applicationContext
    private val userIdentityStore = PreferenceStoreFactory.userIdentity(applicationContext)

    var state by mutableStateOf(
        SettingsUiState(
            userId = userIdentityStore.readUsername().orEmpty(),
            versionName = readVersionName(),
        ),
    )
        private set

    fun refresh() {
        state = state.copy(userId = userIdentityStore.readUsername().orEmpty())
        scope.launch {
            val result = api.auth.getAccount()
            if (result is ApiResult.Success) {
                state = state.copy(displayName = result.data?.displayName.orEmpty())
            }
        }
    }

    private fun readVersionName(): String = applicationContext.packageManager
        .getPackageInfo(applicationContext.packageName, 0)
        .versionName
        .orEmpty()
}
