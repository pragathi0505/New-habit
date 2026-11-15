package com.example.util

import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * API 24+ safe calendar and epochDay utilities using proleptic Gregorian day math.
 * Avoids java.time.LocalDate crashes on Android 7.0 (API 24-25).
 */
object DateUtil {
    const val ALL_DAYS_MASK = 0b1111111 // Mon..Sun (bits 0..6)
    const val WEEKDAYS_MASK = 0b0011111 // Mon..Fri (bits 0..4)
    const val WEEKENDS_MASK = 0b1100000 // Sat..Sun (bits 5..6)

    fun todayEpochDay(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return civilToEpochDay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Converts (year, month 1..12, day 1..31) to days since 1970-01-01.
     */
    fun civilToEpochDay(year: Int, month: Int, day: Int): Long {
        val y = year - if (month <= 2) 1 else 0
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mPrime = month + if (month > 2) -3 else 9
        val doy = (153 * mPrime + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468L
    }

    /**
     * Converts days since 1970-01-01 to Triple(year, month 1..12, day 1..31).
     */
    fun epochDayToCivil(epochDay: Long): Triple<Int, Int, Int> {
        val z = epochDay + 719468L
        val era = (if (z >= 0) z else z - 146096L) / 146097L
        val doe = (z - era * 146097L).toInt()
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era.toInt() * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + if (mp < 10) 3 else -9
        val year = y + if (m <= 2) 1 else 0
        return Triple(year, m, d)
    }

    fun epochDayToCalendar(epochDay: Long): Calendar {
        val (year, month, day) = epochDayToCivil(epochDay)
        return Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)
        }
    }

    /**
     * Returns 0 for Monday .. 6 for Sunday.
     * 1970-01-01 was a Thursday (index 3).
     */
    fun dayOfWeekIndex(epochDay: Long): Int {
        val mod = ((epochDay + 3) % 7).toInt()
        return if (mod < 0) mod + 7 else mod
    }

    fun isScheduledForDay(scheduledDaysMask: Int, epochDay: Long): Boolean {
        val bit = 1 shl dayOfWeekIndex(epochDay)
        return (scheduledDaysMask and bit) != 0
    }

    fun dayShortLabels(): List<String> = listOf("M", "T", "W", "T", "F", "S", "S")

    fun dayMediumLabels(): List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    fun formatShortDayName(epochDay: Long): String {
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        return sdf.format(epochDayToCalendar(epochDay).time)
    }

    fun formatDayNumber(epochDay: Long): String {
        val (_, _, day) = epochDayToCivil(epochDay)
        return day.toString()
    }

    fun formatFullDate(epochDay: Long): String {
        val sdf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        return sdf.format(epochDayToCalendar(epochDay).time)
    }

    fun formatFullDateWithYear(epochDay: Long): String {
        val sdf = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        return sdf.format(epochDayToCalendar(epochDay).time)
    }

    fun formatMonthYear(year: Int, month: Int): String {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1, 12, 0, 0)
        }
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun daysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1, 12, 0, 0)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun addMonths(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1, 12, 0, 0)
            add(Calendar.MONTH, delta)
        }
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    fun formatMonthDay(epochDay: Long): String {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        return sdf.format(epochDayToCalendar(epochDay).time)
    }

    fun formatTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun formatScheduleLabel(mask: Int): String {
        return when (mask) {
            ALL_DAYS_MASK -> "Every day"
            WEEKDAYS_MASK -> "Weekdays"
            WEEKENDS_MASK -> "Weekends"
            else -> {
                val names = dayMediumLabels()
                (0..6).filter { (mask and (1 shl it)) != 0 }.joinToString(", ") { names[it] }
            }
        }
    }

    fun currentGreetingResId(nowMillis: Long = System.currentTimeMillis()): Int {
        val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> R.string.greeting_morning
            hour < 17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
    }
}
