package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CalendarDayStatus
import com.example.domain.DayProgressSummary
import com.example.domain.StreakCalculator
import com.example.ui.components.HabitIcons
import com.example.ui.model.DailyHabitUiState
import com.example.ui.model.HabitWithStats
import com.example.ui.theme.LocalHabitColors
import com.example.util.DateUtil

@Composable
fun CalendarScreen(
    uiState: DailyHabitUiState,
    onToggleHabitOnDay: (habitId: Long, epochDay: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = uiState.todayEpochDay
    val (todayYear, todayMonth, _) = remember(today) { DateUtil.epochDayToCivil(today) }

    var displayedYear by remember { mutableIntStateOf(todayYear) }
    var displayedMonth by remember { mutableIntStateOf(todayMonth) }
    var selectedCalendarDay by remember { mutableLongStateOf(today) }
    var selectedHabitFilterId by remember { mutableStateOf<Long?>(null) }

    val filteredItems = remember(uiState.activeHabits, selectedHabitFilterId) {
        if (selectedHabitFilterId == null) uiState.activeHabits
        else uiState.activeHabits.filter { it.habit.id == selectedHabitFilterId }
    }

    val filteredHabitEntities = remember(filteredItems) { filteredItems.map { it.habit } }
    val completionsMap = remember(filteredItems) {
        filteredItems.associate { it.habit.id to it.completedEpochDays }
    }

    // Build month day summaries
    val daysCount = remember(displayedYear, displayedMonth) {
        DateUtil.daysInMonth(displayedYear, displayedMonth)
    }
    val firstDayEpoch = remember(displayedYear, displayedMonth) {
        DateUtil.civilToEpochDay(displayedYear, displayedMonth, 1)
    }
    val leadingEmptyCells = remember(firstDayEpoch) {
        DateUtil.dayOfWeekIndex(firstDayEpoch) // 0=Mon .. 6=Sun
    }

    val monthDaySummaries = remember(
        displayedYear,
        displayedMonth,
        filteredHabitEntities,
        completionsMap
    ) {
        (1..daysCount).map { dayOfMonth ->
            val epochDay = DateUtil.civilToEpochDay(displayedYear, displayedMonth, dayOfMonth)
            dayOfMonth to StreakCalculator.summarizeDayForHabits(
                habits = filteredHabitEntities,
                completionsByHabit = completionsMap,
                epochDay = epochDay
            )
        }
    }

    val completedDaysInMonth = monthDaySummaries.count { (_, s) ->
        s.status(today) == CalendarDayStatus.COMPLETED
    }
    val partialDaysInMonth = monthDaySummaries.count { (_, s) ->
        s.status(today) == CalendarDayStatus.PARTIAL
    }
    val missedDaysInMonth = monthDaySummaries.count { (_, s) ->
        s.status(today) == CalendarDayStatus.MISSED
    }

    // Streak stats for either All Habits or the selected filtered habit
    val selectedHabitObj = filteredItems.singleOrNull()?.takeIf { selectedHabitFilterId != null }
    val currentStreakValue = selectedHabitObj?.stats?.currentStreak ?: uiState.overallStats.dailyConsistencyStreak
    val longestStreakValue = selectedHabitObj?.stats?.bestStreak ?: uiState.overallStats.bestHabitStreak
    val thirdMetricLabel = if (selectedHabitObj != null) "30d Rate" else stringResource(R.string.streak_perfect_label)
    val thirdMetricValue = if (selectedHabitObj != null) {
        "${(selectedHabitObj.stats.completionRate30Days * 100).toInt()}%"
    } else {
        "${uiState.overallStats.perfectDayStreak}d"
    }

    // Habits scheduled on selectedCalendarDay
    val habitsOnSelectedDay = remember(filteredItems, selectedCalendarDay) {
        filteredItems.filter { item ->
            DateUtil.isScheduledForDay(item.habit.scheduledDaysMask, selectedCalendarDay)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("calendar_screen_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item(key = "calendar_header") {
                Column {
                    Text(
                        text = stringResource(R.string.calendar_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.calendar_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Streak Tracking Summary Card
            item(key = "streak_tracking_row") {
                val habitColors = LocalHabitColors.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("streak_tracking_summary"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StreakMetricCard(
                        icon = Icons.Rounded.LocalFireDepartment,
                        label = stringResource(R.string.streak_current_label),
                        value = "${currentStreakValue}d",
                        accent = habitColors.streakAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StreakMetricCard(
                        icon = Icons.Rounded.EmojiEvents,
                        label = stringResource(R.string.streak_longest_label),
                        value = "${longestStreakValue}d",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StreakMetricCard(
                        icon = Icons.Rounded.Verified,
                        label = thirdMetricLabel,
                        value = thirdMetricValue,
                        accent = habitColors.successAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Habit Filter Chips
            if (uiState.activeHabits.isNotEmpty()) {
                item(key = "habit_filter_chips") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedHabitFilterId == null,
                            onClick = { selectedHabitFilterId = null },
                            label = { Text("All Habits") },
                            modifier = Modifier.testTag("calendar_filter_all")
                        )
                        uiState.activeHabits.forEach { item ->
                            FilterChip(
                                selected = selectedHabitFilterId == item.habit.id,
                                onClick = { selectedHabitFilterId = item.habit.id },
                                label = { Text(item.habit.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("calendar_filter_${item.habit.id}")
                            )
                        }
                    }
                }
            }

            // Full Monthly Calendar Card
            item(key = "monthly_calendar_card") {
                MonthlyCalendarCard(
                    year = displayedYear,
                    month = displayedMonth,
                    leadingEmptyCells = leadingEmptyCells,
                    monthDaySummaries = monthDaySummaries,
                    selectedEpochDay = selectedCalendarDay,
                    todayEpochDay = today,
                    completedDaysInMonth = completedDaysInMonth,
                    partialDaysInMonth = partialDaysInMonth,
                    missedDaysInMonth = missedDaysInMonth,
                    onPreviousMonth = {
                        val (y, m) = DateUtil.addMonths(displayedYear, displayedMonth, -1)
                        displayedYear = y
                        displayedMonth = m
                    },
                    onNextMonth = {
                        val (y, m) = DateUtil.addMonths(displayedYear, displayedMonth, 1)
                        displayedYear = y
                        displayedMonth = m
                    },
                    onSelectDay = { epochDay ->
                        selectedCalendarDay = epochDay
                    }
                )
            }

            // Selected Day Breakdown Header
            item(key = "selected_day_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = DateUtil.formatFullDateWithYear(selectedCalendarDay),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedCalendarDay > today) {
                                "Upcoming scheduled habits"
                            } else {
                                "Tap a habit checkbox to update this day"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (habitsOnSelectedDay.isEmpty()) {
                item(key = "selected_day_empty") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = stringResource(R.string.no_habits_for_day),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(
                    items = habitsOnSelectedDay,
                    key = { "cal_day_${selectedCalendarDay}_${it.habit.id}" }
                ) { item ->
                    CalendarDayHabitRow(
                        item = item,
                        selectedEpochDay = selectedCalendarDay,
                        todayEpochDay = today,
                        onToggle = { onToggleHabitOnDay(item.habit.id, selectedCalendarDay) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MonthlyCalendarCard(
    year: Int,
    month: Int,
    leadingEmptyCells: Int,
    monthDaySummaries: List<Pair<Int, DayProgressSummary>>,
    selectedEpochDay: Long,
    todayEpochDay: Long,
    completedDaysInMonth: Int,
    partialDaysInMonth: Int,
    missedDaysInMonth: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Long) -> Unit
) {
    val habitColors = LocalHabitColors.current
    val completedColor = habitColors.successAccent
    val partialColor = habitColors.streakAccent
    val missedBg = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
    val missedText = MaterialTheme.colorScheme.error

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monthly_calendar_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Month Header with Previous / Next Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("calendar_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.previous_month)
                    )
                }

                Text(
                    text = DateUtil.formatMonthYear(year, month),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("calendar_month_title")
                )

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("calendar_next_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next_month)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Weekday Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                DateUtil.dayMediumLabels().forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid Cells (leading blanks + days of month)
            val totalCells = leadingEmptyCells + monthDaySummaries.size
            val rowCount = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (rowIdx in 0 until rowCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (colIdx in 0 until 7) {
                            val cellIndex = rowIdx * 7 + colIdx
                            val dayIndex = cellIndex - leadingEmptyCells
                            if (dayIndex < 0 || dayIndex >= monthDaySummaries.size) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            } else {
                                val (dayNumber, summary) = monthDaySummaries[dayIndex]
                                val status = summary.status(todayEpochDay)
                                val isSelected = summary.epochDay == selectedEpochDay
                                val isToday = summary.epochDay == todayEpochDay

                                val cellBg = when (status) {
                                    CalendarDayStatus.COMPLETED -> completedColor
                                    CalendarDayStatus.PARTIAL -> partialColor.copy(alpha = 0.22f)
                                    CalendarDayStatus.MISSED -> missedBg
                                    CalendarDayStatus.PENDING_TODAY -> MaterialTheme.colorScheme.primaryContainer
                                    CalendarDayStatus.REST, CalendarDayStatus.FUTURE ->
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                }

                                val cellTextColor = when (status) {
                                    CalendarDayStatus.COMPLETED -> Color.White
                                    CalendarDayStatus.PARTIAL -> partialColor
                                    CalendarDayStatus.MISSED -> missedText
                                    CalendarDayStatus.PENDING_TODAY -> MaterialTheme.colorScheme.onPrimaryContainer
                                    CalendarDayStatus.REST, CalendarDayStatus.FUTURE ->
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cellBg)
                                        .then(
                                            when {
                                                isSelected -> Modifier.border(
                                                    width = 2.dp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                isToday -> Modifier.border(
                                                    width = 1.5.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                else -> Modifier
                                            }
                                        )
                                        .clickable { onSelectDay(summary.epochDay) }
                                        .testTag("calendar_day_${summary.epochDay}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = cellTextColor
                                        )
                                        if (status == CalendarDayStatus.COMPLETED ||
                                            status == CalendarDayStatus.MISSED ||
                                            status == CalendarDayStatus.PARTIAL
                                        ) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (status) {
                                                            CalendarDayStatus.COMPLETED -> Color.White.copy(alpha = 0.9f)
                                                            CalendarDayStatus.PARTIAL -> partialColor
                                                            CalendarDayStatus.MISSED -> missedText
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color Legend Row + Month Counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarLegendItem(
                    color = completedColor,
                    label = "${stringResource(R.string.legend_completed)} ($completedDaysInMonth)"
                )
                CalendarLegendItem(
                    color = partialColor.copy(alpha = 0.55f),
                    label = "${stringResource(R.string.legend_partial)} ($partialDaysInMonth)"
                )
                CalendarLegendItem(
                    color = missedText,
                    label = "${stringResource(R.string.legend_missed)} ($missedDaysInMonth)"
                )
            }
        }
    }
}

@Composable
private fun CalendarLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarDayHabitRow(
    item: HabitWithStats,
    selectedEpochDay: Long,
    todayEpochDay: Long,
    onToggle: () -> Unit
) {
    val habit = item.habit
    val accent = Color(habit.colorHex)
    val isDone = selectedEpochDay in item.completedEpochDays
    val isPastMissed = !isDone && selectedEpochDay < todayEpochDay && selectedEpochDay >= habit.createdAtEpochDay
    val canToggle = selectedEpochDay <= todayEpochDay

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = canToggle, onClick = onToggle),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = HabitIcons.fromKey(habit.iconKey),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${habit.category} • ${item.stats.currentStreak}d streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Explicit Completed / Missed / Pending pill
            Surface(
                color = when {
                    isDone -> LocalHabitColors.current.successContainer
                    isPastMissed -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        tint = when {
                            isDone -> LocalHabitColors.current.successAccent
                            isPastMissed -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            isDone -> stringResource(R.string.legend_completed)
                            isPastMissed -> stringResource(R.string.legend_missed)
                            selectedEpochDay > todayEpochDay -> "Upcoming"
                            else -> "Pending"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isDone -> LocalHabitColors.current.successAccent
                            isPastMissed -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
