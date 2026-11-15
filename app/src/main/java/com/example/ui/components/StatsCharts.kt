package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.DayProgressSummary
import com.example.ui.theme.LocalHabitColors
import com.example.util.DateUtil

@Composable
fun WeeklyConsistencyChart(
    weekSummaries: List<DayProgressSummary>,
    modifier: Modifier = Modifier
) {
    val habitColors = LocalHabitColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val guideLineColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_chart_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.weekly_consistency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val totalDone = weekSummaries.sumOf { it.completedCount }
                val totalScheduled = weekSummaries.sumOf { it.scheduledCount }
                Text(
                    text = "$totalDone / $totalScheduled",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                val barCount = weekSummaries.size.coerceAtLeast(1)
                val slotWidth = size.width / barCount
                val barWidth = (slotWidth * 0.44f).coerceAtMost(32.dp.toPx())
                val chartHeight = size.height
                val corner = CornerRadius(10.dp.toPx(), 10.dp.toPx())

                // Horizontal reference lines (50% and 100%)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                drawLine(
                    color = guideLineColor,
                    start = Offset(0f, chartHeight * 0.5f),
                    end = Offset(size.width, chartHeight * 0.5f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
                drawLine(
                    color = guideLineColor,
                    start = Offset(0f, 1.dp.toPx()),
                    end = Offset(size.width, 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                weekSummaries.forEachIndexed { index, summary ->
                    val left = index * slotWidth + (slotWidth - barWidth) / 2f
                    // Background track
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = corner
                    )

                    val ratio = summary.ratio
                    if (ratio > 0f) {
                        val barHeight = (chartHeight * ratio).coerceAtLeast(12.dp.toPx())
                        val fillColor = when {
                            ratio >= 1f -> habitColors.successAccent
                            ratio >= 0.5f -> primaryColor
                            else -> habitColors.streakAccent
                        }
                        drawRoundRect(
                            color = fillColor,
                            topLeft = Offset(left, chartHeight - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = corner
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                weekSummaries.forEach { summary ->
                    Text(
                        text = DateUtil.formatShortDayName(summary.epochDay).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ConsistencyHeatmapCard(
    summaries35Days: List<DayProgressSummary>,
    modifier: Modifier = Modifier
) {
    val habitColors = LocalHabitColors.current
    val baseSuccess = habitColors.successAccent
    val emptyTile = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("consistency_heatmap_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.activity_heatmap),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Less",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(0f, 0.3f, 0.65f, 1f).forEach { level ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (level == 0f) emptyTile
                                    else baseSuccess.copy(alpha = 0.25f + level * 0.75f)
                                )
                        )
                    }
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5 rows x 7 columns = 35 days
            val rows = summaries35Days.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { weekRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weekRow.forEach { day ->
                            val tileColor: Color = when {
                                day.scheduledCount == 0 || day.completedCount == 0 -> emptyTile
                                day.ratio >= 1f -> baseSuccess
                                day.ratio >= 0.6f -> baseSuccess.copy(alpha = 0.68f)
                                else -> baseSuccess.copy(alpha = 0.38f)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(tileColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = DateUtil.formatDayNumber(day.epochDay),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (day.completedCount > 0 && day.ratio >= 0.6f) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
