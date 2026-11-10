package com.yu_morph.smartbudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yu_morph.smartbudget.data.local.DisplayMode
import com.yu_morph.smartbudget.data.local.PreferenceStoreFactory
import com.yu_morph.smartbudget.ui.SmartBudgetApp
import com.yu_morph.smartbudget.ui.theme.SmartBudgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var displayMode by remember {
                mutableStateOf(PreferenceStoreFactory.appSettings(applicationContext).read().displayMode)
            }
            val useDarkTheme = when (displayMode) {
                DisplayMode.LIGHT -> false
                DisplayMode.DARK -> true
                DisplayMode.SYSTEM -> isSystemInDarkTheme()
            }
            SmartBudgetTheme(darkTheme = useDarkTheme) {
                SmartBudgetApp(onDisplayModeChanged = { displayMode = it }, onExit = ::finish)
            }
        }
    }
}
