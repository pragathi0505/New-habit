package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

data class HabitIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object HabitIcons {
    val options: List<HabitIconOption> = listOf(
        HabitIconOption("water", "Hydration", Icons.Rounded.WaterDrop),
        HabitIconOption("meditate", "Mindfulness", Icons.Rounded.SelfImprovement),
        HabitIconOption("book", "Reading", Icons.Rounded.AutoStories),
        HabitIconOption("run", "Cardio", Icons.Rounded.DirectionsRun),
        HabitIconOption("fitness", "Strength", Icons.Rounded.FitnessCenter),
        HabitIconOption("sleep", "Sleep", Icons.Rounded.Bedtime),
        HabitIconOption("journal", "Journal", Icons.Rounded.EditNote),
        HabitIconOption("tea", "Routine", Icons.Rounded.LocalCafe),
        HabitIconOption("spa", "Wellness", Icons.Rounded.Spa),
        HabitIconOption("code", "Focus", Icons.Rounded.Code),
        HabitIconOption("music", "Creative", Icons.Rounded.MusicNote),
        HabitIconOption("check", "General", Icons.Rounded.CheckCircle)
    )

    fun fromKey(key: String): ImageVector {
        return options.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.CheckCircle
    }
}
