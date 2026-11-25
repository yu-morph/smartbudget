package com.yu_morph.smartbudget.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.local.DisplayMode
import com.yu_morph.smartbudget.data.ocr.RemoteReceiptOcrRepository
import com.yu_morph.smartbudget.data.remote.SmartBudgetApiFactory
import com.yu_morph.smartbudget.notification.BudgetNotificationCoordinator
import com.yu_morph.smartbudget.notification.hasNotificationPermission
import com.yu_morph.smartbudget.ui.auth.AuthCoordinator
import com.yu_morph.smartbudget.ui.auth.AuthLoadingDialog
import com.yu_morph.smartbudget.ui.auth.LoginScreen
import com.yu_morph.smartbudget.ui.auth.SignUpScreen
import com.yu_morph.smartbudget.ui.document.MarkdownDocument
import com.yu_morph.smartbudget.ui.document.MarkdownViewerScreen
import com.yu_morph.smartbudget.ui.main.AnalysisCoordinator
import com.yu_morph.smartbudget.ui.main.BudgetBookCoordinator
import com.yu_morph.smartbudget.ui.main.BudgetBookDetailScreen
import com.yu_morph.smartbudget.ui.main.HomeDashboardCoordinator
import com.yu_morph.smartbudget.ui.main.HomeNewsCoordinator
import com.yu_morph.smartbudget.ui.main.MainScreen
import com.yu_morph.smartbudget.ui.main.MainTab
import com.yu_morph.smartbudget.ui.main.NotificationSetting
import com.yu_morph.smartbudget.ui.main.SettingsCoordinator
import com.yu_morph.smartbudget.ui.main.SettingsDetailDestination
import com.yu_morph.smartbudget.ui.main.SettingsDetailScreen
import com.yu_morph.smartbudget.ui.splash.SplashScreen
import java.time.YearMonth

private enum class AppDestination { SPLASH, LOGIN, SIGN_UP, MAIN }

private enum class AppContent { BASE, DOCUMENT, SETTINGS_DETAIL, BUDGET_BOOK_DETAIL }

private const val MORPH_DURATION_MILLIS = 350
private const val DETAIL_TRANSITION_DURATION_MILLIS = 260
private const val DETAIL_SLIDE_DISTANCE_DIVISOR = 5

@Composable
@Suppress("LongMethod")
fun SmartBudgetApp(onDisplayModeChanged: (DisplayMode) -> Unit, onLanguageChanged: () -> Unit, onExit: () -> Unit) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.SPLASH) }
    var openDocument by rememberSaveable { mutableStateOf<MarkdownDocument?>(null) }
    var openSettings by rememberSaveable { mutableStateOf<SettingsDetailDestination?>(null) }
    var openBudgetBookMonth by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMainTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember(context) { SmartBudgetApiFactory.create(context.applicationContext) }
    val budgetNotifications = remember(context, api) { BudgetNotificationCoordinator(context, api) }
    val coordinator = remember(context, api, scope) { AuthCoordinator(context, api, scope) }
    val settingsCoordinator = remember(context, api, scope, budgetNotifications, onDisplayModeChanged) {
        SettingsCoordinator(context, api, scope, budgetNotifications, onDisplayModeChanged, onLanguageChanged)
    }
    val budgetBookCoordinator = remember(context, api, scope, budgetNotifications) {
        BudgetBookCoordinator(context, api, scope, budgetNotifications)
    }
    val homeDashboardCoordinator = remember(context, api, scope) { HomeDashboardCoordinator(context, api, scope) }
    val homeNewsCoordinator = remember(context, scope) { HomeNewsCoordinator(context, scope) }
    val analysisCoordinator = remember(context, api, scope) { AnalysisCoordinator(context, api, scope) }
    val receiptOcrRepository = remember(api) { RemoteReceiptOcrRepository(api.ocr) }
    val onNotificationChanged = rememberNotificationChangeHandler(destination, settingsCoordinator)
    DisposableEffect(api) {
        onDispose(api::close)
    }
    DisposableEffect(homeNewsCoordinator) {
        onDispose(homeNewsCoordinator::close)
    }
    DisposableEffect(analysisCoordinator) {
        onDispose(analysisCoordinator::close)
    }
    BackHandler(enabled = destination == AppDestination.SIGN_UP) {
        destination = AppDestination.LOGIN
    }
    val appContent = when {
        openDocument != null -> AppContent.DOCUMENT
        openSettings != null -> AppContent.SETTINGS_DETAIL
        openBudgetBookMonth != null -> AppContent.BUDGET_BOOK_DETAIL
        else -> AppContent.BASE
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        AnimatedContent(
            targetState = appContent,
            transitionSpec = { detailTransition(targetState != AppContent.BASE) },
            label = "앱 상세 화면 전환",
        ) { content ->
            when (content) {
                AppContent.BASE -> AuthenticationContent(
                    destination = destination,
                    coordinator = coordinator,
                    settingsCoordinator = settingsCoordinator,
                    budgetBookCoordinator = budgetBookCoordinator,
                    homeDashboardCoordinator = homeDashboardCoordinator,
                    homeNewsCoordinator = homeNewsCoordinator,
                    analysisCoordinator = analysisCoordinator,
                    selectedMainTab = selectedMainTab,
                    onMainTabSelected = { selectedMainTab = it },
                    onDestinationChange = { destination = it },
                    onOpenDocument = { openDocument = it },
                    onOpenSettings = { openSettings = it },
                    onOpenBudgetBook = { openBudgetBookMonth = it.toString() },
                    onExit = onExit,
                )

                AppContent.DOCUMENT -> openDocument?.let { document ->
                    MarkdownViewerScreen(document = document, onBack = { openDocument = null })
                }

                AppContent.SETTINGS_DETAIL -> openSettings?.let { settings ->
                    SettingsDetailScreen(
                        destination = settings,
                        state = settingsCoordinator.state,
                        onBack = { openSettings = null },
                        onSaveAccount = { name, current, new, budget, profileChanged, budgetChanged, onSaved ->
                            settingsCoordinator.saveAccountSettings(
                                name,
                                current,
                                new,
                                budget,
                                profileChanged,
                                budgetChanged,
                            ) { passwordChanged ->
                                onSaved()
                                afterProfileSaved(passwordChanged) {
                                    openSettings = null
                                    analysisCoordinator.reset()
                                    coordinator.signOut()
                                    selectedMainTab = MainTab.HOME
                                    destination = AppDestination.LOGIN
                                }
                            }
                        },
                        onProfileImageChanged = settingsCoordinator::updateProfileImage,
                        onLoadBudget = settingsCoordinator::loadMonthlyBudget,
                        onSyncBankTransactions = {
                            settingsCoordinator.syncBankTransactions { openSettings = null }
                        },
                        onLanguageSelected = settingsCoordinator::setLanguage,
                        onNotificationChanged = onNotificationChanged,
                        onDisplayModeChanged = settingsCoordinator::setDisplayMode,
                        onDeleteAccount = {
                            settingsCoordinator.deleteAccount {
                                openSettings = null
                                analysisCoordinator.reset()
                                coordinator.signOut()
                                selectedMainTab = MainTab.HOME
                                destination = AppDestination.LOGIN
                            }
                        },
                    )
                }

                AppContent.BUDGET_BOOK_DETAIL -> openBudgetBookMonth?.let { month ->
                    BudgetBookDetailScreen(
                        month = YearMonth.parse(month),
                        state = budgetBookCoordinator.state,
                        onLoadMonth = budgetBookCoordinator::loadMonth,
                        onCreateTransaction = budgetBookCoordinator::createTransaction,
                        onUpdateTransaction = budgetBookCoordinator::updateTransaction,
                        onDeleteTransaction = budgetBookCoordinator::deleteTransaction,
                        onMonthChange = { changedMonth -> openBudgetBookMonth = changedMonth.toString() },
                        onBack = { openBudgetBookMonth = null },
                        receiptOcrRepository = receiptOcrRepository,
                    )
                }
            }
        }
        if (coordinator.isLoading) AuthLoadingDialog()
        if (settingsCoordinator.state.isLoading) AuthLoadingDialog()
    }
}

@Composable
private fun rememberNotificationChangeHandler(
    destination: AppDestination,
    settingsCoordinator: SettingsCoordinator,
): (NotificationSetting, Boolean) -> Unit {
    val context = LocalContext.current
    var pendingNotificationType by remember { mutableStateOf<NotificationSetting?>(null) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) pendingNotificationType?.let { settingsCoordinator.setNotification(it, true) }
            pendingNotificationType = null
        }
    val budgetNotificationEnabled =
        settingsCoordinator.state.appSettings.budgetApproachingNotification ||
            settingsCoordinator.state.appSettings.budgetExceededNotification

    LaunchedEffect(destination, budgetNotificationEnabled) {
        if (shouldRequestNotificationPermission(destination, budgetNotificationEnabled, permissionRequested, context)) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    return { type, enabled ->
        if (
            enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission(context)
        ) {
            pendingNotificationType = type
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            settingsCoordinator.setNotification(type, enabled)
        }
    }
}

private fun shouldRequestNotificationPermission(
    destination: AppDestination,
    budgetNotificationEnabled: Boolean,
    permissionRequested: Boolean,
    context: android.content.Context,
): Boolean {
    var shouldRequest =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && destination == AppDestination.MAIN
    shouldRequest = shouldRequest && budgetNotificationEnabled
    shouldRequest = shouldRequest && !permissionRequested
    return shouldRequest && !hasNotificationPermission(context)
}

private inline fun afterProfileSaved(passwordChanged: Boolean, onPasswordChanged: () -> Unit) {
    if (passwordChanged) onPasswordChanged()
}

private fun <S> androidx.compose.animation.AnimatedContentTransitionScope<S>.detailTransition(opening: Boolean) = run {
    val direction = if (opening) 1 else -1
    (
        slideInHorizontally(tween(DETAIL_TRANSITION_DURATION_MILLIS)) {
            it * direction / DETAIL_SLIDE_DISTANCE_DIVISOR
        } +
            fadeIn(tween(DETAIL_TRANSITION_DURATION_MILLIS))
        ) togetherWith
        (
            slideOutHorizontally(tween(DETAIL_TRANSITION_DURATION_MILLIS)) {
                -it * direction / DETAIL_SLIDE_DISTANCE_DIVISOR
            } +
                fadeOut(tween(DETAIL_TRANSITION_DURATION_MILLIS))
            )
}

@Composable
@Suppress("LongMethod")
private fun AuthenticationContent(
    destination: AppDestination,
    coordinator: AuthCoordinator,
    settingsCoordinator: SettingsCoordinator,
    budgetBookCoordinator: BudgetBookCoordinator,
    homeDashboardCoordinator: HomeDashboardCoordinator,
    homeNewsCoordinator: HomeNewsCoordinator,
    analysisCoordinator: AnalysisCoordinator,
    selectedMainTab: MainTab,
    onMainTabSelected: (MainTab) -> Unit,
    onDestinationChange: (AppDestination) -> Unit,
    onOpenDocument: (MarkdownDocument) -> Unit,
    onOpenSettings: (SettingsDetailDestination) -> Unit,
    onOpenBudgetBook: (YearMonth) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            (
                fadeIn(tween(MORPH_DURATION_MILLIS)) +
                    scaleIn(tween(MORPH_DURATION_MILLIS), initialScale = 0.94f)
                ) togetherWith
                (
                    fadeOut(tween(MORPH_DURATION_MILLIS)) +
                        scaleOut(tween(MORPH_DURATION_MILLIS), targetScale = 1.04f)
                    )
        },
        label = "인증 화면 모핑",
    ) { currentDestination ->
        when (currentDestination) {
            AppDestination.SPLASH -> SplashScreen {
                coordinator.restoreSession(
                    onAuthenticated = { onDestinationChange(AppDestination.MAIN) },
                    onSignInRequired = { onDestinationChange(AppDestination.LOGIN) },
                )
            }

            AppDestination.LOGIN -> LoginScreen(
                onLogin = { username, password ->
                    coordinator.signIn(username, password) {
                        onDestinationChange(AppDestination.MAIN)
                    }
                },
                onSignUp = { onDestinationChange(AppDestination.SIGN_UP) },
            )

            AppDestination.SIGN_UP -> SignUpScreen(
                onSignUp = { displayName, username, password ->
                    coordinator.signUp(displayName, username, password) {
                        onDestinationChange(AppDestination.LOGIN)
                    }
                },
                onLogin = { onDestinationChange(AppDestination.LOGIN) },
                onOpenPrivacyPolicy = { onOpenDocument(MarkdownDocument.PRIVACY_POLICY) },
            )

            AppDestination.MAIN -> MainScreen(
                selectedTab = selectedMainTab,
                onTabSelected = onMainTabSelected,
                settingsState = settingsCoordinator.state,
                onSettingsLoad = settingsCoordinator::refresh,
                budgetBookState = budgetBookCoordinator.overviewState,
                onBudgetBookLoad = budgetBookCoordinator::loadOverview,
                homeDashboardState = homeDashboardCoordinator.state,
                onHomeDashboardLoad = homeDashboardCoordinator::load,
                homeNewsState = homeNewsCoordinator.state,
                onHomeNewsLoad = homeNewsCoordinator::load,
                onPreviousHomeNewsPage = homeNewsCoordinator::showPreviousEconomyPage,
                onNextHomeNewsPage = homeNewsCoordinator::showNextEconomyPage,
                onNewsArticleClick = { article ->
                    if (!launchAppCustomTab(context, article.link)) {
                        context.showAppToast(R.string.home_news_open_failed)
                    }
                },
                analysisState = analysisCoordinator.state,
                onAnalysisLoad = { analysisCoordinator.loadHistory() },
                onRequestAnalysis = analysisCoordinator::generate,
                onOpenAnalysisReport = analysisCoordinator::openReport,
                onClearAnalysisFailure = analysisCoordinator::clearFailure,
                onLogout = {
                    analysisCoordinator.reset()
                    coordinator.signOut()
                    onMainTabSelected(MainTab.HOME)
                    onDestinationChange(AppDestination.LOGIN)
                },
                onOpenDocument = onOpenDocument,
                onOpenSettings = onOpenSettings,
                onOpenBudgetBook = onOpenBudgetBook,
                onExit = onExit,
            )
        }
    }
}
