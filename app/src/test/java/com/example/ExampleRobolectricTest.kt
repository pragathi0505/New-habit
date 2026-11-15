package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRepository
import com.example.data.local.ReminderPreferences
import com.example.util.DateUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HabitRepository(database.habitDao(), ReminderPreferences(context))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun readAppNameFromContext() {
        val appName = context.getString(R.string.app_name)
        assertEquals("DailyHabit", appName)
    }

    @Test
    fun repository_savesHabitAndTogglesCompletionOffline() = runTest {
        val saved = repository.saveHabit(
            HabitEntity(title = "Read 20 Pages", category = "Learning")
        )
        val today = DateUtil.todayEpochDay()

        val active = repository.activeHabits.first()
        assertEquals(1, active.size)
        assertEquals("Read 20 Pages", active.first().title)

        val completedNow = repository.toggleCompletion(saved.id, today)
        assertTrue(completedNow)
        assertEquals(1, repository.allCompletions.first().size)

        val completedAfterSecondTap = repository.toggleCompletion(saved.id, today)
        assertFalse(completedAfterSecondTap)
        assertEquals(0, repository.allCompletions.first().size)
    }

    @Test
    fun repository_seedsInitialHabitsOnceOnly() = runTest {
        repository.seedInitialHabitsIfNeeded()
        val seeded = repository.activeHabits.first()
        assertEquals(5, seeded.size)

        // Delete all seeded habits and verify a second call does not re-seed
        seeded.forEach { repository.deleteHabit(it.id) }
        assertEquals(0, repository.activeHabits.first().size)

        repository.seedInitialHabitsIfNeeded()
        assertEquals(0, repository.activeHabits.first().size)
    }

    @Test
    fun repository_persistsCustomHabitFields() = runTest {
        val customHabit = HabitEntity(
            title = "Evening Stretch",
            iconKey = "spa",
            colorHex = 0xFFE06D44L,
            scheduledDaysMask = DateUtil.WEEKDAYS_MASK,
            targetDays = 30,
            reminderEnabled = true,
            reminderHour = 20,
            reminderMinute = 15
        )
        repository.saveHabit(customHabit)

        val saved = repository.activeHabits.first().single()
        assertEquals("Evening Stretch", saved.title)
        assertEquals("spa", saved.iconKey)
        assertEquals(0xFFE06D44L, saved.colorHex)
        assertEquals(DateUtil.WEEKDAYS_MASK, saved.scheduledDaysMask)
        assertEquals(30, saved.targetDays)
        assertTrue(saved.reminderEnabled)
        assertEquals(20, saved.reminderHour)
        assertEquals(15, saved.reminderMinute)
    }

    @Test
    fun diskBackedDatabase_retainsHabitsCompletionsAndSettingsAfterReopen() = runTest {
        val dbName = "reopen_persistence_test_db"
        context.deleteDatabase(dbName)

        val today = DateUtil.todayEpochDay()

        // First app session: open DB, save habit, log 2 days of completions, save reminder prefs, and close
        val dbSession1 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE)
            .allowMainThreadQueries()
            .build()
        val repo1 = HabitRepository(dbSession1.habitDao(), ReminderPreferences(context))
        val created = repo1.saveHabit(
            HabitEntity(
                title = "Offline Meditation",
                iconKey = "meditate",
                colorHex = 0xFF2D8A61L,
                targetDays = 30,
                reminderEnabled = true,
                reminderHour = 7,
                reminderMinute = 45
            )
        )
        repo1.toggleCompletion(created.id, today - 1)
        repo1.toggleCompletion(created.id, today)
        repo1.updateDailySummary(enabled = true, hour = 20, minute = 30)
        dbSession1.close()

        // Second app session: reopen DB and Preferences from disk and verify everything is retained
        val dbSession2 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE)
            .allowMainThreadQueries()
            .build()
        val repo2 = HabitRepository(dbSession2.habitDao(), ReminderPreferences(context))

        val retainedHabits = repo2.activeHabits.first()
        val retainedCompletions = repo2.allCompletions.first()
        val retainedSummary = repo2.getDailySummarySettingsOnce()

        assertEquals(1, retainedHabits.size)
        assertEquals("Offline Meditation", retainedHabits.first().title)
        assertEquals(30, retainedHabits.first().targetDays)
        assertEquals(2, retainedCompletions.size)
        assertTrue(retainedSummary.enabled)
        assertEquals(20, retainedSummary.hour)
        assertEquals(30, retainedSummary.minute)

        dbSession2.close()
        context.deleteDatabase(dbName)
    }
}
