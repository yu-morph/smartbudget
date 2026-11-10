package com.yu_morph.smartbudget.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.yu_morph.smartbudget.data.local.DisplayMode
import com.yu_morph.smartbudget.data.remote.SmartBudgetApiFactory
import com.yu_morph.smartbudget.ui.auth.AuthCoordinator
import com.yu_morph.smartbudget.ui.auth.AuthLoadingDialog
import com.yu_morph.smartbudget.ui.auth.LoginScreen
import com.yu_morph.smartbudget.ui.auth.SignUpScreen
import com.yu_morph.smartbudget.ui.document.MarkdownDocument
import com.yu_morph.smartbudget.ui.document.MarkdownViewerScreen
import com.yu_morph.smartbudget.ui.main.BudgetBookDetailScreen
import com.yu_morph.smartbudget.ui.main.MainScreen
import com.yu_morph.smartbudget.ui.main.SettingsCoordinator
import com.yu_morph.smartbudget.ui.main.SettingsDetailDestination
import com.yu_morph.smartbudget.ui.main.SettingsDetailScreen
import com.yu_morph.smartbudget.ui.splash.SplashScreen
import java.time.YearMonth

private enum class AppDestination { SPLASH, LOGIN, SIGN_UP, MAIN }

private const val MORPH_DURATION_MILLIS = 350
private const val DETAIL_TRANSITION_DURATION_MILLIS = 280
private const val BACKGROUND_PARALLAX_DIVISOR = 4

@Composable
@Suppress("LongMethod")
fun SmartBudgetApp(onDisplayModeChanged: (DisplayMode) -> Unit, onExit: () -> Unit) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.SPLASH) }
    var openDocument by rememberSaveable { mutableStateOf<MarkdownDocument?>(null) }
    var openSettings by rememberSaveable { mutableStateOf<SettingsDetailDestination?>(null) }
    var openBudgetBookMonth by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember(context) { SmartBudgetApiFactory.create(context.applicationContext) }
    val coordinator = remember(context, api, scope) { AuthCoordinator(context, api, scope) }
    val settingsCoordinator = remember(context, api, scope, onDisplayModeChanged) {
        SettingsCoordinator(context, api, scope, onDisplayModeChanged)
    }
    DisposableEffect(api) {
        onDispose(api::close)
    }
    BackHandler(enabled = destination == AppDestination.SIGN_UP) {
        destination = AppDestination.LOGIN
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AuthenticationContent(
            destination = destination,
            coordinator = coordinator,
            settingsCoordinator = settingsCoordinator,
            onDestinationChange = { destination = it },
            onOpenDocument = { openDocument = it },
            onOpenSettings = { openSettings = it },
            onOpenBudgetBook = { openBudgetBookMonth = it.toString() },
            onExit = onExit,
        )
        if (coordinator.isLoading) AuthLoadingDialog()
        AnimatedContent(
            targetState = openDocument,
            transitionSpec = { detailTransition(targetState != null) },
            label = "문서 화면 전환",
        ) { document ->
            document?.let {
                MarkdownViewerScreen(document = it, onBack = { openDocument = null })
            }
        }
        AnimatedContent(
            targetState = openSettings,
            transitionSpec = { detailTransition(targetState != null) },
            label = "설정 상세 화면 전환",
        ) { settings ->
            settings?.let {
                SettingsDetailScreen(
                    destination = it,
                    state = settingsCoordinator.state,
                    onBack = { openSettings = null },
                    onUpdateDisplayName = { name ->
                        settingsCoordinator.updateDisplayName(name) { openSettings = null }
                    },
                    onUpdatePassword = { current, new ->
                        settingsCoordinator.updatePassword(current, new) {
                            openSettings = null
                            coordinator.signOut()
                            destination = AppDestination.LOGIN
                        }
                    },
                    onLoadBudget = settingsCoordinator::loadMonthlyBudget,
                    onSaveBudget = settingsCoordinator::saveMonthlyBudget,
                    onLanguageSelected = settingsCoordinator::setLanguage,
                    onNotificationChanged = settingsCoordinator::setNotification,
                    onDisplayModeChanged = settingsCoordinator::setDisplayMode,
                    onVerifyWithdrawal = settingsCoordinator::verifyWithdrawalPassword,
                    onDeleteAccount = {
                        settingsCoordinator.deleteAccount {
                            openSettings = null
                            coordinator.signOut()
                            destination = AppDestination.LOGIN
                        }
                    },
                )
            }
        }
        AnimatedContent(
            targetState = openBudgetBookMonth,
            transitionSpec = { detailTransition(targetState != null) },
            label = "가계부 상세 화면 전환",
        ) { month ->
            month?.let {
                BudgetBookDetailScreen(
                    month = YearMonth.parse(it),
                    onMonthChange = { changedMonth -> openBudgetBookMonth = changedMonth.toString() },
                    onBack = { openBudgetBookMonth = null },
                )
            }
        }
        if (settingsCoordinator.state.isLoading) AuthLoadingDialog()
    }
}

private fun <S> androidx.compose.animation.AnimatedContentTransitionScope<S>.detailTransition(opening: Boolean) =
    if (opening) {
        (
            slideInHorizontally(tween(DETAIL_TRANSITION_DURATION_MILLIS)) { it } +
                fadeIn(tween(DETAIL_TRANSITION_DURATION_MILLIS))
            ) togetherWith
            (
                slideOutHorizontally(tween(DETAIL_TRANSITION_DURATION_MILLIS)) {
                    -it / BACKGROUND_PARALLAX_DIVISOR
                } +
                    fadeOut(tween(DETAIL_TRANSITION_DURATION_MILLIS))
                )
    } else {
        fadeIn(tween(DETAIL_TRANSITION_DURATION_MILLIS)) togetherWith
            (
                slideOutHorizontally(tween(DETAIL_TRANSITION_DURATION_MILLIS)) { it } +
                    fadeOut(tween(DETAIL_TRANSITION_DURATION_MILLIS))
                )
    }

@Composable
private fun AuthenticationContent(
    destination: AppDestination,
    coordinator: AuthCoordinator,
    settingsCoordinator: SettingsCoordinator,
    onDestinationChange: (AppDestination) -> Unit,
    onOpenDocument: (MarkdownDocument) -> Unit,
    onOpenSettings: (SettingsDetailDestination) -> Unit,
    onOpenBudgetBook: (YearMonth) -> Unit,
    onExit: () -> Unit,
) {
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
                settingsState = settingsCoordinator.state,
                onSettingsLoad = settingsCoordinator::refresh,
                onLogout = {
                    coordinator.signOut()
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
