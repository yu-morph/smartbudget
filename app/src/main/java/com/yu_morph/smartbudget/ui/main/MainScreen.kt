package com.yu_morph.smartbudget.ui.main

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet as FilledAccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart as FilledBarChart
import androidx.compose.material.icons.filled.Home as FilledHome
import androidx.compose.material.icons.filled.Settings as FilledSettings
import androidx.compose.material.icons.outlined.AccountBalanceWallet as OutlinedAccountBalanceWallet
import androidx.compose.material.icons.outlined.Home as OutlinedHome
import androidx.compose.material.icons.outlined.Settings as OutlinedSettings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen

private val MainNavigationInactiveColor = Color(0xFF737373)

private enum class MainTab(
    @param:StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(R.string.home_tab, Icons.Filled.FilledHome, Icons.Outlined.OutlinedHome),
    BUDGET_BOOK(
        R.string.budget_book_tab,
        Icons.Filled.FilledAccountBalanceWallet,
        Icons.Outlined.OutlinedAccountBalanceWallet,
    ),
    ANALYSIS(R.string.analysis_tab, Icons.Filled.FilledBarChart, Icons.Filled.FilledBarChart),
    SETTINGS(R.string.settings_tab, Icons.Filled.FilledSettings, Icons.Outlined.OutlinedSettings),
}

@Composable
fun MainScreen(onExit: () -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    val context = LocalContext.current
    val exitConfirmation = remember { ExitConfirmation() }
    BackHandler {
        if (exitConfirmation.shouldExit(SystemClock.elapsedRealtime())) {
            onExit()
        } else {
            Toast.makeText(context, R.string.exit_confirmation, Toast.LENGTH_SHORT).show()
        }
    }
    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            MainNavigationBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        },
    ) { innerPadding ->
        // 각 탭의 실제 화면은 후속 작업에서 구현합니다.
        Box(modifier = Modifier.fillMaxSize().background(Color.White).padding(innerPadding))
    }
}

@Composable
private fun MainNavigationBar(selectedTab: MainTab, onTabSelected: (MainTab) -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Surface(
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    contentColor = if (isSelected) SmartBudgetGreen else MainNavigationInactiveColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = stringResource(tab.label),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }
    }
}
