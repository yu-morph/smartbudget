package com.yu_morph.smartbudget.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import com.yu_morph.smartbudget.data.remote.SmartBudgetApiFactory
import com.yu_morph.smartbudget.ui.auth.AuthCoordinator
import com.yu_morph.smartbudget.ui.auth.AuthLoadingDialog
import com.yu_morph.smartbudget.ui.auth.LoginScreen
import com.yu_morph.smartbudget.ui.auth.SignUpScreen
import com.yu_morph.smartbudget.ui.main.MainScreen
import com.yu_morph.smartbudget.ui.splash.SplashScreen

private enum class AppDestination { SPLASH, LOGIN, SIGN_UP, MAIN }

private const val MORPH_DURATION_MILLIS = 350

@Composable
fun SmartBudgetApp(onExit: () -> Unit) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.SPLASH) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember(context) { SmartBudgetApiFactory.create(context.applicationContext) }
    val coordinator = remember(context, api, scope) { AuthCoordinator(context, api, scope) }
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
            onDestinationChange = { destination = it },
            onExit = onExit,
        )
        if (coordinator.isLoading) AuthLoadingDialog()
    }
}

@Composable
private fun AuthenticationContent(
    destination: AppDestination,
    coordinator: AuthCoordinator,
    onDestinationChange: (AppDestination) -> Unit,
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
            )

            AppDestination.MAIN -> MainScreen(onExit = onExit)
        }
    }
}
