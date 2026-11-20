package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BirthdayReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        BirthdayNotificationHelper.checkAndShowUpcomingBirthdays(context.applicationContext)
    }
}
