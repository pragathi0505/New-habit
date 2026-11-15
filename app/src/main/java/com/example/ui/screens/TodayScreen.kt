package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.HabitCard
import com.example.ui.components.ProgressRingCard
import com.example.ui.components.WeekStrip
import com.example.ui.model.DailyHabitUiState
import com.example.ui.model.HabitWithStats
import com.example.util.DateUtil

@Composable
fun TodayScreen(
    uiState: DailyHabitUiState,
    onSelectDay: (Long) -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleHabit: (Long) -> Unit,
    onOpenHabitDetail: (HabitWithStats) -> Unit,
    onAddHabitClick: () -> Unit,
    onAddStarterPack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = uiState.selectedEpochDay == uiState.todayEpochDay
    val greetingRes = DateUtil.currentGreetingResId()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("today_screen_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Prominent Today's Date Badge & Greeting
            item(key = "header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("todays_date_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isToday) {
                                        "Today • ${DateUtil.formatFullDateWithYear(uiState.todayEpochDay)}"
                                    } else {
                                        DateUtil.formatFullDateWithYear(uiState.selectedEpochDay)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (!isToday) {
                            TextButton(
                                onClick = { onSelectDay(uiState.todayEpochDay) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(stringResource(R.string.jump_to_today))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(greetingRes),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Today's Progress Hero Card (Completion Percentage + Progress Ring + Linear Bar)
            item(key = "progress_ring") {
                ProgressRingCard(
                    completedCount = uiState.allCompletedCountOnSelectedDay,
                    totalCount = uiState.allScheduledCountOnSelectedDay,
                    dailyConsistencyStreak = uiState.overallStats.dailyConsistencyStreak,
                    isToday = isToday,
                    dateLabel = DateUtil.formatMonthDay(uiState.selectedEpochDay)
                )
            }

            // 7-Day Week Strip
            item(key = "week_strip") {
                WeekStrip(
                    weekSummaries = uiState.weekSummaries,
                    selectedEpochDay = uiState.selectedEpochDay,
                    todayEpochDay = uiState.todayEpochDay,
                    onSelectDay = onSelectDay
                )
            }

            // Empty onboarding state if user deleted/archived all habits
            if (!uiState.isLoading && uiState.activeHabits.isEmpty()) {
                item(key = "empty_onboarding") {
                    EmptyHabitsCard(
                        onAddStarterPack = onAddStarterPack,
                        onCreateHabit = onAddHabitClick
                    )
                }
            } else {
                // Section Title + Counter Pill
                item(key = "habits_section_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isToday) {
                                stringResource(R.string.todays_habits_heading)
                            } else {
                                stringResource(R.string.day_habits_heading)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${uiState.allCompletedCountOnSelectedDay}/${uiState.allScheduledCountOnSelectedDay}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Category Filter Chips
                if (uiState.categoriesPresent.size > 2) {
                    item(key = "category_chips") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.categoriesPresent.forEach { category ->
                                FilterChip(
                                    selected = uiState.selectedCategory == category,
                                    onClick = { onSelectCategory(category) },
                                    label = { Text(category) },
                                    modifier = Modifier.testTag("filter_chip_$category")
                                )
                            }
                        }
                    }
                }

                val dayHabits = uiState.scheduledForSelectedDay
                if (dayHabits.isEmpty()) {
                    item(key = "rest_day") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.no_habits_for_day),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.no_habits_for_day_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = dayHabits,
                        key = { it.habit.id }
                    ) { item ->
                        HabitCard(
                            item = item,
                            canToggle = uiState.selectedEpochDay <= uiState.todayEpochDay,
                            onToggle = { onToggleHabit(item.habit.id) },
                            onClick = { onOpenHabitDetail(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHabitsCard(
    onAddStarterPack: () -> Unit,
    onCreateHabit: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_habits_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_habit_empty),
                contentDescription = stringResource(R.string.cd_empty_illustration),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.empty_habits_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.empty_habits_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCreateHabit,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("empty_create_habit_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.create_first_habit))
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onAddStarterPack,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("empty_starter_pack_button")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_starter_pack))
            }
        }
    }
}
