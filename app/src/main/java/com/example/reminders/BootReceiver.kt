package com.example.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.ReminderPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = ReminderScheduler(appContext)
                val dao = AppDatabase.getInstance(appContext).habitDao()
                val reminderHabits = dao.getActiveReminderHabitsOnce()
                reminderHabits.forEach { scheduler.scheduleHabitReminder(it) }

                val summarySettings = ReminderPreferences(appContext).getDailySummarySettingsOnce()
                scheduler.scheduleDailySummary(
                    enabled = summarySettings.enabled,
                    hour = summarySettings.hour,
                    minute = summarySettings.minute
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
