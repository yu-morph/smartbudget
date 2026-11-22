@file:Suppress("LongMethod", "MagicNumber", "MatchingDeclarationName", "TooManyFunctions")

package com.yu_morph.smartbudget.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.ocr.ReceiptOcrResult
import com.yu_morph.smartbudget.data.remote.model.CreateTransactionRequest
import com.yu_morph.smartbudget.data.remote.model.PatchValue
import com.yu_morph.smartbudget.data.remote.model.TransactionData
import com.yu_morph.smartbudget.data.remote.model.TransactionSource
import com.yu_morph.smartbudget.data.remote.model.TransactionType
import com.yu_morph.smartbudget.data.remote.model.UpdateTransactionRequest
import com.yu_morph.smartbudget.ui.receipt.ReceiptAnalyzingDialog
import com.yu_morph.smartbudget.ui.receipt.ReceiptCaptureDialog
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val EditorErrorColor = Color(0xFFD94A4A)
private const val MAX_AMOUNT_DIGITS = 9

internal data class TransactionEditorValue(
    val type: TransactionType,
    val category: String,
    val storeName: String,
    val note: String,
    val amount: Int,
)

internal fun TransactionEditorValue.toCreateRequest(date: LocalDate) = CreateTransactionRequest(
    date = date.toString(),
    type = type,
    source = TransactionSource.MANUAL,
    category = category,
    storeName = storeName.ifBlank { null },
    note = note.ifBlank { null },
    amount = amount,
)

internal fun TransactionEditorValue.toUpdateRequest(date: LocalDate) = UpdateTransactionRequest(
    date = date.toString(),
    type = type,
    category = category,
    storeName = PatchValue.Present(storeName.ifBlank { null }),
    note = PatchValue.Present(note.ifBlank { null }),
    amount = amount,
)

/** 영수증 OCR 결과를 반영한 뒤의 입력 필드 값. */
internal data class ReceiptFilledFields(
    val type: TransactionType,
    val category: String,
    val storeName: String,
    val amountText: String,
)

/**
 * 영수증 OCR 결과를 현재 입력값에 반영한다.
 *
 * 영수증은 지출이므로 거래 유형을 지출로 바꾸고, 수입 카테고리가 선택되어 있었다면 기본 지출 카테고리로 되돌린다.
 * 인식하지 못한 필드(`null`)는 사용자가 입력한 기존 값을 유지한다.
 */
internal fun applyReceiptOcrResult(
    result: ReceiptOcrResult,
    currentType: TransactionType,
    currentCategory: String,
    currentStoreName: String,
    currentAmountText: String,
): ReceiptFilledFields = ReceiptFilledFields(
    type = TransactionType.EXPENSE,
    category =
        if (currentType == TransactionType.EXPENSE) {
            currentCategory
        } else {
            expenseTransactionCategoryCodes.first()
        },
    storeName = result.storeName?.takeIf(String::isNotBlank) ?: currentStoreName,
    amountText = result.amount?.takeIf { it > 0 }?.toString() ?: currentAmountText,
)

/** 금액 숫자 문자열에 세 자리마다 쉼표를 넣는다. */
internal fun formatThousands(digits: String): String = buildString {
    digits.forEachIndexed { index, char ->
        if (index > 0 && (digits.length - index) % 3 == 0) append(',')
        append(char)
    }
}

/** 영수증 OCR 진행 상태. */
private enum class ReceiptOcrStatus { IDLE, PROCESSING, APPLIED, FAILED }

/**
 * 가계부 내역 추가/수정 다이얼로그.
 *
 * 카메라 버튼으로 영수증을 촬영하면 [onRecognizeReceipt]로 OCR을 요청하고, 결과를 입력 필드에 채운다.
 * OCR 결과는 자동 저장하지 않으며 사용자가 확인한 뒤 "저장하기"를 눌러야 저장된다.
 */
@Composable
internal fun TransactionEditorDialog(
    transaction: TransactionData?,
    isSaving: Boolean,
    onSave: (TransactionEditorValue) -> Unit,
    onDismiss: () -> Unit,
    onRecognizeReceipt: suspend (ByteArray) -> Result<ReceiptOcrResult>,
) {
    var type by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.type ?: TransactionType.EXPENSE) }
    var category by rememberSaveable(transaction?.id) {
        mutableStateOf(transaction?.category ?: expenseTransactionCategoryCodes.first())
    }
    var storeName by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.storeName.orEmpty()) }
    var note by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.note.orEmpty()) }
    var amountText by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.amount?.toString().orEmpty()) }
    var isCategoryExpanded by rememberSaveable { mutableStateOf(false) }
    var showCamera by rememberSaveable { mutableStateOf(false) }
    var ocrStatus by remember { mutableStateOf(ReceiptOcrStatus.IDLE) }
    var ocrJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val amount = amountText.toIntOrNull()
    val isBusy = isSaving || ocrStatus == ReceiptOcrStatus.PROCESSING
    val categories =
        if (type == TransactionType.INCOME) incomeTransactionCategoryCodes else expenseTransactionCategoryCodes

    Dialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).imePadding(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = 640.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, top = 12.dp, end = 12.dp, bottom = 20.dp),
            ) {
                EditorHeader(
                    isEditing = transaction != null,
                    isBusy = isBusy,
                    onCameraClick = { showCamera = true },
                    onClose = onDismiss,
                )
                Column(
                    modifier = Modifier.padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TransactionTypeSelector(type = type, enabled = !isBusy) { selectedType ->
                        if (selectedType != type) {
                            type = selectedType
                            category =
                                if (selectedType == TransactionType.INCOME) {
                                    incomeTransactionCategoryCodes.first()
                                } else {
                                    expenseTransactionCategoryCodes.first()
                                }
                        }
                    }
                    ReceiptOcrStatusBanner(ocrStatus)
                    EditorField(label = stringResource(R.string.budget_book_editor_category)) {
                        CategoryField(
                            category = category,
                            expanded = isCategoryExpanded,
                            enabled = !isBusy,
                            onClick = { isCategoryExpanded = !isCategoryExpanded },
                        )
                        AnimatedVisibility(visible = isCategoryExpanded) {
                            CategoryOptions(
                                categories = categories,
                                selected = category,
                                onSelect = {
                                    category = it
                                    isCategoryExpanded = false
                                },
                            )
                        }
                    }
                    EditorField(
                        label =
                            stringResource(
                                if (type == TransactionType.INCOME) {
                                    R.string.budget_book_editor_income_source
                                } else {
                                    R.string.budget_book_editor_store_name
                                },
                            ),
                    ) {
                        EditorTextField(value = storeName, onValueChange = { storeName = it }, enabled = !isBusy)
                    }
                    EditorField(label = stringResource(R.string.budget_book_editor_note)) {
                        EditorTextField(value = note, onValueChange = { note = it }, enabled = !isBusy)
                    }
                    EditorField(label = stringResource(R.string.budget_book_amount)) {
                        EditorTextField(
                            value = amountText,
                            onValueChange = { amountText = it.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS) },
                            enabled = !isBusy,
                            keyboardType = KeyboardType.Number,
                            suffix = stringResource(R.string.settings_won),
                            visualTransformation = ThousandsSeparatorTransformation,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    SaveButton(
                        enabled = amount != null && amount > 0 && !isBusy,
                        isSaving = isSaving,
                        onClick = {
                            onSave(
                                TransactionEditorValue(
                                    type = type,
                                    category = category,
                                    storeName = storeName,
                                    note = note,
                                    amount = checkNotNull(amount),
                                ),
                            )
                        },
                    )
                }
            }
        }
    }

    if (showCamera) {
        ReceiptCaptureDialog(
            onDismiss = { showCamera = false },
            onConfirm = { jpegBytes ->
                showCamera = false
                ocrStatus = ReceiptOcrStatus.PROCESSING
                ocrJob =
                    scope.launch {
                        val result = onRecognizeReceipt(jpegBytes)
                        // 사용자가 로딩 중에 취소했다면 결과를 반영하지 않는다.
                        if (!isActive) return@launch
                        result
                            .onSuccess { ocrResult ->
                                val filled = applyReceiptOcrResult(ocrResult, type, category, storeName, amountText)
                                type = filled.type
                                category = filled.category
                                storeName = filled.storeName
                                amountText = filled.amountText
                                ocrStatus = ReceiptOcrStatus.APPLIED
                            }.onFailure {
                                ocrStatus = ReceiptOcrStatus.FAILED
                            }
                    }
            },
        )
    }

    if (ocrStatus == ReceiptOcrStatus.PROCESSING) {
        ReceiptAnalyzingDialog(
            onCancel = {
                ocrJob?.cancel()
                ocrJob = null
                ocrStatus = ReceiptOcrStatus.IDLE
            },
        )
    }
}

@Composable
private fun EditorHeader(isEditing: Boolean, isBusy: Boolean, onCameraClick: () -> Unit, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text =
                stringResource(
                    if (isEditing) R.string.budget_book_editor_edit_title else R.string.budget_book_editor_add_title,
                ),
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        HeaderIconButton(
            icon = Icons.Outlined.CameraAlt,
            contentDescription = stringResource(R.string.receipt_ocr_open),
            enabled = !isBusy,
            tint = SmartBudgetGreen,
            onClick = onCameraClick,
        )
        HeaderIconButton(
            icon = Icons.Outlined.Close,
            contentDescription = stringResource(R.string.close_action),
            enabled = !isBusy,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onClose,
        )
    }
}

/** 가계부 Bottom Sheet의 + 버튼과 같은 모양의 원형 아이콘 버튼. 터치 영역은 48dp를 유지한다. */
@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Box(
            modifier = Modifier.size(34.dp).background(editorMutedBackground(), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp),
                tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            )
        }
    }
}

@Composable
private fun TransactionTypeSelector(type: TransactionType, enabled: Boolean, onSelect: (TransactionType) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { option ->
            val selected = option == type
            Surface(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(CircleShape)
                        .clickable(enabled = enabled, role = Role.Tab) { onSelect(option) },
                shape = CircleShape,
                color = if (selected) SmartBudgetGreen else editorMutedBackground(),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text =
                            stringResource(
                                if (option == TransactionType.EXPENSE) {
                                    R.string.budget_book_expense
                                } else {
                                    R.string.budget_book_income
                                },
                            ),
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptOcrStatusBanner(status: ReceiptOcrStatus) {
    val (message, color) =
        when (status) {
            ReceiptOcrStatus.IDLE -> return

            ReceiptOcrStatus.PROCESSING ->
                stringResource(R.string.receipt_ocr_processing) to MaterialTheme.colorScheme.onSurfaceVariant

            ReceiptOcrStatus.APPLIED -> stringResource(R.string.receipt_ocr_applied) to SmartBudgetGreen

            ReceiptOcrStatus.FAILED -> stringResource(R.string.receipt_ocr_failed) to EditorErrorColor
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (status) {
            ReceiptOcrStatus.PROCESSING ->
                CircularProgressIndicator(color = SmartBudgetGreen, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))

            ReceiptOcrStatus.APPLIED ->
                Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(16.dp), tint = color)

            else -> Icon(Icons.Outlined.ErrorOutline, null, modifier = Modifier.size(16.dp), tint = color)
        }
        Text(message, color = color, fontSize = 13.sp)
    }
}

/** 시안의 "작은 회색 라벨 + 둥근 입력칸" 형태를 만든다. */
@Composable
private fun EditorField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val containerColor = editorMutedBackground()
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
        suffix = suffix?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) } },
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = SmartBudgetGreen,
            ),
    )
}

/** 선택된 카테고리를 입력칸 모양으로 보여 주고, 누르면 카테고리 목록을 펼친다. */
@Composable
private fun CategoryField(category: String, expanded: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val categoryUi = transactionCategoryUi(category)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(editorMutedBackground())
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.budget_book_editor_select_category),
                    onClick = onClick,
                ).padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(28.dp).background(categoryUi.color.copy(alpha = 0.14f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(categoryUi.icon, null, modifier = Modifier.size(16.dp), tint = categoryUi.color)
        }
        Text(
            stringResource(categoryUi.label),
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryOptions(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.chunked(2).forEach { rowCategories ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCategories.forEach { code ->
                    CategoryOption(
                        code = code,
                        selected = selected == code,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(code) },
                    )
                }
                // 홀수 개일 때 마지막 칸이 전체 너비를 차지하지 않도록 빈 칸을 둔다.
                if (rowCategories.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryOption(code: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val category = transactionCategoryUi(code)
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) category.color.copy(alpha = 0.16f) else editorMutedBackground(),
        border = if (selected) BorderStroke(1.dp, category.color) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(category.icon, null, modifier = Modifier.size(16.dp), tint = category.color)
            Text(stringResource(category.label), fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SaveButton(enabled: Boolean, isSaving: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = SmartBudgetGreen,
                contentColor = Color.White,
                disabledContainerColor = SmartBudgetGreen.copy(alpha = 0.35f),
                disabledContentColor = Color.White,
            ),
    ) {
        if (isSaving) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(
                stringResource(R.string.budget_book_editor_save),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 금액 입력칸에 세 자리마다 쉼표를 표시한다. 실제 값은 숫자만 유지한다. */
private object ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = formatThousands(digits)
        val offsetMapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    val commas = (1 until offset).count { index -> (digits.length - index) % 3 == 0 }
                    return offset + commas
                }

                override fun transformedToOriginal(offset: Int): Int = formatted.take(offset).count { it != ',' }
            }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
internal fun DeleteTransactionDialog(isDeleting: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text(stringResource(R.string.budget_book_delete_confirm_title)) },
        text = { Text(stringResource(R.string.budget_book_delete_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                Text(stringResource(R.string.budget_book_delete_action), color = EditorErrorColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(
                    stringResource(R.string.budget_book_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun editorMutedBackground(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFFF3F3F3) else Color(0xFF252525)
