@file:Suppress("LongMethod", "MagicNumber", "TooManyFunctions")

package com.yu_morph.smartbudget.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.ocr.FakeReceiptOcrRepository
import com.yu_morph.smartbudget.data.ocr.ReceiptOcrRepository
import com.yu_morph.smartbudget.data.remote.model.CreateTransactionRequest
import com.yu_morph.smartbudget.data.remote.model.TransactionData
import com.yu_morph.smartbudget.data.remote.model.TransactionType
import com.yu_morph.smartbudget.data.remote.model.UpdateTransactionRequest
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

private val CalendarIncomeColor = Color(0xFFD94A4A)
private val CalendarExpenseColor = Color(0xFF3278C7)
private val CalendarSundayColor = Color(0xFFD94A4A)
private val CalendarSaturdayColor = Color(0xFF3278C7)
private val CalendarWarningColor = Color(0xFFF59E0B)
private const val MONTH_SWIPE_THRESHOLD = 72f
private const val MONTH_TRANSITION_DURATION_MILLIS = 240

internal data class MonthlyTransactionAmounts(val income: Int, val expense: Int)

internal data class CategoryAmountStat(val category: String, val amount: Int)

internal data class MonthlyLedgerStats(
    val amounts: MonthlyTransactionAmounts,
    val largestExpenseCategories: List<CategoryAmountStat>,
    val expenseRatioPercent: Int?,
    val expenseChange: Int,
)

internal fun monthlyTransactionAmounts(transactions: List<TransactionData>): MonthlyTransactionAmounts =
    transactions.fold(MonthlyTransactionAmounts(income = 0, expense = 0)) { total, transaction ->
        when (transaction.type) {
            TransactionType.INCOME -> total.copy(income = total.income + transaction.amount)
            TransactionType.EXPENSE -> total.copy(expense = total.expense + transaction.amount)
        }
    }

internal fun monthlyLedgerStats(
    transactions: List<TransactionData>,
    previousMonthTransactions: List<TransactionData>,
): MonthlyLedgerStats {
    val amounts = monthlyTransactionAmounts(transactions)
    val previousAmounts = monthlyTransactionAmounts(previousMonthTransactions)
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    val largestCategories =
        expenses
            .groupBy(TransactionData::category)
            .map { (category, items) -> CategoryAmountStat(category, items.sumOf(TransactionData::amount)) }
            .sortedWith(compareByDescending<CategoryAmountStat> { it.amount }.thenBy { it.category })
            .take(3)
    return MonthlyLedgerStats(
        amounts = amounts,
        largestExpenseCategories = largestCategories,
        expenseRatioPercent =
            amounts.income.takeIf { it > 0 }?.let { income ->
                ((amounts.expense.toLong() * 100) / income).toInt()
            },
        expenseChange = amounts.expense - previousAmounts.expense,
    )
}

@Composable
internal fun BudgetBookRoute(
    bottomPadding: Dp,
    state: BudgetBookOverviewUiState,
    onLoad: (YearMonth) -> Unit,
    onOpenMonth: (YearMonth) -> Unit,
) {
    val latestMonth = remember { YearMonth.now() }
    LaunchedEffect(Unit) { onLoad(latestMonth) }
    BudgetBookOverview(
        bottomPadding = bottomPadding,
        state = state,
        latestMonth = latestMonth,
        onPreviousMonthChange = onLoad,
        onOpenMonth = onOpenMonth,
    )
}

@Composable
private fun BudgetBookOverview(
    bottomPadding: Dp,
    state: BudgetBookOverviewUiState,
    latestMonth: YearMonth,
    onPreviousMonthChange: (YearMonth) -> Unit,
    onOpenMonth: (YearMonth) -> Unit,
) {
    val currentMonth = YearMonth.now()
    val currentTransactions = state.transactionsByMonth[currentMonth].orEmpty()
    val currentTransactionsByDate =
        remember(currentTransactions) { currentTransactions.groupBy { LocalDate.parse(it.date) } }
    val displayedPreviousMonth = state.referenceMonth?.coerceAtMost(latestMonth) ?: latestMonth
    val previousTransactions = state.transactionsByMonth[displayedPreviousMonth].orEmpty()
    val previousStats =
        monthlyLedgerStats(
            transactions = previousTransactions,
            previousMonthTransactions = state.transactionsByMonth[displayedPreviousMonth.minusMonths(1)].orEmpty(),
        )
    MainTabContainer(bottomPadding = bottomPadding) {
        LedgerCard(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onOpenMonth(currentMonth) },
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        stringResource(R.string.budget_book_current_month),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    Text(monthTitle(currentMonth), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { onOpenMonth(currentMonth) }) {
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = stringResource(R.string.budget_book_view_all),
                        tint = SettingsChevronColor,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(overviewCalendarBackground())
                        .padding(12.dp),
            ) {
                MonthCalendar(
                    month = currentMonth,
                    compact = true,
                    modifier = Modifier.fillMaxWidth(),
                    transactionsByDate = currentTransactionsByDate,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        LedgerCard(contentPadding = PaddingValues(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.budget_book_monthly_statistics),
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                PreviousMonthNavigation(
                    month = displayedPreviousMonth,
                    latestMonth = latestMonth,
                    onPrevious = { onPreviousMonthChange(displayedPreviousMonth.minusMonths(1)) },
                    onNext = { onPreviousMonthChange(displayedPreviousMonth.plusMonths(1)) },
                )
            }
            Spacer(Modifier.height(10.dp))
            MonthlyStatisticsCardContent(
                stats = previousStats,
                budgetAmount = state.selectedBudgetAmount,
                onClick = { onOpenMonth(displayedPreviousMonth) },
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetBookDetailScreen(
    month: YearMonth,
    state: BudgetBookUiState,
    onLoadMonth: (YearMonth) -> Unit,
    onCreateTransaction: (CreateTransactionRequest, () -> Unit) -> Unit,
    onUpdateTransaction: (Int, UpdateTransactionRequest, () -> Unit) -> Unit,
    onDeleteTransaction: (Int, () -> Unit) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onBack: () -> Unit,
    // OCR API가 구현되면 실제 구현을 주입한다. 그 전까지는 고정 더미 결과를 사용한다.
    receiptOcrRepository: ReceiptOcrRepository = remember { FakeReceiptOcrRepository() },
) {
    var selectedDateText by rememberSaveable { mutableStateOf<String?>(null) }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }
    var editorTransaction by remember { mutableStateOf<TransactionData?>(null) }
    var showTransactionEditor by rememberSaveable { mutableStateOf(false) }
    var deletingTransaction by remember { mutableStateOf<TransactionData?>(null) }
    val transactions = if (state.month == month) state.transactions else emptyList()
    val transactionsByDate = remember(transactions) { transactions.groupBy { LocalDate.parse(it.date) } }
    val selectedDate = selectedDateText?.let(LocalDate::parse)
    LaunchedEffect(month) {
        selectedDateText = null
        onLoadMonth(month)
    }
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                expandedHeight = 52.dp,
                title = {
                    Text(
                        text = stringResource(R.string.budget_book_detail),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.navigate_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MonthSelector(month, onMonthChange, onTitleClick = { showMonthPicker = true })
                MonthSummary(transactions)
                AnimatedContent(
                    targetState = month,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (
                            slideInHorizontally(tween(MONTH_TRANSITION_DURATION_MILLIS)) { it * direction } +
                                fadeIn(tween(MONTH_TRANSITION_DURATION_MILLIS))
                            ) togetherWith
                            (
                                slideOutHorizontally(tween(MONTH_TRANSITION_DURATION_MILLIS)) { -it * direction } +
                                    fadeOut(tween(MONTH_TRANSITION_DURATION_MILLIS))
                                )
                    },
                    label = "가계부 월 전환",
                ) { displayedMonth ->
                    MonthCalendar(
                        month = displayedMonth,
                        compact = false,
                        transactionsByDate = if (displayedMonth == month) transactionsByDate else emptyMap(),
                        selectedDate = selectedDate?.takeIf { YearMonth.from(it) == displayedMonth },
                        onDateClick = { date ->
                            if (YearMonth.from(date) == displayedMonth) {
                                selectedDateText = date.toString()
                            } else {
                                onMonthChange(YearMonth.from(date))
                            }
                        },
                        onSwipeMonth = onMonthChange,
                        modifier = Modifier.fillMaxSize(),
                        fillAvailableHeight = true,
                    )
                }
            }
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SmartBudgetGreen, strokeWidth = 3.dp)
                }
            }
        }
    }
    if (showMonthPicker) {
        MonthPickerDialog(
            selectedMonth = month,
            onSelect = {
                showMonthPicker = false
                onMonthChange(it)
            },
            onDismiss = { showMonthPicker = false },
        )
    }
    if (selectedDate != null) {
        DailyTransactionsSheet(
            date = selectedDate,
            transactions = transactionsByDate[selectedDate].orEmpty(),
            onAdd = {
                editorTransaction = null
                showTransactionEditor = true
            },
            onEdit = {
                editorTransaction = it
                showTransactionEditor = true
            },
            onDelete = { deletingTransaction = it },
            onDismiss = { selectedDateText = null },
        )
    }
    if (showTransactionEditor && selectedDate != null) {
        TransactionEditorDialog(
            transaction = editorTransaction,
            isSaving = state.isMutating,
            onDismiss = { showTransactionEditor = false },
            onSave = { value ->
                val onSuccess = { showTransactionEditor = false }
                editorTransaction?.let { transaction ->
                    onUpdateTransaction(transaction.id, value.toUpdateRequest(selectedDate), onSuccess)
                } ?: onCreateTransaction(value.toCreateRequest(selectedDate), onSuccess)
            },
            onRecognizeReceipt = receiptOcrRepository::recognize,
        )
    }
    deletingTransaction?.let { transaction ->
        DeleteTransactionDialog(
            isDeleting = state.isMutating,
            onDismiss = { deletingTransaction = null },
            onConfirm = {
                onDeleteTransaction(transaction.id) { deletingTransaction = null }
            },
        )
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    compact: Boolean,
    modifier: Modifier = Modifier,
    fillAvailableHeight: Boolean = false,
    transactionsByDate: Map<LocalDate, List<TransactionData>> = emptyMap(),
    selectedDate: LocalDate? = null,
    onDateClick: ((LocalDate) -> Unit)? = null,
    onSwipeMonth: ((YearMonth) -> Unit)? = null,
) {
    val days = remember(month) { calendarDays(month) }
    val cellHeight = if (compact) 52.dp else 72.dp
    val mutedBackground = calendarMutedBackground()
    val borderColor = calendarBorderColor()
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    val swipeModifier =
        if (onSwipeMonth == null) {
            Modifier
        } else {
            Modifier.pointerInput(month) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDrag = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        horizontalDrag += dragAmount
                    },
                    onDragEnd = {
                        when {
                            horizontalDrag > MONTH_SWIPE_THRESHOLD -> onSwipeMonth(month.minusMonths(1))
                            horizontalDrag < -MONTH_SWIPE_THRESHOLD -> onSwipeMonth(month.plusMonths(1))
                        }
                        horizontalDrag = 0f
                    },
                    onDragCancel = { horizontalDrag = 0f },
                )
            }
        }
    Column(modifier = modifier.then(swipeModifier)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(if (compact) Color.Transparent else mutedBackground)
                    .padding(
                        top = if (compact) 0.dp else 2.dp,
                        bottom = if (compact) 8.dp else 2.dp,
                    ),
        ) {
            listOf(
                R.string.weekday_sunday,
                R.string.weekday_monday,
                R.string.weekday_tuesday,
                R.string.weekday_wednesday,
                R.string.weekday_thursday,
                R.string.weekday_friday,
                R.string.weekday_saturday,
            ).forEachIndexed { index, day ->
                Text(
                    stringResource(day),
                    modifier = Modifier.weight(1f),
                    color =
                        when (index) {
                            0 -> CalendarSundayColor
                            6 -> CalendarSaturdayColor
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = borderColor)
        days.chunked(7).forEach { week ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(if (fillAvailableHeight) Modifier.weight(1f) else Modifier),
            ) {
                week.forEach { date ->
                    CalendarDay(
                        date = date,
                        displayedMonth = month,
                        transactions = transactionsByDate[date].orEmpty(),
                        isSelected = date == selectedDate,
                        mutedBackground = mutedBackground,
                        borderColor = borderColor,
                        height = cellHeight,
                        compact = compact,
                        fillAvailableHeight = fillAvailableHeight,
                        onClick = onDateClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDay(
    date: LocalDate,
    displayedMonth: YearMonth,
    transactions: List<TransactionData>,
    isSelected: Boolean,
    mutedBackground: Color,
    borderColor: Color,
    height: Dp,
    compact: Boolean,
    fillAvailableHeight: Boolean,
    onClick: ((LocalDate) -> Unit)?,
) {
    val isOutsideMonth = YearMonth.from(date) != displayedMonth
    val interactionModifier = if (onClick == null) Modifier else Modifier.clickable { onClick(date) }
    Column(
        modifier =
            Modifier
                .weight(1f)
                .calendarCellStyle(
                    height = height,
                    fillAvailableHeight = fillAvailableHeight,
                    compact = compact,
                    isOutsideMonth = isOutsideMonth,
                    isSelected = isSelected,
                    mutedBackground = mutedBackground,
                    borderColor = borderColor,
                )
                .then(interactionModifier)
                .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        CalendarDateLabel(date, compact, isOutsideMonth)
        if (!isOutsideMonth) CategoryIndicators(transactions, compact)
        if (!compact) Spacer(Modifier.weight(1f))
        if (!compact && !isOutsideMonth) DailyAmounts(transactions)
    }
}

@Composable
private fun CategoryIndicators(transactions: List<TransactionData>, compact: Boolean) {
    val categories = transactions.distinctBy(TransactionData::category)
    val topPaddingSize = if (compact) 4.dp else 8.dp
    if (categories.isNotEmpty()) {
        Row(
            modifier = Modifier.padding(start = 2.dp, top = topPaddingSize),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            categories.forEach { transaction ->
                var backgroundColor = transactionCategoryUi(transaction.category).color

                if (compact) {
                    backgroundColor = backgroundColor.copy(alpha = 0.8f)
                }

                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(backgroundColor),
                )
            }
        }
    }
}

private fun Modifier.calendarCellStyle(
    height: Dp,
    fillAvailableHeight: Boolean,
    compact: Boolean,
    isOutsideMonth: Boolean,
    isSelected: Boolean,
    mutedBackground: Color,
    borderColor: Color,
): Modifier {
    val sizedModifier = if (fillAvailableHeight) Modifier.fillMaxHeight() else Modifier.height(height)
    val borderedModifier =
        if (compact) {
            Modifier
        } else if (isSelected) {
            Modifier.border(width = 1.5.dp, color = SmartBudgetGreen)
        } else {
            Modifier.drawBehind {
                val strokeWidth = 0.5.dp.toPx()
                val verticalX = size.width - strokeWidth / 2
                val horizontalY = size.height - strokeWidth / 2
                drawLine(
                    color = borderColor,
                    start = Offset(verticalX, 0f),
                    end = Offset(verticalX, size.height),
                    strokeWidth = strokeWidth,
                )
                drawLine(
                    color = borderColor,
                    start = Offset(0f, horizontalY),
                    end = Offset(size.width, horizontalY),
                    strokeWidth = strokeWidth,
                )
            }
        }
    val backgroundColor = when {
        isSelected -> SmartBudgetGreen.copy(alpha = 0.1f)
        isOutsideMonth && !compact -> mutedBackground
        else -> Color.Transparent
    }
    return then(sizedModifier).background(backgroundColor).then(borderedModifier)
}

@Composable
private fun CalendarDateLabel(date: LocalDate, compact: Boolean, isOutsideMonth: Boolean) {
    val isToday = date == LocalDate.now()
    Box(
        modifier =
            Modifier
                .size(if (compact) 24.dp else 26.dp)
                .background(
                    if (isToday) Color.Black else Color.Transparent,
                    CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            color = calendarDayTextColor(date, isToday, isOutsideMonth),
            fontSize = 12.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun calendarDayTextColor(date: LocalDate, isToday: Boolean, isOutsideMonth: Boolean): Color {
    if (isToday) return Color.White
    val color =
        when (date.dayOfWeek) {
            DayOfWeek.SUNDAY -> CalendarSundayColor
            DayOfWeek.SATURDAY -> CalendarSaturdayColor
            else -> MaterialTheme.colorScheme.onSurface
        }
    return if (isOutsideMonth) color.copy(alpha = 0.48f) else color
}

@Composable
private fun DailyAmounts(transactions: List<TransactionData>) {
    val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf(TransactionData::amount)
    val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf(TransactionData::amount)
    val languageTag = LocalConfiguration.current.locales[0].toLanguageTag()
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        if (income > 0) AmountLabel(formatCalendarAmount(income, languageTag), CalendarIncomeColor)
        if (expense > 0) AmountLabel(formatCalendarAmount(-expense, languageTag), CalendarExpenseColor)
        if (income > 0 && expense > 0) {
            AmountLabel(formatCalendarAmount(income - expense, languageTag), MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun AmountLabel(value: String, color: Color) {
    Text(
        text = value,
        modifier = Modifier.fillMaxWidth(),
        color = color,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        maxLines = 1,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun MonthSelector(month: YearMonth, onMonthChange: (YearMonth) -> Unit, onTitleClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,

    ) {
        IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
            Icon(Icons.Outlined.ChevronLeft, stringResource(R.string.budget_book_previous_month))
        }
        Text(
            text = monthTitle(month),
            modifier =
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .then(if (onTitleClick == null) Modifier else Modifier.clickable(onClick = onTitleClick))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
            Icon(Icons.Outlined.ChevronRight, stringResource(R.string.budget_book_next_month))
        }
    }
}

@Composable
private fun MonthSummary(transactions: List<TransactionData>) {
    val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf(TransactionData::amount)
    val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf(TransactionData::amount)
    val borderColor = calendarBorderColor()
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 0.5.dp, color = borderColor)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryAmount(
                label = stringResource(R.string.budget_book_income),
                amount = income,
                color = CalendarIncomeColor,
                modifier = Modifier.weight(1f),
            )
            SummaryAmount(
                label = stringResource(R.string.budget_book_expense),
                amount = expense,
                color = CalendarExpenseColor,
                modifier = Modifier.weight(1f),
            )
            SummaryAmount(
                label = stringResource(R.string.budget_book_total),
                amount = income - expense,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                signed = true,
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = borderColor)
    }
}

@Composable
private fun SummaryAmount(label: String, amount: Int, color: Color, modifier: Modifier, signed: Boolean = false) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(
            text =
                stringResource(
                    R.string.budget_book_currency_amount,
                    if (signed) amount.withSignedComma() else amount.withComma(),
                ),
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthPickerDialog(selectedMonth: YearMonth, onSelect: (YearMonth) -> Unit, onDismiss: () -> Unit) {
    var selectedYear by rememberSaveable(selectedMonth) { mutableIntStateOf(selectedMonth.year) }
    val unselectedColor = calendarMutedBackground()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.budget_book_select_month)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { selectedYear -= 1 }) {
                        Icon(Icons.Outlined.ChevronLeft, stringResource(R.string.budget_book_previous_year))
                    }
                    Text(selectedYear.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { selectedYear += 1 }) {
                        Icon(Icons.Outlined.ChevronRight, stringResource(R.string.budget_book_next_year))
                    }
                }
                repeat(4) { rowIndex ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { columnIndex ->
                            val monthNumber = rowIndex * 3 + columnIndex + 1
                            val isSelected =
                                selectedYear == selectedMonth.year && monthNumber == selectedMonth.monthValue
                            Surface(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .clickable { onSelect(YearMonth.of(selectedYear, monthNumber)) },
                                shape = RoundedCornerShape(12.dp),
                                color =
                                    if (isSelected) {
                                        SmartBudgetGreen
                                    } else {
                                        unselectedColor
                                    },
                            ) {
                                Text(
                                    text = stringResource(R.string.month_short, monthNumber),
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.budget_book_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyTransactionsSheet(
    date: LocalDate,
    transactions: List<TransactionData>,
    onAdd: () -> Unit,
    onEdit: (TransactionData) -> Unit,
    onDelete: (TransactionData) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            DailyTransactionsHeader(date = date, transactionCount = transactions.size, onAdd = onAdd)
            Spacer(Modifier.height(12.dp))
            if (transactions.isEmpty()) {
                Text(
                    text = stringResource(R.string.budget_book_no_transactions),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                    itemsIndexed(transactions, key = { _, transaction -> transaction.id }) { index, transaction ->
                        SwipeableTransactionRow(transaction = transaction, onEdit = onEdit, onDelete = onDelete)
                        if (index < transactions.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTransactionsHeader(date: LocalDate, transactionCount: Int, onAdd: () -> Unit) {
    val badgeColor = calendarMutedBackground()
    val actionHeight = 30.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.full_date, date.year, date.monthValue, date.dayOfMonth),
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Surface(
            modifier = Modifier.height(actionHeight),
            shape = RoundedCornerShape(actionHeight / 2),
            color = badgeColor,
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.budget_book_transaction_count, transactionCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .size(actionHeight)
                    .clip(CircleShape)
                    .background(badgeColor, CircleShape)
                    .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.budget_book_add_transaction),
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun SwipeableTransactionRow(
    transaction: TransactionData,
    onEdit: (TransactionData) -> Unit,
    onDelete: (TransactionData) -> Unit,
) {
    val revealWidth = with(LocalDensity.current) { 96.dp.toPx() }
    var offset by remember(transaction.id) { mutableFloatStateOf(0f) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransactionActionButton(
                icon = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.budget_book_edit_transaction),
                color = SmartBudgetGreen,
            ) {
                offset = 0f
                onEdit(transaction)
            }
            TransactionActionButton(
                icon = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.budget_book_delete_transaction),
                color = CalendarIncomeColor,
            ) {
                offset = 0f
                onDelete(transaction)
            }
        }
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offset.roundToInt(), 0) }
                    .pointerInput(transaction.id) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                offset = (offset + dragAmount).coerceIn(-revealWidth, 0f)
                            },
                            onDragEnd = {
                                offset = if (offset <= -revealWidth / 2) -revealWidth else 0f
                            },
                            onDragCancel = { offset = 0f },
                        )
                    },
            color = MaterialTheme.colorScheme.surface,
        ) {
            DailyTransactionRow(transaction)
        }
    }
}

@Composable
private fun TransactionActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.14f))
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, modifier = Modifier.size(18.dp), tint = color)
    }
}

@Composable
private fun DailyTransactionRow(transaction: TransactionData) {
    val isIncome = transaction.type == TransactionType.INCOME
    val category = transactionCategoryUi(transaction.category)
    val categoryName = stringResource(category.label)
    val sourceOrPlace =
        transaction.storeName?.takeIf(String::isNotBlank)
            ?: stringResource(
                if (isIncome) {
                    R.string.budget_book_unknown_income_source
                } else {
                    R.string.budget_book_unknown_expense_place
                },
            )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .background(
                        category.color.copy(alpha = 0.14f),
                        RoundedCornerShape(12.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = categoryName,
                modifier = Modifier.size(21.dp),
                tint = category.color,
            )
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp).weight(1f)) {
            Text(sourceOrPlace, fontWeight = FontWeight.SemiBold)
            Text(
                text = categoryName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text =
                stringResource(
                    R.string.budget_book_currency_amount,
                    (if (isIncome) "+" else "-") + transaction.amount.withComma(),
                ),
            modifier = Modifier.widthIn(max = 140.dp),
            color = if (isIncome) CalendarIncomeColor else CalendarExpenseColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun PreviousMonthNavigation(
    month: YearMonth,
    latestMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Outlined.ChevronLeft,
                contentDescription = stringResource(R.string.budget_book_previous_month),
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text =
                if (month.year == YearMonth.now().year) {
                    stringResource(R.string.month_short, month.monthValue)
                } else {
                    stringResource(R.string.year_month, month.year, month.monthValue)
                },
            modifier = Modifier.widthIn(min = 48.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(36.dp),
            enabled = isNextStatisticsMonthAvailable(month, latestMonth),
        ) {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = stringResource(R.string.budget_book_next_month),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun MonthlyStatisticsCardContent(stats: MonthlyLedgerStats, budgetAmount: Int?, onClick: () -> Unit) {
    val languageTag = LocalConfiguration.current.locales[0].toLanguageTag()
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CashFlowAmount(
                label = stringResource(R.string.budget_book_income),
                amount = stats.amounts.income,
                color = ExpenseRed,
                modifier = Modifier.weight(1f),
            )
            CashFlowAmount(
                label = stringResource(R.string.budget_book_expense),
                amount = stats.amounts.expense,
                color = IncomeBlue,
                modifier = Modifier.weight(1f),
            )
            CashFlowAmount(
                label = stringResource(R.string.budget_book_total),
                amount = stats.amounts.income - stats.amounts.expense,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        PreviousMonthBudgetStatus(budgetAmount = budgetAmount, usedAmount = stats.amounts.expense)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        MonthlyMetrics(stats)
        if (stats.largestExpenseCategories.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            CategoryStatisticsTags(
                title = stringResource(R.string.budget_book_largest_expense_categories),
                tags =
                    stats.largestExpenseCategories.map { stat ->
                        CategoryStatTag(
                            category = stat.category,
                            value =
                                stringResource(
                                    R.string.budget_book_currency_amount,
                                    formatCalendarAmount(stat.amount, languageTag).removePrefix("+"),
                                ),
                        )
                    },
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        Button(
            onClick = onClick,
            modifier = Modifier.align(Alignment.End).height(36.dp),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                ),
        ) {
            Text(
                stringResource(R.string.budget_book_view_ledger),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun PreviousMonthBudgetStatus(budgetAmount: Int?, usedAmount: Int) {
    val progress = budgetUsageProgress(budgetAmount, usedAmount)
    val progressColor = progress?.let(::budgetUsageColor) ?: MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column{
                Text(
                    text =
                        budgetAmount?.let {
                            stringResource(R.string.budget_book_budget_amount, it.withComma())
                        } ?: stringResource(R.string.budget_book_budget_not_set),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.budget_book_used_amount, usedAmount.withComma()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                )
            }
            BudgetUsageIndicator(
                progress = progress ?: 0f,
                progressColor = progressColor,
                label =
                    if (progress == null) {
                        stringResource(R.string.budget_book_budget_usage_unavailable)
                    } else {
                        null
                    },
            )
        }
    }
}

internal fun isNextStatisticsMonthAvailable(month: YearMonth, latestMonth: YearMonth): Boolean = month < latestMonth

internal fun budgetUsageProgress(budgetAmount: Int?, usedAmount: Int): Float? =
    budgetAmount?.takeIf { it > 0 }?.let { usedAmount.toFloat() / it }

private data class CategoryStatTag(val category: String, val value: String)

@Composable
private fun MonthlyMetrics(stats: MonthlyLedgerStats) {
    val ratio = stats.expenseRatioPercent
    val ratioProgressColor =
        when {
            ratio == null || ratio < 100 -> SmartBudgetGreen
            ratio < 200 -> CalendarWarningColor
            else -> CalendarIncomeColor
        }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        MonthlyMetric(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.budget_book_income_expense_ratio),
            value = ratio?.let { "$it%" } ?: stringResource(R.string.budget_book_no_income_ratio),
            valueColor = MaterialTheme.colorScheme.onSurface,
            progress = ratio?.div(100f)?.coerceIn(0f, 1f),
            progressColor = ratioProgressColor,
            valueTextAlign = TextAlign.End,
            alignValueToBottom = true,
        )
        MonthlyMetric(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.budget_book_expense_change),
            value = stringResource(R.string.budget_book_currency_amount, stats.expenseChange.withSignedComma()),
            valueColor = when {
                stats.expenseChange > 0 -> CalendarExpenseColor
                stats.expenseChange < 0 -> CalendarIncomeColor
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            valueTextAlign = TextAlign.Start,
            alignValueToBottom = true,
        )
    }
}

@Composable
private fun MonthlyMetric(
    modifier: Modifier,
    label: String,
    value: String,
    valueColor: Color,
    progress: Float? = null,
    progressColor: Color = SmartBudgetGreen,
    valueTextAlign: TextAlign = TextAlign.Start,
    alignValueToBottom: Boolean = false,
) {
    Column(
        modifier = modifier.height(44.dp),
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 11.sp,
        )
        Spacer(Modifier.height(2.dp))
        if (alignValueToBottom) Spacer(Modifier.weight(1f))
        Text(
            value,
            modifier = Modifier.fillMaxWidth(),
            color = valueColor,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = valueTextAlign,
        )
        progress?.let {
            Spacer(Modifier.height(5.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(it)
                            .fillMaxHeight()
                            .background(progressColor, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun CategoryStatisticsTags(title: String, tags: List<CategoryStatTag>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tags.forEach { tag ->
                val category = transactionCategoryUi(tag.category)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = category.color.copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 11.dp, top = 7.dp, end = 8.dp, bottom = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(category.icon, null, modifier = Modifier.size(15.dp), tint = category.color)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(category.label),
                                color = category.color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                tag.value,
                                fontSize = 10.sp,
                                lineHeight = 10.sp,
                                modifier = Modifier.alpha(0.8f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding), content = content)
    }
}

internal fun calendarDays(month: YearMonth): List<LocalDate> {
    val firstDay = month.atDay(1)
    val leadingDays = firstDay.dayOfWeek.value % DayOfWeek.SUNDAY.value
    val calendarStart = firstDay.minusDays(leadingDays.toLong())
    val occupiedCells = leadingDays + month.lengthOfMonth()
    val cellCount = ((occupiedCells + 6) / 7) * 7
    return List(cellCount) { offset -> calendarStart.plusDays(offset.toLong()) }
}

@Composable
private fun monthTitle(month: YearMonth): String = stringResource(R.string.year_month, month.year, month.monthValue)

private fun Int.withComma(): String = String.format("%,d", this)

private fun Int.withSignedComma(): String = if (this > 0) "+${withComma()}" else withComma()

internal fun formatCalendarAmount(amount: Int, languageTag: String): String {
    val absoluteAmount = kotlin.math.abs(amount.toLong())
    val sign = when {
        amount > 0 -> "+"
        amount < 0 -> "-"
        else -> ""
    }
    val value =
        if (languageTag.startsWith("ko")) {
            when {
                absoluteAmount >= 100_000_000 -> "${absoluteAmount / 100_000_000}억"
                absoluteAmount >= 1_000_000 -> "${absoluteAmount / 1_000_000}백만"
                absoluteAmount >= 10_000 -> "${absoluteAmount / 10_000}만"
                absoluteAmount >= 1_000 -> "${absoluteAmount / 1_000}천"
                else -> absoluteAmount.toString()
            }
        } else {
            when {
                absoluteAmount >= 1_000_000 -> "${absoluteAmount / 1_000_000}M"
                absoluteAmount >= 1_000 -> "${absoluteAmount / 1_000}K"
                else -> absoluteAmount.toString()
            }
        }
    return "$sign$value"
}

@Composable
private fun calendarMutedBackground(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFFF3F3F3) else Color(0xFF252525)

@Composable
private fun overviewCalendarBackground(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFFF7F7F7) else Color(0xFF252525)

@Composable
private fun calendarBorderColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFFECECEC) else Color(0xFF343434)
