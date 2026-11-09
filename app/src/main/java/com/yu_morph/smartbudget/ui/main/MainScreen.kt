package com.yu_morph.smartbudget.ui.main

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet as FilledAccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart as FilledBarChart
import androidx.compose.material.icons.filled.Home as FilledHome
import androidx.compose.material.icons.filled.Settings as FilledSettings
import androidx.compose.material.icons.outlined.AccountBalanceWallet as OutlinedAccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart as OutlinedBarChart
import androidx.compose.material.icons.outlined.Home as OutlinedHome
import androidx.compose.material.icons.outlined.Settings as OutlinedSettings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    ANALYSIS(R.string.analysis_tab, Icons.Filled.FilledBarChart, Icons.Outlined.OutlinedBarChart),
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
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
            ) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                ) {
                    MainTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.label),
                                )
                            },
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor = SmartBudgetGreen,
                                    unselectedIconColor = MainNavigationInactiveColor,
                                    indicatorColor = Color.Transparent,
                                ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // 각 탭의 실제 화면은 후속 작업에서 구현합니다.
        Box(modifier = Modifier.fillMaxSize().background(Color.White).padding(innerPadding))
    }
}
