package com.example.notifications

import android.Manifest
import android.app.AlarmManager
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
import com.example.MainActivity
import com.example.R
import com.example.model.FamilyMember
import com.example.util.BirthdayUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

object BirthdayNotificationHelper {

    const val CHANNEL_ID = "birthday_reminders_channel"
    private const val PREFS_NAME = "amader_poribar_birthday_prefs"
    private const val KEY_MEMBERS_JSON = "cached_members_json"
    private const val KEY_NOTIFY_ENABLED = "notify_enabled"
    private const val KEY_DAYS_BEFORE = "notify_days_before"
    private const val KEY_NOTIFY_HOUR = "notify_hour"
    private const val ALARM_REQUEST_CODE = 4201

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun syncBirthdaysAndSchedule(
        context: Context,
        members: List<FamilyMember>,
        notifyEnabled: Boolean,
        daysBefore: Int,
        notifyHour: Int
    ) {
        val array = JSONArray()
        members.filter { it.birthdayReminder && BirthdayUtils.hasBirthday(it.birthDay, it.birthMonth) }
            .forEach { member ->
                array.put(
                    JSONObject().apply {
                        put("id", member.id)
                        put("fullName", member.fullName)
                        put("relation", member.relation)
                        put("birthDay", member.birthDay)
                        put("birthMonth", member.birthMonth)
                        put("birthYear", member.birthYear)
                    }
                )
            }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MEMBERS_JSON, array.toString())
            .putBoolean(KEY_NOTIFY_ENABLED, notifyEnabled)
            .putInt(KEY_DAYS_BEFORE, daysBefore.coerceIn(0, 7))
            .putInt(KEY_NOTIFY_HOUR, notifyHour.coerceIn(0, 23))
            .apply()

        if (notifyEnabled) {
            scheduleDailyCheck(context, notifyHour)
        } else {
            cancelDailyCheck(context)
        }
    }

    fun scheduleDailyCheck(context: Context, notifyHourOverride: Int? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_NOTIFY_ENABLED, true)) return
        val hour = (notifyHourOverride ?: prefs.getInt(KEY_NOTIFY_HOUR, 9)).coerceIn(0, 23)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BirthdayReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerCalendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelDailyCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BirthdayReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun checkAndShowUpcomingBirthdays(context: Context): Int {
        if (!hasNotificationPermission(context)) return 0
        ensureNotificationChannel(context)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_NOTIFY_ENABLED, true)) return 0

        val daysBefore = prefs.getInt(KEY_DAYS_BEFORE, 1).coerceIn(0, 7)
        val rawJson = prefs.getString(KEY_MEMBERS_JSON, "[]") ?: "[]"
        val array = try {
            JSONArray(rawJson)
        } catch (_: Exception) {
            return 0
        }

        var sentCount = 0
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = obj.optString("fullName")
            val relation = obj.optString("relation")
            val day = obj.optLong("birthDay")
            val month = obj.optLong("birthMonth")
            val year = obj.optLong("birthYear")
            val daysUntil = BirthdayUtils.daysUntilBirthday(day, month) ?: continue

            if (daysUntil == 0 || daysUntil <= daysBefore) {
                val title = if (daysUntil == 0) {
                    "আজ $name-এর জন্মদিন!"
                } else {
                    "আসন্ন জন্মদিনের রিমাইন্ডার"
                }
                val ageText = BirthdayUtils.upcomingAge(day, month, year)?.let {
                    " (${BirthdayUtils.toBengaliDigits(it)}তম জন্মদিন)"
                }.orEmpty()
                val body = when (daysUntil) {
                    0 -> "$name ($relation)-এর আজ শুভ জন্মদিন$ageText! শুভেচ্ছা পাঠাতে ট্যাপ করুন।"
                    1 -> "আগামীকাল $name ($relation)-এর জন্মদিন$ageText!"
                    else -> "আর ${BirthdayUtils.toBengaliDigits(daysUntil)} দিন পর $name ($relation)-এর জন্মদিন$ageText।"
                }
                showNotification(
                    context = context,
                    notificationId = 5000 + i,
                    title = title,
                    body = body
                )
                sentCount++
            }
        }
        return sentCount
    }

    fun sendTestNotification(context: Context, nextMember: FamilyMember?): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureNotificationChannel(context)

        val title: String
        val body: String
        if (nextMember != null && BirthdayUtils.hasBirthday(nextMember.birthDay, nextMember.birthMonth)) {
            val days = BirthdayUtils.daysUntilBirthday(nextMember.birthDay, nextMember.birthMonth) ?: 0
            title = "জন্মদিনের রিমাইন্ডার চালু আছে"
            body = if (days == 0) {
                "আজ ${nextMember.fullName} (${nextMember.relation})-এর শুভ জন্মদিন!"
            } else {
                "${nextMember.fullName} (${nextMember.relation})-এর জন্মদিনে আর ${BirthdayUtils.toBengaliDigits(days)} দিন বাকি।"
            }
        } else {
            title = "আমাদের পরিবার: জন্মদিনের নোটিফিকেশন"
            body = "পরিবারের কারও জন্মদিন আসলেই আপনি সময়মতো এমন নোটিফিকেশন পাবেন!"
        }

        showNotification(
            context = context,
            notificationId = 4999,
            title = title,
            body = body
        )
        return true
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission(context)) return
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }
}
