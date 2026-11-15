package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.HabitRepository
import com.example.data.local.ReminderPreferences
import com.example.reminders.HabitNotificationHelper
import com.example.reminders.ReminderScheduler

class DailyHabitApp : Application() {
    lateinit var repository: HabitRepository
        private set

    lateinit var reminderScheduler: ReminderScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        HabitNotificationHelper.createChannels(this)
        val database = AppDatabase.getInstance(this)
        val reminderPreferences = ReminderPreferences(this)
        repository = HabitRepository(database.habitDao(), reminderPreferences)
        reminderScheduler = ReminderScheduler(this)
    }
}
