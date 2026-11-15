package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class HabitSemanticColors(
    val streakAccent: Color,
    val streakContainer: Color,
    val successAccent: Color,
    val successContainer: Color,
    val cardElevated: Color,
    val subtleDivider: Color
)

val LocalHabitColors = staticCompositionLocalOf {
    HabitSemanticColors(
        streakAccent = TerracottaSecondary,
        streakContainer = TerracottaSecondaryContainer,
        successAccent = BotanicalTertiary,
        successContainer = BotanicalTertiaryContainer,
        cardElevated = WarmSurface,
        subtleDivider = SubtleDivider
    )
}

private val LightColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = OnSagePrimary,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = OnSagePrimaryContainer,
    secondary = TerracottaSecondary,
    onSecondary = OnTerracottaSecondary,
    secondaryContainer = TerracottaSecondaryContainer,
    onSecondaryContainer = OnTerracottaSecondaryContainer,
    tertiary = BotanicalTertiary,
    onTertiary = OnBotanicalTertiary,
    tertiaryContainer = BotanicalTertiaryContainer,
    onTertiaryContainer = OnBotanicalTertiaryContainer,
    background = WarmAlabasterBackground,
    onBackground = CharcoalInk,
    surface = WarmSurface,
    onSurface = CharcoalInk,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = MutedMossText,
    outline = SoftStoneOutline,
    outlineVariant = SubtleDivider,
    error = ErrorCrimson,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkSagePrimary,
    onPrimary = DarkOnSagePrimary,
    primaryContainer = DarkSagePrimaryContainer,
    onPrimaryContainer = DarkOnSagePrimaryContainer,
    secondary = DarkTerracottaSecondary,
    onSecondary = DarkOnTerracottaSecondary,
    secondaryContainer = DarkTerracottaSecondaryContainer,
    onSecondaryContainer = DarkOnTerracottaSecondaryContainer,
    tertiary = DarkBotanicalTertiary,
    onTertiary = DarkOnBotanicalTertiary,
    tertiaryContainer = DarkBotanicalTertiaryContainer,
    onTertiaryContainer = DarkOnBotanicalTertiaryContainer,
    background = DeepPineBackground,
    onBackground = WarmIvoryInk,
    surface = DeepPineSurface,
    onSurface = WarmIvoryInk,
    surfaceVariant = DeepPineSurfaceVariant,
    onSurfaceVariant = MutedSageText,
    outline = DarkPineOutline,
    outlineVariant = DeepPineSurfaceVariant,
    error = Color(0xFFFF7A7A),
    onError = Color(0xFF420000)
)

private val DailyHabitShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DailyHabitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) {
        HabitSemanticColors(
            streakAccent = DarkTerracottaSecondary,
            streakContainer = DarkTerracottaSecondaryContainer,
            successAccent = DarkBotanicalTertiary,
            successContainer = DarkBotanicalTertiaryContainer,
            cardElevated = DeepPineSurface,
            subtleDivider = DeepPineSurfaceVariant
        )
    } else {
        HabitSemanticColors(
            streakAccent = TerracottaSecondary,
            streakContainer = TerracottaSecondaryContainer,
            successAccent = BotanicalTertiary,
            successContainer = BotanicalTertiaryContainer,
            cardElevated = WarmSurface,
            subtleDivider = SubtleDivider
        )
    }

    CompositionLocalProvider(LocalHabitColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DailyHabitTypography,
            shapes = DailyHabitShapes,
            content = content
        )
    }
}
