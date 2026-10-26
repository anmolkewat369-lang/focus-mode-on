package com.example.data.repository

import com.example.data.db.DailyReviewDao
import com.example.data.db.FocusSessionDao
import com.example.data.db.TaskDao
import com.example.data.model.DailyReviewEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.TaskEntity
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TimeFlowRepository(
    private val taskDao: TaskDao,
    private val focusSessionDao: FocusSessionDao,
    private val dailyReviewDao: DailyReviewDao,
    val userPreferences: UserPreferences
) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val allSessions: Flow<List<FocusSessionEntity>> = focusSessionDao.getAllSessions()
    val allReviews: Flow<List<DailyReviewEntity>> = dailyReviewDao.getAllReviews()

    fun getTasksForDate(date: String): Flow<List<TaskEntity>> = taskDao.getTasksForDate(date)

    fun getTaskById(id: String): Flow<TaskEntity?> = taskDao.getTaskById(id)

    suspend fun insertTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.insertTask(task)
    }

    suspend fun insertTasks(tasks: List<TaskEntity>) = withContext(Dispatchers.IO) {
        taskDao.insertTasks(tasks)
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(id: String) = withContext(Dispatchers.IO) {
        taskDao.deleteTaskById(id)
    }

    suspend fun toggleTaskCompleted(taskId: String) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskByIdOnce(taskId) ?: return@withContext
        val newStatus = !task.isCompleted
        val updated = task.copy(
            isCompleted = newStatus,
            completedAtMillis = if (newStatus) System.currentTimeMillis() else null
        )
        taskDao.updateTask(updated)
    }

    suspend fun recordFocusSession(session: FocusSessionEntity) = withContext(Dispatchers.IO) {
        focusSessionDao.insertSession(session)
        // If session was linked to a task, increment the task's actual minutes
        if (!session.taskId.isNullOrBlank() && session.durationSeconds > 0) {
            val task = taskDao.getTaskByIdOnce(session.taskId)
            if (task != null) {
                val addedMinutes = (session.durationSeconds + 30) / 60
                val updatedTask = task.copy(
                    actualMinutes = task.actualMinutes + addedMinutes
                )
                taskDao.updateTask(updatedTask)
            }
        }
    }

    suspend fun saveDailyReview(review: DailyReviewEntity) = withContext(Dispatchers.IO) {
        dailyReviewDao.insertReview(review)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        taskDao.clearAllTasks()
        focusSessionDao.clearAllSessions()
        dailyReviewDao.clearAllReviews()
        userPreferences.resetPreferences()
    }

    suspend fun seedStarterTasksForUseCase(useCase: String, todayDate: String) = withContext(Dispatchers.IO) {
        val tasks = when (useCase) {
            "Student" -> listOf(
                TaskEntity(
                    title = "Review Machine Learning Lecture Notes",
                    description = "Summarize Key chapters 4 & 5 for midterms",
                    category = "Study",
                    priority = "High",
                    estimatedMinutes = 45,
                    scheduledDate = todayDate,
                    scheduledStartMinute = 9 * 60,
                    scheduledEndMinute = 9 * 60 + 45
                ),
                TaskEntity(
                    title = "Database Systems Lab Assignment",
                    description = "Write SQL queries for homework schema",
                    category = "Study",
                    priority = "Urgent",
                    estimatedMinutes = 60,
                    scheduledDate = todayDate,
                    scheduledStartMinute = 10 * 60,
                    scheduledEndMinute = 11 * 60
                ),
                TaskEntity(
                    title = "CS Department Seminar",
                    description = "Keynote on Distributed Consensus in Room 302",
                    category = "Study",
                    priority = "Medium",
                    estimatedMinutes = 60,
                    scheduledDate = todayDate,
                    scheduledStartMinute = 14 * 60,
                    scheduledEndMinute = 15 * 60,
                    isFixedCommitment = true
                ),
                TaskEntity(
                    title = "Campus Gym Workout",
                    description = "Cardio and stretching",
                    category = "Health",
                    priority = "Medium",
                    estimatedMinutes = 45,
                    scheduledDate = todayDate,
                    scheduledStartMinute = 17 * 60,
                    scheduledEndMinute = 17 * 60 + 45
                )
            )
            "Freelancer" -> listOf(
                TaskEntity(
                    title = "Design System Client Review",
                    description = "Walkthrough Figma wireframes with founder",
                    category = "Work",
                    priority = "High",
                    estimatedMinutes = 45,
                    scheduledDate = todayDate,
                    scheduledStartMinute = 10 * 60,
                    scheduledEndMinute = 10 * 60 + 45,
                    isFixedCommitment = true
                ),
                TaskEntity(
                    title = "Implement Stripe Checkout Integration",
                    description = "Code webhook handlers and payment verification",
                    category = "Work",
                    priority = "Urgent",
                    estimatedMinutes = 90,
                    scheduledDate = todayDate,
                    scheduledStartMinute = 11 * 60,
                    scheduledEndMinute = 12 * 60 + 30
                ),
                TaskEntity(
                    title = "Invoice & Timesheet Reconciliation",
                    description = "Send milestone 2 invoice to client",
                    category = "Work",
                    priority = "Medium",
                    estimatedMinutes = 30,
                    scheduledDate = todayDate,
                    scheduledStartMinute = 15 * 60,
                    scheduledEndMinute = 15 * 60 + 30
                )
            )
            else -> listOf(
                TaskEntity(
                    title = "Team Daily Standup",
                    description = "Sprint sync & blockers overview",
                    category = "Work",
                    priority = "High",
                    estimatedMinutes = 30,
                    scheduledDate = todayDate,
                    scheduledStartMinute = 9 * 60 + 30,
                    scheduledEndMinute = 10 * 60,
                    isFixedCommitment = true
                ),
                TaskEntity(
                    title = "Architecture Refactoring for API V2",
                    description = "Refactor caching layer to optimize p99 latency",
                    category = "Deep Work",
                    priority = "Urgent",
                    estimatedMinutes = 60,
                    scheduledDate = todayDate,
                    scheduledStartMinute = 10 * 60 + 15,
                    scheduledEndMinute = 11 * 60 + 15
                ),
                TaskEntity(
                    title = "Review Pull Requests & RFCs",
                    description = "Provide feedback on schema migrations",
                    category = "Work",
                    priority = "Medium",
                    estimatedMinutes = 45,
                    scheduledDate = todayDate,
                    scheduledStartMinute = 13 * 60 + 30,
                    scheduledEndMinute = 14 * 60 + 15
                ),
                TaskEntity(
                    title = "Mindful Afternoon Walk",
                    description = "Take 20 minutes screen-free fresh air",
                    category = "Health",
                    priority = "Low",
                    estimatedMinutes = 20,
                    scheduledDate = todayDate,
                    scheduledStartMinute = 16 * 60,
                    scheduledEndMinute = 16 * 60 + 20
                )
            )
        }
        taskDao.insertTasks(tasks)
    }

    suspend fun exportDataAsJson(tasks: List<TaskEntity>, sessions: List<FocusSessionEntity>, reviews: List<DailyReviewEntity>): String {
        return withContext(Dispatchers.Default) {
            val root = JSONObject()
            root.put("appName", "TimeFlow")
            root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

            val tasksArray = JSONArray()
            for (t in tasks) {
                val obj = JSONObject()
                obj.put("id", t.id)
                obj.put("title", t.title)
                obj.put("description", t.description)
                obj.put("category", t.category)
                obj.put("priority", t.priority)
                obj.put("estimatedMinutes", t.estimatedMinutes)
                obj.put("actualMinutes", t.actualMinutes)
                obj.put("isCompleted", t.isCompleted)
                obj.put("scheduledDate", t.scheduledDate ?: "")
                obj.put("isFixedCommitment", t.isFixedCommitment)
                tasksArray.put(obj)
            }
            root.put("tasks", tasksArray)

            val sessionsArray = JSONArray()
            for (s in sessions) {
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("taskTitle", s.taskTitle)
                obj.put("durationSeconds", s.durationSeconds)
                obj.put("sessionType", s.sessionType)
                obj.put("date", s.date)
                sessionsArray.put(obj)
            }
            root.put("focusSessions", sessionsArray)

            val reviewsArray = JSONArray()
            for (r in reviews) {
                val obj = JSONObject()
                obj.put("date", r.date)
                obj.put("completedCount", r.completedCount)
                obj.put("plannedMinutes", r.plannedMinutes)
                obj.put("actualMinutes", r.actualMinutes)
                obj.put("suggestion", r.suggestion)
                reviewsArray.put(obj)
            }
            root.put("dailyReviews", reviewsArray)

            root.toString(2)
        }
    }
}
