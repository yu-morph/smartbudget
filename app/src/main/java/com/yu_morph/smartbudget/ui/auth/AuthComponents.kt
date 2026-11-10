package com.yu_morph.smartbudget.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    isError: Boolean = false,
    onDone: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        placeholder = { Text(text = placeholder, fontSize = 13.sp) },
        textStyle = TextStyle(fontSize = 13.sp),
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    defaultKeyboardAction(ImeAction.Done)
                    onDone?.invoke()
                },
            ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SmartBudgetGreen,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            errorBorderColor = MaterialTheme.colorScheme.error,
        ),
    )
}

@Composable
internal fun PrimaryAuthButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SmartBudgetGreen),
    ) {
        Text(text)
    }
}
