package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.TimeFlowViewModel
import com.example.ui.components.CreateEditTaskDialog
import com.example.ui.components.DailyReviewDialog
import com.example.ui.components.ExportDataDialog
import com.example.ui.components.RecoveryPlannerDialog
import com.example.ui.components.SmartPlannerDialog
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.TimeFlowTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TimeFlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDark = userSettings.isDarkTheme ?: systemDark

            TimeFlowTheme(darkTheme = useDark) {
                if (!userSettings.onboardingCompleted) {
                    OnboardingScreen(
                        onComplete = { useCase, startMin, endMin, focusMin, breakMin ->
                            viewModel.completeOnboarding(useCase, startMin, endMin, focusMin, breakMin)
                        }
                    )
                } else {
                    TimeFlowApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun TimeFlowApp(viewModel: TimeFlowViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val reviews by viewModel.allReviews.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    val activeFocusTask by viewModel.activeFocusTask.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()

    val showCreateEditDialog by viewModel.showCreateEditDialog.collectAsStateWithLifecycle()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()
    val showSmartPlannerDialog by viewModel.showSmartPlannerDialog.collectAsStateWithLifecycle()
    val planResult by viewModel.planResult.collectAsStateWithLifecycle()
    val showRecoveryDialog by viewModel.showRecoveryDialog.collectAsStateWithLifecycle()
    val recoveryProposal by viewModel.recoveryProposal.collectAsStateWithLifecycle()
    val showDailyReviewDialog by viewModel.showDailyReviewDialog.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var exportJsonData by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Handle Android system back button: return to TODAY if on other screens
    if (currentScreen != AppScreen.TODAY) {
        BackHandler {
            viewModel.navigateTo(AppScreen.TODAY)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                val navItems = listOf(
                    NavItem(AppScreen.TODAY, "Today", Icons.Default.Today),
                    NavItem(AppScreen.TASKS, "Tasks", Icons.Default.Checklist),
                    NavItem(AppScreen.CALENDAR, "Calendar", Icons.Default.CalendarMonth),
                    NavItem(AppScreen.FOCUS, "Focus", Icons.Default.HourglassTop, hasBadge = timerState.isRunning),
                    NavItem(AppScreen.INSIGHTS, "Insights", Icons.Default.BarChart),
                    NavItem(AppScreen.SETTINGS, "Settings", Icons.Default.Settings)
                )

                navItems.forEach { item ->
                    val isSelected = currentScreen == item.screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.navigateTo(item.screen) },
                        icon = {
                            if (item.hasBadge) {
                                BadgedBox(badge = { Badge() }) {
                                    Icon(item.icon, contentDescription = item.label)
                                }
                            } else {
                                Icon(item.icon, contentDescription = item.label)
                            }
                        },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${item.screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    AppScreen.TODAY -> TodayScreen(
                        tasks = tasks,
                        userSettings = userSettings,
                        onStartFocus = { viewModel.startTaskInFocusMode(it) },
                        onToggleCompleted = { viewModel.toggleTaskCompleted(it) },
                        onEditTask = { viewModel.openEditTaskDialog(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onOpenCreateTask = { viewModel.openCreateTaskDialog() },
                        onOpenSmartPlanner = { viewModel.openSmartPlanner() },
                        onOpenRecoveryPlanner = { viewModel.openRecoveryPlanner() },
                        onOpenDailyReview = { viewModel.openDailyReview() }
                    )
                    AppScreen.TASKS -> TasksScreen(
                        tasks = tasks,
                        categories = userSettings.categories,
                        onStartFocus = { viewModel.startTaskInFocusMode(it) },
                        onToggleCompleted = { viewModel.toggleTaskCompleted(it) },
                        onEditTask = { viewModel.openEditTaskDialog(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onOpenCreateTask = { viewModel.openCreateTaskDialog() }
                    )
                    AppScreen.CALENDAR -> CalendarScreen(
                        tasks = tasks,
                        selectedDate = selectedDate,
                        onSelectDate = { viewModel.selectDate(it) },
                        onStartFocus = { viewModel.startTaskInFocusMode(it) },
                        onToggleCompleted = { viewModel.toggleTaskCompleted(it) },
                        onEditTask = { viewModel.openEditTaskDialog(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onOpenCreateTask = { viewModel.openCreateTaskDialog(it) }
                    )
                    AppScreen.FOCUS -> FocusScreen(
                        activeTask = activeFocusTask,
                        timerState = timerState,
                        onStartTimer = { viewModel.startTimer() },
                        onPauseTimer = { viewModel.pauseTimer() },
                        onResumeTimer = { viewModel.resumeTimer() },
                        onResetTimer = { viewModel.resetTimer() },
                        onAddFiveMinutes = { viewModel.addFiveMinutes() },
                        onCompleteTask = { viewModel.completeTaskFromTimer() },
                        onSwitchToBreak = { viewModel.switchToBreak() },
                        onSwitchToFocus = { viewModel.switchToFocus() },
                        onOpenTaskSelector = { viewModel.navigateTo(AppScreen.TASKS) }
                    )
                    AppScreen.INSIGHTS -> InsightsScreen(
                        tasks = tasks,
                        sessions = sessions,
                        reviews = reviews,
                        onOpenDailyReview = { viewModel.openDailyReview() }
                    )
                    AppScreen.SETTINGS -> SettingsScreen(
                        userSettings = userSettings,
                        onUpdateTheme = { viewModel.updateThemePreference(it) },
                        onUpdatePlanningPreferences = { start, end, focus, brk, buf ->
                            viewModel.updatePlanningPreferences(start, end, focus, brk, buf)
                        },
                        onAddCategory = { viewModel.addCustomCategory(it) },
                        onExportData = {
                            viewModel.exportDataJson { json ->
                                exportJsonData = json
                            }
                        },
                        onResetData = { viewModel.resetAllData() }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreateEditDialog) {
        CreateEditTaskDialog(
            initialTask = editingTask,
            categories = userSettings.categories,
            onDismiss = { viewModel.dismissTaskDialog() },
            onSave = { id, title, desc, cat, prio, est, dead, date, sStart, sEnd, isFixed, isRec ->
                viewModel.saveTask(id, title, desc, cat, prio, est, dead, date, sStart, sEnd, isFixed, isRec)
            }
        )
    }

    if (showSmartPlannerDialog) {
        SmartPlannerDialog(
            planResult = planResult,
            onDismiss = { viewModel.dismissSmartPlanner() },
            onApply = { viewModel.applySmartPlan() }
        )
    }

    if (showRecoveryDialog) {
        RecoveryPlannerDialog(
            recoveryProposal = recoveryProposal,
            onDismiss = { viewModel.dismissRecoveryPlanner() },
            onApply = { viewModel.applyRecoveryPlan() }
        )
    }

    if (showDailyReviewDialog) {
        val today = selectedDate
        val todayTasks = tasks.filter { it.scheduledDate == today }
        DailyReviewDialog(
            completedCount = todayTasks.count { it.isCompleted },
            unfinishedCount = todayTasks.count { !it.isCompleted },
            plannedMinutes = todayTasks.sumOf { it.estimatedMinutes },
            actualMinutes = todayTasks.sumOf { it.actualMinutes },
            onDismiss = { viewModel.dismissDailyReview() },
            onSubmit = { well, interrupted, change ->
                viewModel.submitDailyReview(well, interrupted, change)
            }
        )
    }

    exportJsonData?.let { json ->
        ExportDataDialog(
            jsonContent = json,
            onDismiss = { exportJsonData = null }
        )
    }
}

private data class NavItem(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector,
    val hasBadge: Boolean = false
)
