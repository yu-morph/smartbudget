package com.yu_morph.smartbudget.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.components.BrandSignature

@Composable
fun LoginScreen(onLogin: () -> Unit, onSignUp: () -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = 46.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrandSignature(logoSize = 48.dp, textSize = 24)
        Spacer(Modifier.height(26.dp))
        Text(stringResource(R.string.login_title), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthTextField(username, { username = it }, stringResource(R.string.username_hint))
            AuthTextField(password, { password = it }, stringResource(R.string.password_hint), true)
        }
        Spacer(Modifier.height(20.dp))
        PrimaryAuthButton(stringResource(R.string.login_action), onLogin)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextButton(
                onClick = onSignUp,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(stringResource(R.string.sign_up_action), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(
                onClick = {},
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = stringResource(R.string.forgot_password),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}
