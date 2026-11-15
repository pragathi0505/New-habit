package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.DayProgressSummary
import com.example.ui.theme.LocalHabitColors
import com.example.util.DateUtil

@Composable
fun WeekStrip(
    weekSummaries: List<DayProgressSummary>,
    selectedEpochDay: Long,
    todayEpochDay: Long,
    onSelectDay: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val habitColors = LocalHabitColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("week_strip"),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weekSummaries.forEach { summary ->
            val isSelected = summary.epochDay == selectedEpochDay
            val isToday = summary.epochDay == todayEpochDay

            val containerColor by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surface
                },
                label = "day_pill_bg"
            )
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            val dotColor = when {
                summary.scheduledCount == 0 -> Color.Transparent
                summary.completedCount >= summary.scheduledCount -> {
                    if (isSelected) MaterialTheme.colorScheme.onPrimary else habitColors.successAccent
                }
                summary.completedCount > 0 -> {
                    if (isSelected) habitColors.streakContainer else habitColors.streakAccent
                }
                else -> {
                    if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.outlineVariant
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(containerColor)
                    .then(
                        if (!isSelected && isToday) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(16.dp)
                            )
                        } else if (!isSelected) {
                            Modifier.border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelectDay(summary.epochDay) }
                    .padding(vertical = 10.dp, horizontal = 2.dp)
                    .testTag("week_day_${summary.epochDay}"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = DateUtil.formatShortDayName(summary.epochDay).take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = if (isSelected) 0.85f else 0.65f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DateUtil.formatDayNumber(summary.epochDay),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.SemiBold,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}
