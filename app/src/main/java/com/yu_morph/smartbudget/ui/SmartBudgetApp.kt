package com.yu_morph.smartbudget.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.yu_morph.smartbudget.ui.auth.LoginScreen
import com.yu_morph.smartbudget.ui.auth.SignUpScreen
import com.yu_morph.smartbudget.ui.main.MainScreen
import com.yu_morph.smartbudget.ui.splash.SplashScreen

private enum class AppDestination { SPLASH, LOGIN, SIGN_UP, MAIN }

private const val MORPH_DURATION_MILLIS = 350

@Composable
fun SmartBudgetApp() {
    var destination by rememberSaveable { mutableStateOf(AppDestination.SPLASH) }
    BackHandler(enabled = destination == AppDestination.SIGN_UP) {
        destination = AppDestination.LOGIN
    }
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
            AppDestination.SPLASH -> SplashScreen { destination = AppDestination.LOGIN }

            AppDestination.LOGIN -> LoginScreen(
                onLogin = { destination = AppDestination.MAIN },
                onSignUp = { destination = AppDestination.SIGN_UP },
            )

            AppDestination.SIGN_UP -> SignUpScreen(
                onSignUp = { destination = AppDestination.LOGIN },
                onLogin = { destination = AppDestination.LOGIN },
            )

            AppDestination.MAIN -> MainScreen()
        }
    }
}
