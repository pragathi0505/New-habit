package com.example.data.local

import com.example.util.DateUtil
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HabitRepository(
    private val habitDao: HabitDao,
    private val reminderPreferences: ReminderPreferences
) {
    val activeHabits: Flow<List<HabitEntity>> = habitDao.observeActiveHabits()
    val archivedHabits: Flow<List<HabitEntity>> = habitDao.observeArchivedHabits()
    val allCompletions: Flow<List<HabitCompletionEntity>> = habitDao.observeAllCompletions()
    val dailySummarySettings: Flow<DailySummaryReminderSettings> = reminderPreferences.dailySummarySettings

    suspend fun saveHabit(habit: HabitEntity): HabitEntity = withContext(NonCancellable) {
        if (habit.id == 0L) {
            val newId = habitDao.insertHabit(habit)
            habit.copy(id = newId)
        } else {
            habitDao.updateHabit(habit)
            habit
        }
    }

    suspend fun toggleCompletion(habitId: Long, epochDay: Long): Boolean = withContext(NonCancellable) {
        val alreadyDone = habitDao.isCompletedOnDayOnce(habitId, epochDay)
        if (alreadyDone) {
            habitDao.deleteCompletion(habitId, epochDay)
            false
        } else {
            habitDao.insertCompletion(HabitCompletionEntity(habitId = habitId, epochDay = epochDay))
            true
        }
    }

    suspend fun archiveHabit(habitId: Long, archived: Boolean) = withContext(NonCancellable) {
        habitDao.setArchived(habitId, archived)
    }

    suspend fun deleteHabit(habitId: Long) = withContext(NonCancellable) {
        habitDao.deleteHabitById(habitId)
    }

    suspend fun getHabitByIdOnce(habitId: Long): HabitEntity? = habitDao.getHabitByIdOnce(habitId)

    suspend fun getActiveReminderHabitsOnce(): List<HabitEntity> = habitDao.getActiveReminderHabitsOnce()

    suspend fun getDailySummarySettingsOnce(): DailySummaryReminderSettings =
        reminderPreferences.getDailySummarySettingsOnce()

    suspend fun updateDailySummary(enabled: Boolean, hour: Int, minute: Int) = withContext(NonCancellable) {
        reminderPreferences.updateDailySummary(enabled, hour, minute)
    }

    suspend fun seedInitialHabitsIfNeeded() = withContext(NonCancellable) {
        val alreadySeeded = reminderPreferences.hasSeededInitialHabitsOnce()
        if (!alreadySeeded) {
            if (habitDao.getTotalHabitCountOnce() == 0) {
                addStarterHabits()
            }
            reminderPreferences.markInitialHabitsSeeded()
        }
    }

    suspend fun addStarterHabits(): List<HabitEntity> {
        val today = DateUtil.todayEpochDay()
        val starters = listOf(
            HabitEntity(
                title = "Morning Hydration",
                description = "Drink 500ml of water right after waking",
                category = "Health",
                iconKey = "water",
                colorHex = 0xFF3B7A9EL,
                scheduledDaysMask = DateUtil.ALL_DAYS_MASK,
                reminderEnabled = false,
                reminderHour = 7,
                reminderMinute = 30,
                createdAtEpochDay = today
            ),
            HabitEntity(
                title = "15-Min Mindfulness",
                description = "Quiet breathing or guided meditation session",
                category = "Mindfulness",
                iconKey = "meditate",
                colorHex = 0xFF2D8A61L,
                scheduledDaysMask = DateUtil.ALL_DAYS_MASK,
                reminderEnabled = false,
                reminderHour = 8,
                reminderMinute = 0,
                createdAtEpochDay = today
            ),
            HabitEntity(
                title = "30-Min Walk or Exercise",
                description = "Outdoor movement, mobility, or workout",
                category = "Fitness",
                iconKey = "run",
                colorHex = 0xFFE06D44L,
                scheduledDaysMask = DateUtil.ALL_DAYS_MASK,
                reminderEnabled = false,
                reminderHour = 17,
                reminderMinute = 30,
                createdAtEpochDay = today
            ),
            HabitEntity(
                title = "Read 20 Pages",
                description = "Read a book before evening screen time",
                category = "Learning",
                iconKey = "book",
                colorHex = 0xFFD49A2AL,
                scheduledDaysMask = DateUtil.ALL_DAYS_MASK,
                reminderEnabled = false,
                reminderHour = 21,
                reminderMinute = 0,
                createdAtEpochDay = today
            ),
            HabitEntity(
                title = "Evening Gratitude Note",
                description = "Write down 3 highlights from the day",
                category = "Routine",
                iconKey = "journal",
                colorHex = 0xFF7B5EA7L,
                scheduledDaysMask = DateUtil.ALL_DAYS_MASK,
                reminderEnabled = false,
                reminderHour = 21,
                reminderMinute = 30,
                createdAtEpochDay = today
            )
        )
        val ids = habitDao.insertHabits(starters)
        reminderPreferences.markInitialHabitsSeeded()
        return starters.mapIndexed { idx, habit -> habit.copy(id = ids[idx]) }
    }
}
