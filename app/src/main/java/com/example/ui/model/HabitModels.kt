package com.example.ui.model

import com.example.data.local.DailySummaryReminderSettings
import com.example.data.local.HabitEntity
import com.example.domain.DayProgressSummary
import com.example.domain.HabitStreakStats
import com.example.domain.OverallProgressStats
import com.example.util.DateUtil

data class HabitWithStats(
    val habit: HabitEntity,
    val isCompletedOnSelectedDay: Boolean,
    val isScheduledOnSelectedDay: Boolean,
    val stats: HabitStreakStats,
    val completedEpochDays: Set<Long>
)

data class DailyHabitUiState(
    val isLoading: Boolean = true,
    val todayEpochDay: Long = DateUtil.todayEpochDay(),
    val selectedEpochDay: Long = DateUtil.todayEpochDay(),
    val selectedCategory: String = "All",
    val activeHabits: List<HabitWithStats> = emptyList(),
    val archivedHabits: List<HabitEntity> = emptyList(),
    val weekSummaries: List<DayProgressSummary> = emptyList(),
    val last35DaysSummaries: List<DayProgressSummary> = emptyList(),
    val overallStats: OverallProgressStats = OverallProgressStats(0, 0, 0f, 0, 0),
    val dailySummarySettings: DailySummaryReminderSettings = DailySummaryReminderSettings()
) {
    val scheduledForSelectedDay: List<HabitWithStats>
        get() = activeHabits.filter { item ->
            item.isScheduledOnSelectedDay &&
                (selectedCategory == "All" || item.habit.category == selectedCategory)
        }

    val allScheduledCountOnSelectedDay: Int
        get() = activeHabits.count { it.isScheduledOnSelectedDay }

    val allCompletedCountOnSelectedDay: Int
        get() = activeHabits.count { it.isScheduledOnSelectedDay && it.isCompletedOnSelectedDay }

    val categoriesPresent: List<String>
        get() = listOf("All") + activeHabits.map { it.habit.category }.distinct()
}

object HabitCategories {
    val all = listOf("Health", "Mindfulness", "Fitness", "Learning", "Focus", "Routine")
}
