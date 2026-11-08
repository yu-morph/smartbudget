package com.yu_morph.smartbudget.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.components.BrandSignature
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen

@Composable
fun SignUpScreen(onSignUp: () -> Unit, onLogin: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordConfirm by rememberSaveable { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 46.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        BrandSignature(logoSize = 48.dp, textSize = 24)
        Spacer(Modifier.height(56.dp))
        Text(stringResource(R.string.sign_up_title), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(34.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.name_label))
            AuthTextField(name, { name = it }, stringResource(R.string.username_hint))
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.password_label))
            AuthTextField(password, { password = it }, stringResource(R.string.password_rule_hint), true)
            AuthTextField(passwordConfirm, {
                passwordConfirm = it
            }, stringResource(R.string.password_confirm_hint), true)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = agreed,
                onCheckedChange = { agreed = it },
                colors = CheckboxDefaults.colors(checkedColor = SmartBudgetGreen),
            )
            Text(stringResource(R.string.privacy_agreement_prefix), fontSize = 13.sp)
            Text(stringResource(R.string.privacy_policy), color = SmartBudgetGreen, fontSize = 13.sp)
            Text(stringResource(R.string.privacy_agreement_suffix), fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryAuthButton(
            text = stringResource(R.string.sign_up_action),
            onClick = onSignUp,
            enabled = agreed && password == passwordConfirm,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.already_have_account), fontSize = 13.sp)
            TextButton(onClick = onLogin) {
                Text(stringResource(R.string.login_action), color = SmartBudgetGreen)
            }
        }
    }
}
