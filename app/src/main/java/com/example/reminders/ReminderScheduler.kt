package com.example.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.local.HabitEntity
import com.example.util.DateUtil
import java.util.Calendar

class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleHabitReminder(habit: HabitEntity) {
        if (habit.id == 0L) return
        if (!habit.reminderEnabled || habit.isArchived || habit.scheduledDaysMask == 0) {
            cancelHabitReminder(habit.id)
            return
        }
        val triggerMillis = computeNextTriggerMillis(
            hour = habit.reminderHour,
            minute = habit.reminderMinute,
            scheduledDaysMask = habit.scheduledDaysMask
        )
        val pendingIntent = buildHabitPendingIntent(habit.id, habit.title)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    fun cancelHabitReminder(habitId: Long) {
        val pendingIntent = buildHabitPendingIntent(habitId, "")
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun scheduleDailySummary(enabled: Boolean, hour: Int, minute: Int) {
        if (!enabled) {
            cancelDailySummary()
            return
        }
        val triggerMillis = computeNextTriggerMillis(
            hour = hour,
            minute = minute,
            scheduledDaysMask = DateUtil.ALL_DAYS_MASK
        )
        val pendingIntent = buildDailySummaryPendingIntent()
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    fun cancelDailySummary() {
        val pendingIntent = buildDailySummaryPendingIntent()
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildHabitPendingIntent(habitId: Long, habitTitle: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_HABIT_REMINDER
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderReceiver.EXTRA_HABIT_TITLE, habitTitle)
        }
        val requestCode = (habitId.toInt() and 0x0FFFFFFF) + 1000
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildDailySummaryPendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DAILY_SUMMARY
        }
        return PendingIntent.getBroadcast(
            context,
            SUMMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun computeNextTriggerMillis(
        hour: Int,
        minute: Int,
        scheduledDaysMask: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val candidate = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (candidate.timeInMillis <= nowMillis) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }
        var guard = 0
        while (guard < 8) {
            val epochDay = DateUtil.civilToEpochDay(
                candidate.get(Calendar.YEAR),
                candidate.get(Calendar.MONTH) + 1,
                candidate.get(Calendar.DAY_OF_MONTH)
            )
            if (DateUtil.isScheduledForDay(scheduledDaysMask, epochDay)) {
                break
            }
            candidate.add(Calendar.DAY_OF_YEAR, 1)
            guard++
        }
        return candidate.timeInMillis
    }

    companion object {
        private const val SUMMARY_REQUEST_CODE = 999001
    }
}
