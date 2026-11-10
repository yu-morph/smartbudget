package com.yu_morph.smartbudget.ui.main

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet as FilledAccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart as FilledBarChart
import androidx.compose.material.icons.filled.Home as FilledHome
import androidx.compose.material.icons.filled.Settings as FilledSettings
import androidx.compose.material.icons.outlined.AccountBalanceWallet as OutlinedAccountBalanceWallet
import androidx.compose.material.icons.outlined.Home as OutlinedHome
import androidx.compose.material.icons.outlined.Settings as OutlinedSettings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.document.MarkdownDocument
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import java.time.YearMonth
import kotlinx.coroutines.launch

private val MainNavigationInactiveColor = Color(0xFF737373)
private val MainActionGradientStart = Color(0xFF18A874)
private val MainActionGradientEnd = Color(0xFF087A55)
private const val MAIN_TAB_PRESSED_SCALE = 0.88f
private const val TAB_TRANSITION_DURATION_MILLIS = 260
private const val TAB_SLIDE_DISTANCE_DIVISOR = 5

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
@Suppress("LongMethod")
fun MainScreen(
    settingsState: SettingsUiState,
    onSettingsLoad: () -> Unit,
    onLogout: () -> Unit,
    onOpenDocument: (MarkdownDocument) -> Unit,
    onOpenSettings: (SettingsDetailDestination) -> Unit,
    onOpenBudgetBook: (YearMonth) -> Unit,
    onExit: () -> Unit,
) {
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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MainNavigationBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    (
                        slideInHorizontally(tween(TAB_TRANSITION_DURATION_MILLIS)) {
                            it * direction / TAB_SLIDE_DISTANCE_DIVISOR
                        } +
                            fadeIn(tween(TAB_TRANSITION_DURATION_MILLIS))
                        ) togetherWith
                        (
                            slideOutHorizontally(tween(TAB_TRANSITION_DURATION_MILLIS)) {
                                -it * direction / TAB_SLIDE_DISTANCE_DIVISOR
                            } +
                                fadeOut(tween(TAB_TRANSITION_DURATION_MILLIS))
                            )
                },
                label = "메인 탭 화면 전환",
            ) { currentTab ->
                when (currentTab) {
                    MainTab.HOME -> HomeScreen(bottomPadding = innerPadding.calculateBottomPadding())

                    MainTab.BUDGET_BOOK -> BudgetBookRoute(
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        onOpenMonth = onOpenBudgetBook,
                    )

                    MainTab.ANALYSIS -> AnalysisScreen(
                        bottomPadding = innerPadding.calculateBottomPadding(),
                    )

                    MainTab.SETTINGS -> SettingsScreen(
                        state = settingsState,
                        bottomContentPadding = innerPadding.calculateBottomPadding(),
                        onLoad = onSettingsLoad,
                        onLogout = onLogout,
                        onOpenDocument = onOpenDocument,
                        onOpenSettings = onOpenSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun MainNavigationBar(selectedTab: MainTab, onTabSelected: (MainTab) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 16.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(68.dp).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MainTab.entries.take(2).forEach { tab ->
                    MainNavigationTab(tab, selectedTab == tab, onTabSelected)
                }
                Spacer(modifier = Modifier.size(58.dp))
                MainTab.entries.drop(2).forEach { tab ->
                    MainNavigationTab(tab, selectedTab == tab, onTabSelected)
                }
            }
        }
        MainActionButton(modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun MainNavigationTab(tab: MainTab, isSelected: Boolean, onTabSelected: (MainTab) -> Unit) {
    val scale = remember { Animatable(1f) }
    val animationScope = rememberCoroutineScope()
    Surface(
        onClick = {
            onTabSelected(tab)
            animationScope.launch {
                scale.animateTo(MAIN_TAB_PRESSED_SCALE, animationSpec = tween(durationMillis = 70))
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                )
            }
        },
        modifier = Modifier.size(56.dp).graphicsLayer(scaleX = scale.value, scaleY = scale.value),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        contentColor = if (isSelected) SmartBudgetGreen else MainNavigationInactiveColor,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(tab.label),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 12.sp,
            )
        }
    }
}

@Composable
private fun MainActionButton(modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = {
            // FAB 동작은 후속 작업에서 구현합니다.
        },
        modifier = modifier.size(58.dp),
        shape = CircleShape,
        containerColor = Color.Transparent,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(listOf(MainActionGradientStart, MainActionGradientEnd)),
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_logo_description),
                modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = 1.1f, scaleY = 1.1f),
            )
        }
    }
}
