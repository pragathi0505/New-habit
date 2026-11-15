package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.reminders.HabitNotificationHelper
import com.example.ui.components.HabitDetailSheet
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HabitCreationScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.DailyHabitTheme
import com.example.ui.viewmodel.HabitViewModel
import kotlinx.coroutines.launch

private data class NavDestinationItem(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private object Routes {
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val STATS = "stats"
    const val REMINDERS = "reminders"
    const val HABIT_FORM_PATTERN = "habit_form?habitId={habitId}"
    fun habitForm(habitId: Long = -1L): String = "habit_form?habitId=$habitId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DailyHabitApp
        val factory = HabitViewModel.Factory(app.repository, app.reminderScheduler)

        setContent {
            DailyHabitTheme {
                DailyHabitRoot(factory = factory)
            }
        }
    }
}

@Composable
fun DailyHabitRoot(factory: HabitViewModel.Factory) {
    val viewModel: HabitViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var hasNotificationPermission by remember {
        mutableStateOf(HabitNotificationHelper.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("Enable notifications in Android Settings to receive reminders.")
            }
        }
    }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !HabitNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermission = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTodayIfNeeded()
                hasNotificationPermission = HabitNotificationHelper.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var detailHabitId by remember { mutableStateOf<Long?>(null) }

    val navItems = remember {
        listOf(
            NavDestinationItem(
                route = Routes.TODAY,
                labelResId = R.string.nav_today,
                selectedIcon = Icons.Rounded.CheckCircle,
                unselectedIcon = Icons.Outlined.CheckCircle
            ),
            NavDestinationItem(
                route = Routes.CALENDAR,
                labelResId = R.string.nav_calendar,
                selectedIcon = Icons.Rounded.CalendarMonth,
                unselectedIcon = Icons.Outlined.CalendarMonth
            ),
            NavDestinationItem(
                route = Routes.STATS,
                labelResId = R.string.nav_stats,
                selectedIcon = Icons.Rounded.BarChart,
                unselectedIcon = Icons.Outlined.BarChart
            ),
            NavDestinationItem(
                route = Routes.REMINDERS,
                labelResId = R.string.nav_reminders,
                selectedIcon = Icons.Rounded.Notifications,
                unselectedIcon = Icons.Outlined.Notifications
            )
        )
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.TODAY
    val isTopLevelRoute = currentRoute in setOf(Routes.TODAY, Routes.CALENDAR, Routes.STATS, Routes.REMINDERS)

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (currentRoute == Routes.TODAY) {
                    FloatingActionButton(
                        onClick = { navController.navigate(Routes.habitForm()) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_habit_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.add_habit)
                        )
                    }
                }
            },
            bottomBar = {
                if (isTopLevelRoute && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelResId)
                                    )
                                },
                                label = { Text(stringResource(item.labelResId)) },
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelRoute && useNavigationRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelResId)
                                    )
                                },
                                label = { Text(stringResource(item.labelResId)) },
                                modifier = Modifier.testTag("rail_${item.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.TODAY
                    ) {
                        composable(Routes.TODAY) {
                            TodayScreen(
                                uiState = uiState,
                                onSelectDay = viewModel::selectDay,
                                onSelectCategory = viewModel::selectCategory,
                                onToggleHabit = { habitId ->
                                    viewModel.toggleHabitCompletion(habitId, uiState.selectedEpochDay)
                                },
                                onOpenHabitDetail = { item -> detailHabitId = item.habit.id },
                                onAddHabitClick = { navController.navigate(Routes.habitForm()) },
                                onAddStarterPack = { viewModel.addStarterHabits() }
                            )
                        }

                        composable(Routes.CALENDAR) {
                            CalendarScreen(
                                uiState = uiState,
                                onToggleHabitOnDay = { habitId, epochDay ->
                                    viewModel.toggleHabitCompletion(habitId, epochDay)
                                }
                            )
                        }

                        composable(Routes.STATS) {
                            StatsScreen(
                                uiState = uiState,
                                onOpenHabitDetail = { item -> detailHabitId = item.habit.id }
                            )
                        }

                        composable(Routes.REMINDERS) {
                            val testSentMessage = stringResource(R.string.test_notification_sent)
                            RemindersScreen(
                                uiState = uiState,
                                hasNotificationPermission = hasNotificationPermission,
                                onRequestNotificationPermission = requestNotificationPermission,
                                onUpdateDailySummary = viewModel::updateDailySummaryReminder,
                                onToggleHabitReminder = viewModel::toggleHabitReminder,
                                onUpdateHabitReminderTime = viewModel::updateHabitReminderTime,
                                onSendTestNotification = {
                                    HabitNotificationHelper.showHabitNotification(
                                        context = context,
                                        notificationId = 888001,
                                        title = "DailyHabit Reminder",
                                        body = "Your offline habit reminders are ready to keep your streaks alive!",
                                        channelId = HabitNotificationHelper.CHANNEL_HABIT_REMINDERS
                                    )
                                    scope.launch {
                                        snackbarHostState.showSnackbar(testSentMessage)
                                    }
                                },
                                onRestoreHabit = { habitId -> viewModel.archiveHabit(habitId, false) },
                                onDeleteArchivedHabit = viewModel::deleteHabit
                            )
                        }

                        composable(
                            route = Routes.HABIT_FORM_PATTERN,
                            arguments = listOf(
                                navArgument("habitId") {
                                    type = NavType.LongType
                                    defaultValue = -1L
                                }
                            )
                        ) { entry ->
                            val habitIdArg = entry.arguments?.getLong("habitId") ?: -1L
                            val editingHabit = remember(habitIdArg, uiState.activeHabits) {
                                if (habitIdArg > 0L) {
                                    uiState.activeHabits.firstOrNull { it.habit.id == habitIdArg }?.habit
                                } else {
                                    null
                                }
                            }
                            HabitCreationScreen(
                                initialHabit = editingHabit,
                                onNavigateBack = { navController.popBackStack() },
                                onSaveHabit = { habitToSave ->
                                    viewModel.saveHabit(habitToSave)
                                    navController.popBackStack()
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (editingHabit == null) "Habit created!" else "Habit updated!"
                                        )
                                    }
                                },
                                onRequestNotificationPermissionIfNeeded = requestNotificationPermission
                            )
                        }
                    }
                }
            }
        }
    }

    // Habit Detail Bottom Sheet
    val detailItem = detailHabitId?.let { id ->
        uiState.activeHabits.firstOrNull { it.habit.id == id }
    }
    if (detailItem != null) {
        HabitDetailSheet(
            item = detailItem,
            todayEpochDay = uiState.todayEpochDay,
            onDismiss = { detailHabitId = null },
            onToggleDay = { day -> viewModel.toggleHabitCompletion(detailItem.habit.id, day) },
            onEdit = {
                val targetId = detailItem.habit.id
                detailHabitId = null
                navController.navigate(Routes.habitForm(targetId))
            },
            onArchive = {
                viewModel.archiveHabit(detailItem.habit.id, true)
                detailHabitId = null
                scope.launch { snackbarHostState.showSnackbar("Habit archived") }
            },
            onDelete = {
                viewModel.deleteHabit(detailItem.habit.id)
                detailHabitId = null
                scope.launch { snackbarHostState.showSnackbar("Habit deleted") }
            }
        )
    }
}
