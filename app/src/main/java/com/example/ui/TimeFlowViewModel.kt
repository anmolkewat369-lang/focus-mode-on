package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.TimeFlowDatabase
import com.example.data.model.DailyReviewEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.TaskEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserSettings
import com.example.data.repository.TimeFlowRepository
import com.example.domain.PlanResult
import com.example.domain.RecoveryPlanner
import com.example.domain.RecoveryProposal
import com.example.domain.SmartPlanner
import com.example.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max

enum class AppScreen(val title: String) {
    TODAY("Today"),
    TASKS("Tasks"),
    CALENDAR("Calendar"),
    FOCUS("Focus"),
    INSIGHTS("Insights"),
    SETTINGS("Settings")
}

data class TimerUiState(
    val isRunning: Boolean = false,
    val secondsRemaining: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val sessionType: String = "FOCUS", // "FOCUS" or "BREAK"
    val isSessionComplete: Boolean = false,
    val elapsedTrackedSeconds: Int = 0
)

class TimeFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val db = TimeFlowDatabase.getDatabase(application)
    private val userPreferences = UserPreferences(application)
    val repository = TimeFlowRepository(
        taskDao = db.taskDao(),
        focusSessionDao = db.focusSessionDao(),
        dailyReviewDao = db.dailyReviewDao(),
        userPreferences = userPreferences
    )

    val userSettings: StateFlow<UserSettings> = userPreferences.settingsFlow

    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<FocusSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReviews: StateFlow<List<DailyReviewEntity>> = repository.allReviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow(AppScreen.TODAY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedDate = MutableStateFlow(DateUtils.getTodayDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Tasks for the selected date
    val selectedDateTasks: StateFlow<List<TaskEntity>> = combine(allTasks, _selectedDate) { tasks, date ->
        tasks.filter { it.scheduledDate == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active task for Focus / Start Now mode
    private val _activeFocusTask = MutableStateFlow<TaskEntity?>(null)
    val activeFocusTask: StateFlow<TaskEntity?> = _activeFocusTask.asStateFlow()

    // Timer State
    private val _timerState = MutableStateFlow(
        TimerUiState(
            secondsRemaining = 25 * 60,
            totalSeconds = 25 * 60
        )
    )
    val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()
    private var timerJob: Job? = null
    private var sessionStartTimeMillis: Long = 0L

    // Dialog & Flow States
    private val _showCreateEditDialog = MutableStateFlow(false)
    val showCreateEditDialog: StateFlow<Boolean> = _showCreateEditDialog.asStateFlow()

    private val _editingTask = MutableStateFlow<TaskEntity?>(null)
    val editingTask: StateFlow<TaskEntity?> = _editingTask.asStateFlow()

    private val _showSmartPlannerDialog = MutableStateFlow(false)
    val showSmartPlannerDialog: StateFlow<Boolean> = _showSmartPlannerDialog.asStateFlow()

    private val _planResult = MutableStateFlow<PlanResult?>(null)
    val planResult: StateFlow<PlanResult?> = _planResult.asStateFlow()

    private val _showRecoveryDialog = MutableStateFlow(false)
    val showRecoveryDialog: StateFlow<Boolean> = _showRecoveryDialog.asStateFlow()

    private val _recoveryProposal = MutableStateFlow<RecoveryProposal?>(null)
    val recoveryProposal: StateFlow<RecoveryProposal?> = _recoveryProposal.asStateFlow()

    private val _showDailyReviewDialog = MutableStateFlow(false)
    val showDailyReviewDialog: StateFlow<Boolean> = _showDailyReviewDialog.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        // Initialize timer from user preference
        val focusMin = userSettings.value.preferredFocusMinutes
        _timerState.value = _timerState.value.copy(
            secondsRemaining = focusMin * 60,
            totalSeconds = focusMin * 60
        )

        // If user already completed onboarding and has no tasks, seed starter tasks
        viewModelScope.launch {
            allTasks.collect { tasks ->
                if (tasks.isEmpty() && userSettings.value.onboardingCompleted) {
                    repository.seedStarterTasksForUseCase(
                        userSettings.value.primaryUseCase,
                        DateUtils.getTodayDate()
                    )
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectDate(dateIso: String) {
        _selectedDate.value = dateIso
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // ================== Task Management CRUD ==================

    fun openCreateTaskDialog(prefilledDate: String? = null) {
        _editingTask.value = null
        _showCreateEditDialog.value = true
    }

    fun openEditTaskDialog(task: TaskEntity) {
        _editingTask.value = task
        _showCreateEditDialog.value = true
    }

    fun dismissTaskDialog() {
        _showCreateEditDialog.value = false
        _editingTask.value = null
    }

    fun saveTask(
        id: String?,
        title: String,
        description: String,
        category: String,
        priority: String,
        estimatedMinutes: Int,
        deadlineMillis: Long?,
        scheduledDate: String?,
        scheduledStartMinute: Int?,
        scheduledEndMinute: Int?,
        isFixedCommitment: Boolean,
        isRecurring: Boolean
    ) {
        if (title.isBlank()) {
            showSnackbar("Task title cannot be empty")
            return
        }

        viewModelScope.launch {
            if (id != null) {
                val existing = allTasks.value.find { it.id == id }
                val updated = existing?.copy(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority,
                    estimatedMinutes = max(5, estimatedMinutes),
                    deadlineMillis = deadlineMillis,
                    scheduledDate = scheduledDate,
                    scheduledStartMinute = scheduledStartMinute,
                    scheduledEndMinute = scheduledEndMinute,
                    isFixedCommitment = isFixedCommitment,
                    isRecurring = isRecurring
                ) ?: TaskEntity(
                    id = id,
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority,
                    estimatedMinutes = max(5, estimatedMinutes),
                    deadlineMillis = deadlineMillis,
                    scheduledDate = scheduledDate,
                    scheduledStartMinute = scheduledStartMinute,
                    scheduledEndMinute = scheduledEndMinute,
                    isFixedCommitment = isFixedCommitment,
                    isRecurring = isRecurring
                )
                repository.updateTask(updated)
                showSnackbar("Task updated")
            } else {
                val newTask = TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority,
                    estimatedMinutes = max(5, estimatedMinutes),
                    deadlineMillis = deadlineMillis,
                    scheduledDate = scheduledDate ?: _selectedDate.value,
                    scheduledStartMinute = scheduledStartMinute,
                    scheduledEndMinute = scheduledEndMinute,
                    isFixedCommitment = isFixedCommitment,
                    isRecurring = isRecurring
                )
                repository.insertTask(newTask)
                showSnackbar("Task created")
            }
            dismissTaskDialog()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            if (_activeFocusTask.value?.id == taskId) {
                _activeFocusTask.value = null
            }
            showSnackbar("Task deleted")
        }
    }

    fun toggleTaskCompleted(taskId: String) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(taskId)
            vibrateGentle()
        }
    }

    fun startTaskInFocusMode(task: TaskEntity) {
        _activeFocusTask.value = task
        val focusSeconds = max(5 * 60, task.estimatedMinutes * 60)
        _timerState.value = TimerUiState(
            isRunning = false,
            secondsRemaining = focusSeconds,
            totalSeconds = focusSeconds,
            sessionType = "FOCUS",
            isSessionComplete = false,
            elapsedTrackedSeconds = 0
        )
        navigateTo(AppScreen.FOCUS)
    }

    // ================== Timer & Focus Session Logic ==================

    fun startTimer() {
        if (_timerState.value.isRunning) return
        sessionStartTimeMillis = System.currentTimeMillis()
        _timerState.value = _timerState.value.copy(isRunning = true, isSessionComplete = false)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (_timerState.value.isRunning && _timerState.value.secondsRemaining > 0) {
                delay(1000)
                val now = System.currentTimeMillis()
                val deltaSec = ((now - lastTick) / 1000).toInt().coerceAtLeast(1)
                lastTick = now

                val newRemaining = max(0, _timerState.value.secondsRemaining - deltaSec)
                val newElapsed = _timerState.value.elapsedTrackedSeconds + deltaSec

                _timerState.value = _timerState.value.copy(
                    secondsRemaining = newRemaining,
                    elapsedTrackedSeconds = newElapsed
                )

                if (newRemaining == 0) {
                    onTimerFinished()
                    break
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    fun resumeTimer() {
        startTimer()
    }

    fun resetTimer() {
        timerJob?.cancel()
        val defaultMinutes = if (_timerState.value.sessionType == "FOCUS") {
            _activeFocusTask.value?.estimatedMinutes ?: userSettings.value.preferredFocusMinutes
        } else {
            userSettings.value.preferredBreakMinutes
        }
        val defaultSec = defaultMinutes * 60
        _timerState.value = TimerUiState(
            isRunning = false,
            secondsRemaining = defaultSec,
            totalSeconds = defaultSec,
            sessionType = _timerState.value.sessionType,
            isSessionComplete = false,
            elapsedTrackedSeconds = 0
        )
    }

    fun addFiveMinutes() {
        val additional = 5 * 60
        _timerState.value = _timerState.value.copy(
            secondsRemaining = _timerState.value.secondsRemaining + additional,
            totalSeconds = _timerState.value.totalSeconds + additional,
            isSessionComplete = false
        )
    }

    private fun onTimerFinished() {
        timerJob?.cancel()
        vibrateCompletion()
        _timerState.value = _timerState.value.copy(
            isRunning = false,
            isSessionComplete = true
        )
        // Automatically save completed focus session
        saveCurrentSession()
    }

    fun saveCurrentSession() {
        val elapsed = _timerState.value.elapsedTrackedSeconds
        if (elapsed <= 0) return

        val activeTask = _activeFocusTask.value
        val session = FocusSessionEntity(
            taskId = activeTask?.id,
            taskTitle = activeTask?.title ?: if (_timerState.value.sessionType == "FOCUS") "Focus Session" else "Break",
            startTimeMillis = sessionStartTimeMillis.takeIf { it > 0 } ?: (System.currentTimeMillis() - elapsed * 1000L),
            endTimeMillis = System.currentTimeMillis(),
            durationSeconds = elapsed,
            sessionType = _timerState.value.sessionType,
            date = DateUtils.getTodayDate()
        )

        viewModelScope.launch {
            repository.recordFocusSession(session)
            showSnackbar("Saved ${DateUtils.formatSecondsToMinutesSeconds(elapsed)} to actual time")
        }
    }

    fun completeTaskFromTimer() {
        val task = _activeFocusTask.value ?: return
        saveCurrentSession()
        viewModelScope.launch {
            repository.toggleTaskCompleted(task.id)
            showSnackbar("Task '${task.title}' completed!")
            _activeFocusTask.value = null
            resetTimer()
        }
    }

    fun switchToBreak() {
        timerJob?.cancel()
        val breakMinutes = userSettings.value.preferredBreakMinutes
        val breakSec = breakMinutes * 60
        _timerState.value = TimerUiState(
            isRunning = false,
            secondsRemaining = breakSec,
            totalSeconds = breakSec,
            sessionType = "BREAK",
            isSessionComplete = false,
            elapsedTrackedSeconds = 0
        )
        startTimer()
    }

    fun switchToFocus() {
        timerJob?.cancel()
        val focusMinutes = _activeFocusTask.value?.estimatedMinutes ?: userSettings.value.preferredFocusMinutes
        val focusSec = focusMinutes * 60
        _timerState.value = TimerUiState(
            isRunning = false,
            secondsRemaining = focusSec,
            totalSeconds = focusSec,
            sessionType = "FOCUS",
            isSessionComplete = false,
            elapsedTrackedSeconds = 0
        )
        startTimer()
    }

    // ================== Smart Daily Planner Logic ==================

    fun openSmartPlanner() {
        val today = _selectedDate.value
        val tasksForToday = allTasks.value.filter { it.scheduledDate == today || (it.scheduledDate == null && !it.isCompleted) }
        val settings = userSettings.value

        val plan = SmartPlanner.generateSchedule(
            tasks = tasksForToday,
            dayStartMin = settings.dayStartMinute,
            dayEndMin = settings.dayEndMinute,
            bufferMinutes = settings.bufferMinutes,
            breakDurationMinutes = settings.preferredBreakMinutes
        )
        _planResult.value = plan
        _showSmartPlannerDialog.value = true
    }

    fun dismissSmartPlanner() {
        _showSmartPlannerDialog.value = false
        _planResult.value = null
    }

    fun applySmartPlan() {
        val plan = _planResult.value ?: return
        viewModelScope.launch {
            val allUpdated = mutableListOf<TaskEntity>()
            allUpdated.addAll(plan.scheduledTasks.map { it.copy(scheduledDate = _selectedDate.value) })
            allUpdated.addAll(plan.unscheduledTasks.map { it.copy(scheduledDate = _selectedDate.value) })
            repository.insertTasks(allUpdated)
            showSnackbar("Smart daily schedule applied!")
            dismissSmartPlanner()
        }
    }

    // ================== Recovery Planner Logic ==================

    fun openRecoveryPlanner() {
        val today = _selectedDate.value
        val tasksForToday = allTasks.value.filter { it.scheduledDate == today }
        val settings = userSettings.value
        val proposal = RecoveryPlanner.createRecoveryPlan(
            tasks = tasksForToday,
            dayEndMinute = settings.dayEndMinute,
            tomorrowDate = DateUtils.getTomorrowDate()
        )
        _recoveryProposal.value = proposal
        _showRecoveryDialog.value = true
    }

    fun dismissRecoveryPlanner() {
        _showRecoveryDialog.value = false
        _recoveryProposal.value = null
    }

    fun applyRecoveryPlan() {
        val proposal = _recoveryProposal.value ?: return
        viewModelScope.launch {
            repository.insertTasks(proposal.updatedTasks)
            showSnackbar("Schedule successfully recovered!")
            dismissRecoveryPlanner()
        }
    }

    // ================== Daily Review Logic ==================

    fun openDailyReview() {
        _showDailyReviewDialog.value = true
    }

    fun dismissDailyReview() {
        _showDailyReviewDialog.value = false
    }

    fun submitDailyReview(wentWell: String, interruptedBy: String, changeTomorrow: String) {
        val today = DateUtils.getTodayDate()
        val todayTasks = allTasks.value.filter { it.scheduledDate == today }
        val completed = todayTasks.count { it.isCompleted }
        val unfinished = todayTasks.count { !it.isCompleted }
        val plannedMinutes = todayTasks.sumOf { it.estimatedMinutes }
        val actualMinutes = todayTasks.sumOf { it.actualMinutes }

        val suggestion = when {
            actualMinutes > plannedMinutes * 1.2 ->
                "Tasks took longer than expected (+${actualMinutes - plannedMinutes}m). Tomorrow, increase estimates by 20% and add buffer slots."
            unfinished > completed ->
                "You had more unfinished tasks than completed. Consider limiting tomorrow's schedule to 3 essential high-priority tasks."
            completed > 0 ->
                "Great momentum today! You kept on track. Maintain similar realistic task blocks tomorrow."
            else ->
                "Tomorrow is a fresh canvas. Start with one small 20-minute task to build flow."
        }

        val review = DailyReviewEntity(
            date = today,
            completedCount = completed,
            unfinishedCount = unfinished,
            plannedMinutes = plannedMinutes,
            actualMinutes = actualMinutes,
            wentWell = wentWell.trim(),
            interruptedBy = interruptedBy.trim(),
            changeTomorrow = changeTomorrow.trim(),
            suggestion = suggestion
        )

        viewModelScope.launch {
            repository.saveDailyReview(review)
            showSnackbar("Daily review saved")
            dismissDailyReview()
        }
    }

    // ================== User Settings & Onboarding ==================

    fun completeOnboarding(
        useCase: String,
        dayStartMin: Int,
        dayEndMin: Int,
        focusMin: Int,
        breakMin: Int
    ) {
        userPreferences.updateSettings {
            it.copy(
                primaryUseCase = useCase,
                dayStartMinute = dayStartMin,
                dayEndMinute = dayEndMin,
                preferredFocusMinutes = focusMin,
                preferredBreakMinutes = breakMin,
                onboardingCompleted = true
            )
        }
        viewModelScope.launch {
            repository.seedStarterTasksForUseCase(useCase, DateUtils.getTodayDate())
            showSnackbar("Welcome to TimeFlow! Your realistic day is ready.")
        }
    }

    fun updateThemePreference(isDark: Boolean) {
        userPreferences.updateSettings { it.copy(isDarkTheme = isDark) }
        showSnackbar(if (isDark) "Switched to Dark Mode" else "Switched to Light Mode")
    }

    fun toggleTheme() {
        val newTheme = !userSettings.value.isDarkTheme
        updateThemePreference(newTheme)
    }

    fun updatePlanningPreferences(
        dayStartMin: Int,
        dayEndMin: Int,
        focusMin: Int,
        breakMin: Int,
        bufferMin: Int
    ) {
        userPreferences.updateSettings {
            it.copy(
                dayStartMinute = dayStartMin,
                dayEndMinute = dayEndMin,
                preferredFocusMinutes = focusMin,
                preferredBreakMinutes = breakMin,
                bufferMinutes = bufferMin
            )
        }
        showSnackbar("Settings updated")
    }

    fun addCustomCategory(newCategory: String) {
        val trimmed = newCategory.trim()
        if (trimmed.isNotBlank() && !userSettings.value.categories.contains(trimmed)) {
            userPreferences.updateSettings {
                it.copy(categories = it.categories + trimmed)
            }
            showSnackbar("Category '$trimmed' added")
        }
    }

    fun exportDataJson(onExport: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDataAsJson(
                tasks = allTasks.value,
                sessions = allSessions.value,
                reviews = allReviews.value
            )
            onExport(json)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            timerJob?.cancel()
            _activeFocusTask.value = null
            repository.clearAllData()
            showSnackbar("All TimeFlow data has been reset.")
        }
    }

    // ================== Haptics ==================

    private fun vibrateGentle() {
        if (!userSettings.value.hapticsEnabled) return
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateCompletion() {
        if (!userSettings.value.hapticsEnabled) return
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 150, 100, 250)
                val amplitudes = intArrayOf(0, 200, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(300)
            }
        } catch (_: Exception) {}
    }
}
