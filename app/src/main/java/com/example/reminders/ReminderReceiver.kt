package com.example.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.ReminderPreferences
import com.example.util.DateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val scheduler = ReminderScheduler(appContext)
        val db = AppDatabase.getInstance(appContext)
        val dao = db.habitDao()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = DateUtil.todayEpochDay()
                when (intent.action) {
                    ACTION_HABIT_REMINDER -> {
                        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                        if (habitId > 0L) {
                            val habit = dao.getHabitByIdOnce(habitId)
                            if (habit != null && !habit.isArchived && habit.reminderEnabled) {
                                val alreadyDone = dao.isCompletedOnDayOnce(habitId, today)
                                if (!alreadyDone && DateUtil.isScheduledForDay(habit.scheduledDaysMask, today)) {
                                    val body = habit.description.ifBlank {
                                        "Tap to check off \"${habit.title}\" and keep your streak alive!"
                                    }
                                    HabitNotificationHelper.showHabitNotification(
                                        context = appContext,
                                        notificationId = (habitId.toInt() and 0x0FFFFFFF) + 2000,
                                        title = "Time for ${habit.title}",
                                        body = body,
                                        channelId = HabitNotificationHelper.CHANNEL_HABIT_REMINDERS
                                    )
                                }
                                scheduler.scheduleHabitReminder(habit)
                            }
                        }
                    }

                    ACTION_DAILY_SUMMARY -> {
                        val prefs = ReminderPreferences(appContext)
                        val settings = prefs.getDailySummarySettingsOnce()
                        if (settings.enabled) {
                            val activeHabits = dao.observeActiveHabits().first()
                            val scheduledToday = activeHabits.filter {
                                DateUtil.isScheduledForDay(it.scheduledDaysMask, today)
                            }
                            var remaining = 0
                            for (habit in scheduledToday) {
                                if (!dao.isCompletedOnDayOnce(habit.id, today)) {
                                    remaining++
                                }
                            }
                            if (remaining > 0) {
                                val message = if (remaining == 1) {
                                    "You have 1 habit left to check off today."
                                } else {
                                    "You have $remaining habits left to complete today."
                                }
                                HabitNotificationHelper.showHabitNotification(
                                    context = appContext,
                                    notificationId = 999002,
                                    title = "DailyHabit Check-in",
                                    body = message,
                                    channelId = HabitNotificationHelper.CHANNEL_DAILY_SUMMARY
                                )
                            }
                            scheduler.scheduleDailySummary(true, settings.hour, settings.minute)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_HABIT_REMINDER = "com.example.ACTION_HABIT_REMINDER"
        const val ACTION_DAILY_SUMMARY = "com.example.ACTION_DAILY_SUMMARY"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_TITLE = "extra_habit_title"
    }
}
