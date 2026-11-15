package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRepository
import com.example.domain.StreakCalculator
import com.example.reminders.ReminderScheduler
import com.example.ui.model.DailyHabitUiState
import com.example.ui.model.HabitWithStats
import com.example.util.DateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(
    private val repository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val todayEpochDayFlow = MutableStateFlow(DateUtil.todayEpochDay())
    private val selectedEpochDayFlow = MutableStateFlow(DateUtil.todayEpochDay())
    private val selectedCategoryFlow = MutableStateFlow("All")

    init {
        viewModelScope.launch {
            repository.seedInitialHabitsIfNeeded()
        }
    }

    val uiState: StateFlow<DailyHabitUiState> = combine(
        repository.activeHabits,
        repository.archivedHabits,
        repository.allCompletions,
        repository.dailySummarySettings,
        combine(todayEpochDayFlow, selectedEpochDayFlow, selectedCategoryFlow) { today, selected, cat ->
            Triple(today, selected, cat)
        }
    ) { activeHabits, archivedHabits, completions, summarySettings, dateAndCat ->
        val (today, selectedDay, selectedCategory) = dateAndCat
        val completionsByHabit = completions
            .groupBy({ it.habitId }, { it.epochDay })
            .mapValues { it.value.toSet() }

        val habitItems = activeHabits.map { habit ->
            val completedDays = completionsByHabit[habit.id].orEmpty()
            HabitWithStats(
                habit = habit,
                isCompletedOnSelectedDay = selectedDay in completedDays,
                isScheduledOnSelectedDay = DateUtil.isScheduledForDay(habit.scheduledDaysMask, selectedDay),
                stats = StreakCalculator.calculateHabitStats(habit, completedDays, today),
                completedEpochDays = completedDays
            )
        }

        val weekSummaries = StreakCalculator.calculateDailySummaries(
            habits = activeHabits,
            completions = completions,
            startEpochDay = today - 6L,
            endEpochDay = today
        )

        val last35DaysSummaries = StreakCalculator.calculateDailySummaries(
            habits = activeHabits,
            completions = completions,
            startEpochDay = today - 34L,
            endEpochDay = today
        )

        val overallStats = StreakCalculator.calculateOverallStats(
            habits = activeHabits,
            completionsByHabit = completionsByHabit,
            allCompletions = completions,
            todayEpochDay = today
        )

        DailyHabitUiState(
            isLoading = false,
            todayEpochDay = today,
            selectedEpochDay = selectedDay,
            selectedCategory = selectedCategory,
            activeHabits = habitItems,
            archivedHabits = archivedHabits,
            weekSummaries = weekSummaries,
            last35DaysSummaries = last35DaysSummaries,
            overallStats = overallStats,
            dailySummarySettings = summarySettings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyHabitUiState()
    )

    fun refreshTodayIfNeeded() {
        val currentToday = DateUtil.todayEpochDay()
        if (todayEpochDayFlow.value != currentToday) {
            val previousToday = todayEpochDayFlow.value
            todayEpochDayFlow.value = currentToday
            if (selectedEpochDayFlow.value == previousToday) {
                selectedEpochDayFlow.value = currentToday
            }
        }
    }

    fun selectDay(epochDay: Long) {
        selectedEpochDayFlow.value = epochDay
    }

    fun selectCategory(category: String) {
        selectedCategoryFlow.value = category
    }

    fun toggleHabitCompletion(habitId: Long, epochDay: Long) {
        if (epochDay > DateUtil.todayEpochDay()) return
        viewModelScope.launch {
            repository.toggleCompletion(habitId, epochDay)
        }
    }

    fun saveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val saved = repository.saveHabit(habit)
            reminderScheduler.scheduleHabitReminder(saved)
        }
    }

    fun toggleHabitReminder(habit: HabitEntity, enabled: Boolean) {
        viewModelScope.launch {
            val updated = habit.copy(reminderEnabled = enabled)
            val saved = repository.saveHabit(updated)
            reminderScheduler.scheduleHabitReminder(saved)
        }
    }

    fun updateHabitReminderTime(habit: HabitEntity, hour: Int, minute: Int) {
        viewModelScope.launch {
            val updated = habit.copy(
                reminderEnabled = true,
                reminderHour = hour,
                reminderMinute = minute
            )
            val saved = repository.saveHabit(updated)
            reminderScheduler.scheduleHabitReminder(saved)
        }
    }

    fun archiveHabit(habitId: Long, archived: Boolean) {
        viewModelScope.launch {
            repository.archiveHabit(habitId, archived)
            if (archived) {
                reminderScheduler.cancelHabitReminder(habitId)
            } else {
                repository.getHabitByIdOnce(habitId)?.let { reminderScheduler.scheduleHabitReminder(it) }
            }
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            reminderScheduler.cancelHabitReminder(habitId)
            repository.deleteHabit(habitId)
        }
    }

    fun addStarterHabits() {
        viewModelScope.launch {
            repository.addStarterHabits()
        }
    }

    fun updateDailySummaryReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.updateDailySummary(enabled, hour, minute)
            reminderScheduler.scheduleDailySummary(enabled, hour, minute)
        }
    }

    class Factory(
        private val repository: HabitRepository,
        private val reminderScheduler: ReminderScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HabitViewModel(repository, reminderScheduler) as T
        }
    }
}
