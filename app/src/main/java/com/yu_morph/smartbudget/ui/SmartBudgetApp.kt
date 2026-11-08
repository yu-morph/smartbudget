package com.yu_morph.smartbudget.ui

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

@Composable
fun SmartBudgetApp() {
    var destination by rememberSaveable { mutableStateOf(AppDestination.SPLASH) }
    when (destination) {
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
