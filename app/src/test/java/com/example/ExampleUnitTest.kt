package com.example

import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.domain.StreakCalculator
import com.example.util.DateUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun dateUtil_weekdayMaskMatchesExpectedDays() {
        // 2026-11-16 is a Monday
        val monday = DateUtil.civilToEpochDay(2026, 11, 16)
        val friday = monday + 4
        val saturday = monday + 5
        val sunday = monday + 6

        assertEquals(0, DateUtil.dayOfWeekIndex(monday))
        assertTrue(DateUtil.isScheduledForDay(DateUtil.WEEKDAYS_MASK, monday))
        assertTrue(DateUtil.isScheduledForDay(DateUtil.WEEKDAYS_MASK, friday))
        assertFalse(DateUtil.isScheduledForDay(DateUtil.WEEKDAYS_MASK, saturday))
        assertFalse(DateUtil.isScheduledForDay(DateUtil.WEEKDAYS_MASK, sunday))
    }

    @Test
    fun streakCalculator_preservesStreakAcrossScheduledRestDays() {
        val monday = DateUtil.civilToEpochDay(2026, 11, 16)
        val thursdayBefore = monday - 4
        val fridayBefore = monday - 3
        // Habit scheduled weekdays only, completed Thu, Fri, and Mon (Sat/Sun skipped)
        val habit = HabitEntity(
            id = 1L,
            title = "Focus Block",
            scheduledDaysMask = DateUtil.WEEKDAYS_MASK,
            createdAtEpochDay = thursdayBefore
        )
        val completed = setOf(thursdayBefore, fridayBefore, monday)

        val stats = StreakCalculator.calculateHabitStats(
            habit = habit,
            completedDays = completed,
            todayEpochDay = monday
        )

        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
        assertEquals(1.0f, stats.completionRate30Days, 0.01f)
    }

    @Test
    fun streakCalculator_overallStatsCountsWeeklyAndConsistencyStreak() {
        val today = DateUtil.civilToEpochDay(2026, 11, 16)
        val habit = HabitEntity(
            id = 1L,
            title = "Hydrate",
            scheduledDaysMask = DateUtil.ALL_DAYS_MASK,
            createdAtEpochDay = today - 2
        )
        val completions = listOf(
            HabitCompletionEntity(1L, today - 2),
            HabitCompletionEntity(1L, today - 1),
            HabitCompletionEntity(1L, today)
        )

        val overall = StreakCalculator.calculateOverallStats(
            habits = listOf(habit),
            completionsByHabit = mapOf(1L to setOf(today - 2, today - 1, today)),
            allCompletions = completions,
            todayEpochDay = today
        )

        assertEquals(3, overall.totalCheckIns)
        assertEquals(3, overall.bestHabitStreak)
        assertEquals(3, overall.dailyConsistencyStreak)
        assertEquals(3, overall.perfectDayStreak)
    }

    @Test
    fun calendarDayStatus_distinguishesCompletedPartialMissedAndFuture() {
        val today = DateUtil.civilToEpochDay(2026, 11, 15)
        val completedDay = com.example.domain.DayProgressSummary(today - 3, scheduledCount = 2, completedCount = 2)
        val partialDay = com.example.domain.DayProgressSummary(today - 2, scheduledCount = 2, completedCount = 1)
        val missedDay = com.example.domain.DayProgressSummary(today - 1, scheduledCount = 2, completedCount = 0)
        val futureDay = com.example.domain.DayProgressSummary(today + 1, scheduledCount = 2, completedCount = 0)

        assertEquals(com.example.domain.CalendarDayStatus.COMPLETED, completedDay.status(today))
        assertEquals(com.example.domain.CalendarDayStatus.PARTIAL, partialDay.status(today))
        assertEquals(com.example.domain.CalendarDayStatus.MISSED, missedDay.status(today))
        assertEquals(com.example.domain.CalendarDayStatus.FUTURE, futureDay.status(today))
    }
}
