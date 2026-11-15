package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "daily_habit_prefs")

data class DailySummaryReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0
)

class ReminderPreferences(private val context: Context) {
    private object Keys {
        val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")
        val DAILY_SUMMARY_HOUR = intPreferencesKey("daily_summary_hour")
        val DAILY_SUMMARY_MINUTE = intPreferencesKey("daily_summary_minute")
        val HAS_SEEDED_INITIAL_HABITS = booleanPreferencesKey("has_seeded_initial_habits")
    }

    val dailySummarySettings: Flow<DailySummaryReminderSettings> = context.reminderDataStore.data.map { prefs ->
        DailySummaryReminderSettings(
            enabled = prefs[Keys.DAILY_SUMMARY_ENABLED] ?: false,
            hour = prefs[Keys.DAILY_SUMMARY_HOUR] ?: 20,
            minute = prefs[Keys.DAILY_SUMMARY_MINUTE] ?: 0
        )
    }

    suspend fun getDailySummarySettingsOnce(): DailySummaryReminderSettings = dailySummarySettings.first()

    suspend fun updateDailySummary(enabled: Boolean, hour: Int, minute: Int) {
        context.reminderDataStore.edit { prefs ->
            prefs[Keys.DAILY_SUMMARY_ENABLED] = enabled
            prefs[Keys.DAILY_SUMMARY_HOUR] = hour
            prefs[Keys.DAILY_SUMMARY_MINUTE] = minute
        }
    }

    suspend fun hasSeededInitialHabitsOnce(): Boolean {
        return context.reminderDataStore.data.first()[Keys.HAS_SEEDED_INITIAL_HABITS] ?: false
    }

    suspend fun markInitialHabitsSeeded() {
        context.reminderDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEDED_INITIAL_HABITS] = true
        }
    }
}
