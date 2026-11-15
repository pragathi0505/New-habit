package com.example.domain

import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.util.DateUtil

data class HabitStreakStats(
    val currentStreak: Int,
    val bestStreak: Int,
    val totalCompletions: Int,
    val completionRate30Days: Float
)

enum class CalendarDayStatus {
    COMPLETED,
    PARTIAL,
    MISSED,
    PENDING_TODAY,
    REST,
    FUTURE
}

data class DayProgressSummary(
    val epochDay: Long,
    val scheduledCount: Int,
    val completedCount: Int
) {
    val ratio: Float
        get() = if (scheduledCount <= 0) 0f else (completedCount.toFloat() / scheduledCount).coerceIn(0f, 1f)

    fun status(todayEpochDay: Long): CalendarDayStatus {
        return when {
            epochDay > todayEpochDay -> CalendarDayStatus.FUTURE
            scheduledCount == 0 -> CalendarDayStatus.REST
            completedCount >= scheduledCount -> CalendarDayStatus.COMPLETED
            completedCount > 0 -> CalendarDayStatus.PARTIAL
            epochDay == todayEpochDay -> CalendarDayStatus.PENDING_TODAY
            else -> CalendarDayStatus.MISSED
        }
    }
}

data class OverallProgressStats(
    val totalCheckIns: Int,
    val bestHabitStreak: Int,
    val weeklyCompletionRate: Float,
    val activeHabitCount: Int,
    val dailyConsistencyStreak: Int,
    val perfectDayStreak: Int = 0
)

object StreakCalculator {

    fun calculateHabitStats(
        habit: HabitEntity,
        completedDays: Set<Long>,
        todayEpochDay: Long
    ): HabitStreakStats {
        if (habit.scheduledDaysMask == 0) {
            return HabitStreakStats(0, 0, completedDays.size, 0f)
        }

        // Current streak: start from today if completed today, otherwise from the most recent scheduled day before today
        var cursor = todayEpochDay
        if (!DateUtil.isScheduledForDay(habit.scheduledDaysMask, cursor) || cursor !in completedDays) {
            cursor = todayEpochDay - 1
            var safety = 0
            while (safety < 14 && !DateUtil.isScheduledForDay(habit.scheduledDaysMask, cursor)) {
                cursor--
                safety++
            }
        }

        var currentStreak = 0
        val minBound = minOf(habit.createdAtEpochDay, completedDays.minOrNull() ?: todayEpochDay) - 30L
        while (cursor >= minBound) {
            if (DateUtil.isScheduledForDay(habit.scheduledDaysMask, cursor)) {
                if (cursor in completedDays) {
                    currentStreak++
                } else {
                    break
                }
            }
            cursor--
        }

        // Best streak across history from first relevant day up to today
        val earliestDay = listOfNotNull(habit.createdAtEpochDay, completedDays.minOrNull()).minOrNull() ?: todayEpochDay
        var bestStreak = 0
        var runningStreak = 0
        for (day in earliestDay..todayEpochDay) {
            if (DateUtil.isScheduledForDay(habit.scheduledDaysMask, day)) {
                if (day in completedDays) {
                    runningStreak++
                    if (runningStreak > bestStreak) bestStreak = runningStreak
                } else {
                    runningStreak = 0
                }
            }
        }

        // 30-day completion rate (from max(createdAt, today-29) .. today)
        val windowStart = maxOf(habit.createdAtEpochDay, todayEpochDay - 29L)
        var scheduledInWindow = 0
        var completedInWindow = 0
        for (day in windowStart..todayEpochDay) {
            if (DateUtil.isScheduledForDay(habit.scheduledDaysMask, day)) {
                scheduledInWindow++
                if (day in completedDays) completedInWindow++
            }
        }
        val rate = if (scheduledInWindow == 0) 0f else (completedInWindow.toFloat() / scheduledInWindow).coerceIn(0f, 1f)

        return HabitStreakStats(
            currentStreak = currentStreak,
            bestStreak = maxOf(bestStreak, currentStreak),
            totalCompletions = completedDays.size,
            completionRate30Days = rate
        )
    }

    fun calculateDailySummaries(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        startEpochDay: Long,
        endEpochDay: Long
    ): List<DayProgressSummary> {
        val activeIds = habits.map { it.id }.toSet()
        val completionsByDay = completions
            .filter { it.habitId in activeIds }
            .groupBy({ it.epochDay }, { it.habitId })
            .mapValues { it.value.toSet() }

        return (startEpochDay..endEpochDay).map { day ->
            val scheduledHabits = habits.filter { habit ->
                habit.createdAtEpochDay <= day && DateUtil.isScheduledForDay(habit.scheduledDaysMask, day)
            }
            val completedIds = completionsByDay[day].orEmpty()
            val completedCount = scheduledHabits.count { it.id in completedIds }
            DayProgressSummary(
                epochDay = day,
                scheduledCount = scheduledHabits.size,
                completedCount = completedCount
            )
        }
    }

    fun calculateOverallStats(
        habits: List<HabitEntity>,
        completionsByHabit: Map<Long, Set<Long>>,
        allCompletions: List<HabitCompletionEntity>,
        todayEpochDay: Long
    ): OverallProgressStats {
        val activeIds = habits.map { it.id }.toSet()
        val totalCheckIns = allCompletions.count { it.habitId in activeIds }
        val bestStreak = habits.maxOfOrNull { habit ->
            calculateHabitStats(habit, completionsByHabit[habit.id].orEmpty(), todayEpochDay).bestStreak
        } ?: 0

        val weekSummaries = calculateDailySummaries(
            habits = habits,
            completions = allCompletions,
            startEpochDay = todayEpochDay - 6L,
            endEpochDay = todayEpochDay
        )
        val totalScheduledWeek = weekSummaries.sumOf { it.scheduledCount }
        val totalCompletedWeek = weekSummaries.sumOf { it.completedCount }
        val weeklyRate = if (totalScheduledWeek == 0) 0f else (totalCompletedWeek.toFloat() / totalScheduledWeek).coerceIn(0f, 1f)

        // Daily consistency streak: consecutive days ending today (or yesterday) where at least 1 scheduled habit was completed
        val completedDaysAny = allCompletions
            .filter { it.habitId in activeIds }
            .map { it.epochDay }
            .toSet()
        var dailyConsistencyStreak = 0
        var cursor = if (todayEpochDay in completedDaysAny) todayEpochDay else todayEpochDay - 1L
        while (cursor in completedDaysAny) {
            dailyConsistencyStreak++
            cursor--
        }

        // Perfect day streak: consecutive days where 100% of scheduled habits were completed
        val earliestCreated = habits.minOfOrNull { it.createdAtEpochDay } ?: todayEpochDay
        val earliestCompleted = allCompletions.minOfOrNull { it.epochDay } ?: todayEpochDay
        val minDay = minOf(earliestCreated, earliestCompleted)
        val historySummaries = calculateDailySummaries(habits, allCompletions, minDay, todayEpochDay)
            .associateBy { it.epochDay }

        var perfectDayStreak = 0
        val todaySummary = historySummaries[todayEpochDay]
        var pCursor = if (todaySummary != null && todaySummary.scheduledCount > 0 && todaySummary.completedCount == todaySummary.scheduledCount) {
            todayEpochDay
        } else {
            todayEpochDay - 1L
        }
        while (pCursor >= minDay) {
            val s = historySummaries[pCursor]
            if (s == null || s.scheduledCount == 0) {
                pCursor--
                continue
            }
            if (s.completedCount == s.scheduledCount) {
                perfectDayStreak++
                pCursor--
            } else {
                break
            }
        }

        return OverallProgressStats(
            totalCheckIns = totalCheckIns,
            bestHabitStreak = bestStreak,
            weeklyCompletionRate = weeklyRate,
            activeHabitCount = habits.size,
            dailyConsistencyStreak = dailyConsistencyStreak,
            perfectDayStreak = perfectDayStreak
        )
    }

    fun summarizeDayForHabits(
        habits: List<HabitEntity>,
        completionsByHabit: Map<Long, Set<Long>>,
        epochDay: Long
    ): DayProgressSummary {
        val scheduledHabits = habits.filter { habit ->
            habit.createdAtEpochDay <= epochDay && DateUtil.isScheduledForDay(habit.scheduledDaysMask, epochDay)
        }
        val completedCount = scheduledHabits.count { habit ->
            epochDay in completionsByHabit[habit.id].orEmpty()
        }
        return DayProgressSummary(
            epochDay = epochDay,
            scheduledCount = scheduledHabits.size,
            completedCount = completedCount
        )
    }
}
