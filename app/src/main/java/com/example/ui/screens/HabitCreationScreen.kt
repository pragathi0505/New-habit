package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.HabitEntity
import com.example.ui.components.HabitIcons
import com.example.ui.components.HabitTimePickerDialog
import com.example.ui.model.HabitCategories
import com.example.ui.theme.HabitSwatchColors
import com.example.util.DateUtil

private enum class FrequencyPreset(val label: String) {
    DAILY("Every day"),
    WEEKDAYS("Weekdays"),
    WEEKENDS("Weekends"),
    CUSTOM("Custom")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitCreationScreen(
    initialHabit: HabitEntity?,
    onNavigateBack: () -> Unit,
    onSaveHabit: (HabitEntity) -> Unit,
    onRequestNotificationPermissionIfNeeded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = initialHabit != null

    var title by remember(initialHabit) { mutableStateOf(initialHabit?.title ?: "") }
    var description by remember(initialHabit) { mutableStateOf(initialHabit?.description ?: "") }
    var category by remember(initialHabit) { mutableStateOf(initialHabit?.category ?: HabitCategories.all.first()) }
    var iconKey by remember(initialHabit) { mutableStateOf(initialHabit?.iconKey ?: HabitIcons.options.first().key) }
    var colorHex by remember(initialHabit) { mutableLongStateOf(initialHabit?.colorHex ?: HabitSwatchColors.first()) }
    var daysMask by remember(initialHabit) { mutableIntStateOf(initialHabit?.scheduledDaysMask ?: DateUtil.ALL_DAYS_MASK) }
    var targetDays by remember(initialHabit) { mutableIntStateOf(initialHabit?.targetDays ?: 21) }
    var reminderEnabled by remember(initialHabit) { mutableStateOf(initialHabit?.reminderEnabled ?: false) }
    var reminderHour by remember(initialHabit) { mutableIntStateOf(initialHabit?.reminderHour ?: 8) }
    var reminderMinute by remember(initialHabit) { mutableIntStateOf(initialHabit?.reminderMinute ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }
    var attemptedSubmit by remember { mutableStateOf(false) }

    val activeAccent = Color(colorHex)
    val selectedPreset = when (daysMask) {
        DateUtil.ALL_DAYS_MASK -> FrequencyPreset.DAILY
        DateUtil.WEEKDAYS_MASK -> FrequencyPreset.WEEKDAYS
        DateUtil.WEEKENDS_MASK -> FrequencyPreset.WEEKENDS
        else -> FrequencyPreset.CUSTOM
    }

    val submitHabit = {
        attemptedSubmit = true
        if (title.trim().isNotEmpty() && daysMask != 0) {
            val habitToSave = (initialHabit ?: HabitEntity(title = "")).copy(
                title = title.trim(),
                description = description.trim(),
                category = category,
                iconKey = iconKey,
                colorHex = colorHex,
                scheduledDaysMask = daysMask,
                targetDays = targetDays,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
            onSaveHabit(habitToSave)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (isEditing) R.string.edit_habit_title else R.string.create_habit_title
                        ),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("habit_form_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = submitHabit,
                            modifier = Modifier
                                .weight(1.4f)
                                .minimumInteractiveComponentSize()
                                .testTag("save_habit_button")
                        ) {
                            Text(
                                stringResource(
                                    if (isEditing) R.string.save_habit else R.string.create_habit_cta
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("habit_creation_screen"),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Live Preview Card
                HabitLivePreviewCard(
                    title = title.ifBlank { stringResource(R.string.habit_name_placeholder) },
                    isPlaceholder = title.isBlank(),
                    description = description,
                    category = category,
                    iconKey = iconKey,
                    accentColor = activeAccent,
                    daysMask = daysMask,
                    targetDays = targetDays,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute
                )

                // Name & Description Section
                FormSectionCard(title = stringResource(R.string.habit_name_label)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text(stringResource(R.string.habit_name_placeholder)) },
                        singleLine = true,
                        isError = attemptedSubmit && title.trim().isEmpty(),
                        supportingText = if (attemptedSubmit && title.trim().isEmpty()) {
                            { Text(stringResource(R.string.habit_name_required_error)) }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("habit_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.habit_note_label)) },
                        placeholder = { Text(stringResource(R.string.habit_note_placeholder)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("habit_note_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.category_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HabitCategories.all.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                // Color & Icon Section
                FormSectionCard(title = stringResource(R.string.color_label)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HabitSwatchColors.forEach { swatch ->
                            val selected = swatch == colorHex
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(swatch))
                                    .then(
                                        if (selected) {
                                            Modifier.border(
                                                3.dp,
                                                MaterialTheme.colorScheme.onSurface,
                                                CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { colorHex = swatch }
                                    .testTag("color_swatch_$swatch"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected color",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.icon_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HabitIcons.options.forEach { option ->
                            val selected = option.key == iconKey
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) activeAccent.copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .then(
                                        if (selected) {
                                            Modifier.border(2.dp, activeAccent, RoundedCornerShape(14.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { iconKey = option.key }
                                    .testTag("icon_option_${option.key}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.label,
                                    tint = if (selected) activeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Frequency & Target Days Section
                FormSectionCard(title = stringResource(R.string.frequency_label)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FrequencyPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = selectedPreset == preset,
                                onClick = {
                                    daysMask = when (preset) {
                                        FrequencyPreset.DAILY -> DateUtil.ALL_DAYS_MASK
                                        FrequencyPreset.WEEKDAYS -> DateUtil.WEEKDAYS_MASK
                                        FrequencyPreset.WEEKENDS -> DateUtil.WEEKENDS_MASK
                                        FrequencyPreset.CUSTOM -> daysMask
                                    }
                                },
                                label = { Text(preset.label) },
                                modifier = Modifier.testTag("frequency_${preset.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.schedule_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DateUtil.dayShortLabels().forEachIndexed { dayIndex, label ->
                            val bit = 1 shl dayIndex
                            val selected = (daysMask and bit) != 0
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) activeAccent
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        val toggled = daysMask xor bit
                                        if (toggled != 0) daysMask = toggled
                                    }
                                    .testTag("target_weekday_$dayIndex"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = stringResource(R.string.target_days_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.target_days_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(7, 14, 21, 30, 66).forEach { goal ->
                            FilterChip(
                                selected = targetDays == goal,
                                onClick = { targetDays = goal },
                                label = { Text("${goal}d") },
                                modifier = Modifier.testTag("target_days_preset_$goal")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Flag,
                                    contentDescription = null,
                                    tint = activeAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$targetDays target days",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("target_days_value")
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (targetDays > 1) targetDays-- },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease target days")
                                }
                                IconButton(
                                    onClick = { if (targetDays < 365) targetDays++ },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase target days")
                                }
                            }
                        }
                    }
                }

                // Reminder Time Section
                FormSectionCard(title = stringResource(R.string.reminder_label)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (reminderEnabled) "Reminder active" else "No reminder set",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Get an offline notification on scheduled days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { checked ->
                                reminderEnabled = checked
                                if (checked) onRequestNotificationPermissionIfNeeded()
                            },
                            modifier = Modifier.testTag("editor_reminder_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = activeAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showTimePicker = true }
                            .minimumInteractiveComponentSize()
                            .testTag("reminder_time_picker_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTime,
                                    contentDescription = null,
                                    tint = activeAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = DateUtil.formatTime(reminderHour, reminderMinute),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = activeAccent
                                )
                            }
                            Text(
                                text = "Change time",
                                style = MaterialTheme.typography.labelLarge,
                                color = activeAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            7 to 30,
                            12 to 30,
                            18 to 0,
                            21 to 0
                        ).forEach { (h, m) ->
                            val selected = reminderEnabled && reminderHour == h && reminderMinute == m
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    reminderHour = h
                                    reminderMinute = m
                                    reminderEnabled = true
                                    onRequestNotificationPermissionIfNeeded()
                                },
                                label = { Text(DateUtil.formatTime(h, m)) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showTimePicker) {
        HabitTimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                reminderHour = hour
                reminderMinute = minute
                reminderEnabled = true
                showTimePicker = false
                onRequestNotificationPermissionIfNeeded()
            }
        )
    }
}

@Composable
private fun HabitLivePreviewCard(
    title: String,
    isPlaceholder: Boolean,
    description: String,
    category: String,
    iconKey: String,
    accentColor: Color,
    daysMask: Int,
    targetDays: Int,
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.35f), MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.live_preview_label),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = HabitIcons.fromKey(iconKey),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPlaceholder) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (description.isNotBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$category • ${DateUtil.formatScheduleLabel(daysMask)} • ${targetDays}d goal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (reminderEnabled) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = DateUtil.formatTime(reminderHour, reminderMinute),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
