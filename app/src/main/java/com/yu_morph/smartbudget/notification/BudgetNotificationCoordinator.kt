@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.yu_morph.smartbudget.MainActivity
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.local.PreferenceStoreFactory
import com.yu_morph.smartbudget.data.remote.ApiResult
import com.yu_morph.smartbudget.data.remote.SmartBudgetApi
import com.yu_morph.smartbudget.data.remote.api.TransactionListQuery
import com.yu_morph.smartbudget.data.remote.model.TransactionType
import java.text.NumberFormat
import java.time.YearMonth
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val BUDGET_NOTIFICATION_CHANNEL_ID = "monthly_budget_alerts"
private const val BUDGET_APPROACHING_NOTIFICATION_ID = 8_001
private const val TRANSACTION_PAGE_LIMIT = 100

internal enum class BudgetAlertType { APPROACHING, EXCEEDED }

internal fun budgetAlertType(
    budgetAmount: Int?,
    expenseAmount: Long,
    exceededAlreadyNotified: Boolean,
): BudgetAlertType? {
    val budget = budgetAmount?.takeIf { it > 0 }?.toLong() ?: return null
    return when {
        expenseAmount > budget && !exceededAlreadyNotified -> BudgetAlertType.EXCEEDED
        expenseAmount > budget -> null
        expenseAmount * 100 > budget * 80 -> BudgetAlertType.APPROACHING
        else -> null
    }
}

internal fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

class BudgetNotificationCoordinator(context: Context, private val api: SmartBudgetApi) {
    private val applicationContext = context.applicationContext
    private val settingsStore = PreferenceStoreFactory.appSettings(applicationContext)
    private val userIdentityStore = PreferenceStoreFactory.userIdentity(applicationContext)
    private val notificationHistory =
        applicationContext.getSharedPreferences(NOTIFICATION_HISTORY_FILE, Context.MODE_PRIVATE)
    private val evaluationMutex = Mutex()

    suspend fun evaluateAfterLedgerUpdate() = evaluationMutex.withLock {
        if (!hasNotificationPermission(applicationContext)) return@withLock
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (!notificationManager.areNotificationsEnabled()) return@withLock

        val month = YearMonth.now()
        val budgetAmount = loadBudgetAmount(month) ?: return@withLock
        val expenseAmount = loadExpenseAmount(month) ?: return@withLock
        val exceededKey = exceededHistoryKey(month)
        val alertType =
            budgetAlertType(
                budgetAmount = budgetAmount,
                expenseAmount = expenseAmount,
                exceededAlreadyNotified = notificationHistory.getBoolean(exceededKey, false),
            ) ?: return@withLock
        val settings = settingsStore.read()
        val enabled =
            when (alertType) {
                BudgetAlertType.APPROACHING -> settings.budgetApproachingNotification
                BudgetAlertType.EXCEEDED -> settings.budgetExceededNotification
            }
        if (!enabled) return@withLock

        val notificationPosted =
            runCatching {
                createNotificationChannel()
                showNotification(notificationManager, alertType, month, budgetAmount, expenseAmount)
            }.isSuccess
        if (!notificationPosted) return@withLock
        if (alertType == BudgetAlertType.EXCEEDED) {
            notificationHistory.edit().putBoolean(exceededKey, true).commit()
        }
    }

    private suspend fun loadBudgetAmount(month: YearMonth): Int? =
        (api.monthlyBudgets.get(month.toString()) as? ApiResult.Success)?.data?.amount

    private suspend fun loadExpenseAmount(month: YearMonth): Long? {
        var cursor: String? = null
        var expenseAmount = 0L
        do {
            val result =
                api.transactions.list(
                    TransactionListQuery(
                        startDate = month.atDay(1).toString(),
                        endDate = month.atEndOfMonth().toString(),
                        limit = TRANSACTION_PAGE_LIMIT,
                        cursor = cursor,
                    ),
                )
            val page = (result as? ApiResult.Success)?.data ?: return null
            expenseAmount += page.items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.toLong() }
            cursor = page.nextCursor
        } while (cursor != null)
        return expenseAmount
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                BUDGET_NOTIFICATION_CHANNEL_ID,
                applicationContext.getString(R.string.budget_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = applicationContext.getString(R.string.budget_notification_channel_description)
            }
        applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun showNotification(
        manager: NotificationManagerCompat,
        type: BudgetAlertType,
        month: YearMonth,
        budgetAmount: Int,
        expenseAmount: Long,
    ) {
        val percent = ((expenseAmount * 100) / budgetAmount).toInt()
        val numberFormat = NumberFormat.getNumberInstance(applicationContext.resources.configuration.locales[0])
        val titleRes =
            if (type == BudgetAlertType.EXCEEDED) {
                R.string.budget_exceeded_notification_title
            } else {
                R.string.budget_approaching_notification_title
            }
        val message =
            if (type == BudgetAlertType.EXCEEDED) {
                applicationContext.getString(R.string.budget_exceeded_notification_message, percent)
            } else {
                applicationContext.getString(
                    R.string.budget_approaching_notification_message,
                    percent,
                    numberFormat.format(expenseAmount),
                    numberFormat.format(budgetAmount),
                )
            }
        val notification =
            NotificationCompat
                .Builder(applicationContext, BUDGET_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_budget_notification)
                .setContentTitle(applicationContext.getString(titleRes))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(mainActivityIntent())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        val notificationId =
            if (type == BudgetAlertType.EXCEEDED) {
                BUDGET_APPROACHING_NOTIFICATION_ID + month.hashCode()
            } else {
                BUDGET_APPROACHING_NOTIFICATION_ID
            }
        manager.notify(notificationId, notification)
    }

    private fun mainActivityIntent(): PendingIntent = PendingIntent.getActivity(
        applicationContext,
        0,
        Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun exceededHistoryKey(month: YearMonth): String {
        val username = userIdentityStore.readUsername().orEmpty().ifBlank { "unknown" }
        return "budget_exceeded:$username:$month"
    }

    private companion object {
        const val NOTIFICATION_HISTORY_FILE = "smartbudget_budget_notification_history"
    }
}
