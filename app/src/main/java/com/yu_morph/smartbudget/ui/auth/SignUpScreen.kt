package com.yu_morph.smartbudget.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.components.BrandSignature
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreenLight

private val SignUpConditionColor = Color(0xFF555555)

@Composable
fun SignUpScreen(onSignUp: () -> Unit, onLogin: () -> Unit, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordConfirm by rememberSaveable { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 46.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandSignature(logoSize = 48.dp, textSize = 24)
            Spacer(Modifier.height(26.dp))
            Text(stringResource(R.string.sign_up_title), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(28.dp))
            SignUpInputFields(
                username = name,
                onUsernameChange = { name = it },
                password = password,
                onPasswordChange = { password = it },
                passwordConfirmation = passwordConfirm,
                onPasswordConfirmationChange = { passwordConfirm = it },
            )
            Spacer(Modifier.height(6.dp))
            PrivacyAgreementRow(
                agreed = agreed,
                onAgreementChange = { agreed = it },
                onOpenPolicy = { openExampleCustomTab(context) },
            )
            Spacer(Modifier.height(8.dp))
            PrimaryAuthButton(
                text = stringResource(R.string.sign_up_action),
                onClick = onSignUp,
                enabled = agreed && SignUpValidation.isValid(name, password, passwordConfirm),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.already_have_account), fontSize = 13.sp)
                TextButton(onClick = onLogin) {
                    Text(
                        text = stringResource(R.string.login_action),
                        color = SmartBudgetGreenLight,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
        }
        SignUpBackButton(onClick = onBack)
    }
}

@Composable
private fun SignUpInputFields(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordConfirmation: String,
    onPasswordConfirmationChange: (String) -> Unit,
) {
    var usernameEdited by rememberSaveable { mutableStateOf(false) }
    var passwordEdited by rememberSaveable { mutableStateOf(false) }
    var passwordConfirmationEdited by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.name_label))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AuthTextField(
                value = username,
                onValueChange = {
                    usernameEdited = true
                    onUsernameChange(it)
                },
                placeholder = stringResource(R.string.username_hint),
                isError = usernameEdited && !SignUpValidation.isValidUsername(username),
            )
            SignUpConditionLabel(text = stringResource(R.string.username_condition_label))
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.password_label))
        AuthTextField(
            value = password,
            onValueChange = {
                passwordEdited = true
                onPasswordChange(it)
            },
            placeholder = stringResource(R.string.password_rule_hint),
            isPassword = true,
            isError = passwordEdited && !SignUpValidation.isValidPassword(password),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AuthTextField(
                value = passwordConfirmation,
                onValueChange = {
                    passwordConfirmationEdited = true
                    onPasswordConfirmationChange(it)
                },
                placeholder = stringResource(R.string.password_confirm_hint),
                isPassword = true,
                isError = passwordConfirmationEdited && passwordConfirmation != password,
            )
            SignUpConditionLabel(text = stringResource(R.string.password_condition_label))
        }
    }
}

@Composable
private fun SignUpConditionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        color = SignUpConditionColor,
        fontSize = 11.sp
    )
}

@Composable
private fun SignUpBackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.back_to_login_description),
        )
    }
}

@Composable
private fun PrivacyAgreementRow(agreed: Boolean, onAgreementChange: (Boolean) -> Unit, onOpenPolicy: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundedAgreementCheckbox(
            checked = agreed,
            onCheckedChange = onAgreementChange,
            modifier = Modifier.offset(x = (-12).dp),
        )
        Row(
            modifier =
                Modifier
                    .offset(x = (-16).dp)
                    .toggleable(
                        value = agreed,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Checkbox,
                        onValueChange = onAgreementChange,
                    ),
        ) {
            Text(stringResource(R.string.privacy_agreement_prefix), fontSize = 13.sp)
            Text(
                text = stringResource(R.string.privacy_policy),
                modifier = Modifier.clickable(onClick = onOpenPolicy),
                color = SmartBudgetGreen,
                fontSize = 13.sp,
                textDecoration = TextDecoration.Underline,
            )
            Text(stringResource(R.string.privacy_agreement_suffix), fontSize = 13.sp)
        }
    }
}

@Composable
private fun RoundedAgreementCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .size(48.dp)
                .toggleable(
                    value = checked,
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, radius = 20.dp),
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .background(if (checked) SmartBudgetGreen else Color.Transparent, RoundedCornerShape(6.dp))
                    .border(
                        width = 1.dp,
                        color = if (checked) SmartBudgetGreen else AuthFieldBorderColor,
                        shape = RoundedCornerShape(6.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = Color.White,
                )
            }
        }
    }
}
