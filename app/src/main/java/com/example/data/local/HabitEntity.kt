package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.DateUtil

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String = "",
    val category: String = "Health",
    val iconKey: String = "water",
    val colorHex: Long = 0xFF2D8A61L,
    val scheduledDaysMask: Int = DateUtil.ALL_DAYS_MASK,
    val targetDays: Int = 21,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val createdAtEpochDay: Long = DateUtil.todayEpochDay(),
    val isArchived: Boolean = false
)
