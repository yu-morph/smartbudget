@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.remote.news.NewsArticle
import com.yu_morph.smartbudget.ui.document.MarkdownDocument
import com.yu_morph.smartbudget.ui.showAppToast
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import java.time.YearMonth
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.launch

private val MainNavigationInactiveColor = Color(0xFF737373)
private val MainActionGradientStart = Color(0xFF18A874)
private val MainActionGradientEnd = Color(0xFF087A55)
private const val MAIN_TAB_PRESSED_SCALE = 0.88f
private const val TAB_TRANSITION_DURATION_MILLIS = 260
private const val TAB_SLIDE_DISTANCE_DIVISOR = 5
private const val CELEBRATION_DURATION_MILLIS = 1100

private data class ConfettiParticle(
    val horizontalVelocity: Float,
    val launchVelocity: Float,
    val gravity: Float,
    val drift: Float,
    val size: Float,
    val color: Color,
)

internal enum class MainTab(
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
internal fun MainScreen(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    settingsState: SettingsUiState,
    onSettingsLoad: () -> Unit,
    budgetBookState: BudgetBookOverviewUiState,
    onBudgetBookLoad: (YearMonth) -> Unit,
    homeDashboardState: HomeDashboardUiState,
    onHomeDashboardLoad: () -> Unit,
    homeNewsState: HomeNewsUiState,
    onHomeNewsLoad: () -> Unit,
    onPreviousHomeNewsPage: () -> Unit,
    onNextHomeNewsPage: () -> Unit,
    onNewsArticleClick: (NewsArticle) -> Unit,
    onLogout: () -> Unit,
    onOpenDocument: (MarkdownDocument) -> Unit,
    onOpenSettings: (SettingsDetailDestination) -> Unit,
    onOpenBudgetBook: (YearMonth) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val exitConfirmation = remember { ExitConfirmation() }
    BackHandler {
        if (exitConfirmation.shouldExit(SystemClock.elapsedRealtime())) {
            onExit()
        } else {
            context.showAppToast(R.string.exit_confirmation)
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MainNavigationBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
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
                    MainTab.HOME -> HomeScreen(
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        dashboardState = homeDashboardState,
                        displayName = settingsState.displayName,
                        newsState = homeNewsState,
                        onDashboardLoad = onHomeDashboardLoad,
                        onProfileLoad = onSettingsLoad,
                        onNewsLoad = onHomeNewsLoad,
                        onPreviousNewsPage = onPreviousHomeNewsPage,
                        onNextNewsPage = onNextHomeNewsPage,
                        onArticleClick = onNewsArticleClick,
                    )

                    MainTab.BUDGET_BOOK -> BudgetBookRoute(
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        state = budgetBookState,
                        onLoad = onBudgetBookLoad,
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
    var celebrationId by remember { mutableIntStateOf(0) }
    var isCelebrating by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.padding(top = 12.dp).zIndex(1f),
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
        if (celebrationId > 0) {
            FabCelebration(
                celebrationId = celebrationId,
                onAnimationFinished = { isCelebrating = false },
            )
        }
        MainActionButton(
            onClick = {
                if (canStartFabCelebration(isCelebrating)) {
                    isCelebrating = true
                    celebrationId += 1
                }
            },
            modifier = Modifier.align(Alignment.TopCenter).zIndex(2f),
        )
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
private fun MainActionButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
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

@Composable
private fun FabCelebration(celebrationId: Int, onAnimationFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val navigationBarInset = WindowInsets.navigationBars.getBottom(density)
    val popupOffset = IntOffset(0, -(navigationBarInset + with(density) { 80.dp.roundToPx() }))
    val robotResources =
        remember {
            intArrayOf(
                R.drawable.fab_robot_jump_1,
                R.drawable.fab_robot_jump_2,
                R.drawable.fab_robot_jump_3,
                R.drawable.fab_robot_jump_4,
            )
        }
    val particles = remember(celebrationId) { createConfettiParticles(celebrationId) }
    LaunchedEffect(celebrationId) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(CELEBRATION_DURATION_MILLIS, easing = LinearEasing))
        onAnimationFinished()
    }
    val time = progress.value
    val verticalOffset = fabCelebrationVerticalOffset(time)
    val entranceAlpha = (time / 0.08f).coerceIn(0f, 1f)
    val exitAlpha = if (time < 0.82f) 1f else ((1f - time) / 0.18f).coerceIn(0f, 1f)
    val visibility = minOf(entranceAlpha, exitAlpha)
    val scale = 0.72f + 0.28f * sin(PI.toFloat() * time).coerceAtLeast(0f)
    Popup(
        alignment = Alignment.BottomCenter,
        offset = popupOffset,
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(250.dp).clipToBounds(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            ConfettiBurst(
                progress = time,
                particles = particles,
                modifier = Modifier.fillMaxSize(),
            )
            Image(
                painter = painterResource(robotResources[(celebrationId - 1) % robotResources.size]),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(118.dp)
                        .graphicsLayer(
                            translationY = verticalOffset * density.density,
                            scaleX = scale,
                            scaleY = scale,
                            alpha = visibility,
                        ),
            )
        }
    }
}

@Composable
private fun ConfettiBurst(progress: Float, particles: List<ConfettiParticle>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        particles.forEachIndexed { index, particle ->
            val delayedTime = ((progress - index * 0.006f) / 0.78f).coerceIn(0f, 1f)
            if (delayedTime <= 0f) return@forEachIndexed
            val originX = size.width * 0.5f
            val originY = size.height
            val x =
                originX + particle.horizontalVelocity.dp.toPx() * delayedTime + particle.drift.dp.toPx() * delayedTime
            val y =
                originY -
                    particle.launchVelocity.dp.toPx() * delayedTime +
                    particle.gravity.dp.toPx() * delayedTime * delayedTime
            val alpha = (1f - delayedTime).coerceIn(0f, 1f)
            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.size.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y),
            )
        }
    }
}

private fun createConfettiParticles(seed: Int): List<ConfettiParticle> {
    val colors = listOf(SmartBudgetGreen, Color(0xFFFFC857), Color(0xFF56B4E9), Color(0xFFFF6B6B), Color.White)
    val horizontalVelocities = confettiHorizontalVelocities()
    return List(horizontalVelocities.size) { index ->
        val direction = if (horizontalVelocities[index] < 0f) -1f else 1f
        ConfettiParticle(
            horizontalVelocity = horizontalVelocities[index],
            launchVelocity = 172f + ((index + seed) % 7) * 10f,
            gravity = 92f + (index % 5) * 8f,
            drift = direction * ((index + seed) % 3) * 5f,
            size = 2.2f + (index % 3),
            color = colors[index % colors.size],
        )
    }
}

internal fun canStartFabCelebration(isCelebrating: Boolean): Boolean = !isCelebrating

internal fun fabCelebrationVerticalOffset(progress: Float): Float = 112f - 748f * progress + 748f * progress * progress

internal fun confettiHorizontalVelocities(): List<Float> = List(44) { index ->
    val direction = if (index % 2 == 0) -1f else 1f
    val spread = 34f + (index / 2 % 11) * 11f
    direction * spread
}
