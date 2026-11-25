package com.yu_morph.smartbudget.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yu_morph.smartbudget.MainActivity
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.local.PreferenceStoreFactory
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.Locale

class AnalysisReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        syncAnalysisReminder(context)
    }
}

private const val CHANNEL_ID = "monthly_analysis_reminder"
private const val NOTIFICATION_ID = 8_002
private const val REMINDER_HOUR = 19
private const val LAST_SHOWN_MONTH = "last_shown_month"

internal fun analysisReminderTime(now: ZonedDateTime): ZonedDateTime {
    val thisMonth = reminderTimeFor(YearMonth.from(now), now)
    return if (now.isBefore(thisMonth)) thisMonth else reminderTimeFor(YearMonth.from(now).plusMonths(1), now)
}

internal fun isAnalysisReminderDue(now: ZonedDateTime): Boolean =
    now.toLocalDate() == YearMonth.from(now).atEndOfMonth() && now.hour >= REMINDER_HOUR

private fun reminderTimeFor(month: YearMonth, now: ZonedDateTime): ZonedDateTime =
    month.atEndOfMonth().atTime(REMINDER_HOUR, 0).atZone(now.zone)

internal fun syncAnalysisReminder(context: Context) {
    val settings = PreferenceStoreFactory.appSettings(context).read()
    val manager = context.getSystemService(AlarmManager::class.java)
    val alarm = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, AnalysisReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )
    val signedIn = !PreferenceStoreFactory.userIdentity(context).readUsername().isNullOrBlank()
    if (!settings.analysisNotification || !signedIn) {
        manager.cancel(alarm)
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        return
    }

    val now = ZonedDateTime.now()
    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, analysisReminderTime(now).toInstant().toEpochMilli(), alarm)
    if (isAnalysisReminderDue(now)) showAnalysisReminder(context, settings.languageTag, YearMonth.from(now))
}

private fun showAnalysisReminder(context: Context, languageTag: String, month: YearMonth) {
    val notifications = NotificationManagerCompat.from(context)
    val history = context.getSharedPreferences(CHANNEL_ID, Context.MODE_PRIVATE)
    val alreadyShown = history.getString(LAST_SHOWN_MONTH, null) == month.toString()
    if (alreadyShown || !hasNotificationPermission(context) || !notifications.areNotificationsEnabled()) return

    val configuration = Configuration(context.resources.configuration).apply {
        setLocale(Locale.forLanguageTag(languageTag))
    }
    val localized = context.createConfigurationContext(configuration)
    val title = localized.getString(R.string.analysis_notification_title)
    val systemNotifications = context.getSystemService(NotificationManager::class.java)
    systemNotifications.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, title, NotificationManager.IMPORTANCE_DEFAULT),
    )
    val channel = systemNotifications.getNotificationChannel(CHANNEL_ID)
    if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return

    val openApp = PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_budget_notification)
        .setContentTitle(title)
        .setContentText(localized.getString(R.string.analysis_notification_message))
        .setContentIntent(openApp)
        .setAutoCancel(true)
        .build()
    if (runCatching { notifications.notify(NOTIFICATION_ID, notification) }.isSuccess) {
        history.edit().putString(LAST_SHOWN_MONTH, month.toString()).apply()
    }
}
