package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalHabitColors
import kotlin.math.roundToInt

@Composable
fun ProgressRingCard(
    completedCount: Int,
    totalCount: Int,
    dailyConsistencyStreak: Int,
    isToday: Boolean,
    dateLabel: String,
    modifier: Modifier = Modifier
) {
    val targetProgress = if (totalCount <= 0) 0f else (completedCount.toFloat() / totalCount).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress_ring"
    )
    val percentageInt = (targetProgress * 100f).roundToInt()
    val remainingCount = (totalCount - completedCount).coerceAtLeast(0)

    val habitColors = LocalHabitColors.current
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val cardBrush = Brush.linearGradient(
        colors = listOf(
            primary,
            primary.copy(alpha = 0.90f),
            tertiary.copy(alpha = 0.92f)
        )
    )
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(cardBrush)
            .padding(20.dp)
            .testTag("daily_progress_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isToday) {
                                stringResource(R.string.todays_progress_title)
                            } else {
                                stringResource(R.string.day_progress_title)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = onPrimary.copy(alpha = 0.88f),
                            fontWeight = FontWeight.SemiBold
                        )

                        Surface(
                            color = onPrimary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = habitColors.streakContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (dailyConsistencyStreak > 0) {
                                        "${dailyConsistencyStreak}d streak"
                                    } else {
                                        "0d streak"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = when {
                            totalCount == 0 -> stringResource(R.string.no_habits_for_day)
                            completedCount == totalCount -> stringResource(R.string.all_done_title)
                            else -> stringResource(R.string.completed_count_format, completedCount, totalCount)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = onPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when {
                            totalCount == 0 -> dateLabel
                            completedCount == totalCount -> stringResource(R.string.all_done_subtitle)
                            else -> "${stringResource(R.string.remaining_count_format, remainingCount)} • $percentageInt% complete"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = onPrimary.copy(alpha = 0.86f)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .testTag("completion_percentage_ring"),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = onPrimary.copy(alpha = 0.22f)
                    val arcColor = onPrimary
                    Canvas(modifier = Modifier.size(88.dp)) {
                        val strokeWidth = 9.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                        val arcSize = Size(diameter, diameter)

                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        if (animatedProgress > 0f) {
                            drawArc(
                                color = arcColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percentageInt%",
                            style = MaterialTheme.typography.titleMedium,
                            color = onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("completion_percentage_text")
                        )
                        Text(
                            text = "done",
                            style = MaterialTheme.typography.labelSmall,
                            color = onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (totalCount > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(onPrimary.copy(alpha = 0.20f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(onPrimary)
                    )
                }
            }
        }
    }
}
