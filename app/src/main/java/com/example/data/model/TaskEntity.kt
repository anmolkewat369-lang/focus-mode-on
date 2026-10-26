package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: String = "Work",
    val priority: String = "Medium", // "Low", "Medium", "High", "Urgent"
    val estimatedMinutes: Int = 30,
    val actualMinutes: Int = 0,
    val deadlineMillis: Long? = null,
    val scheduledDate: String? = null, // YYYY-MM-DD
    val scheduledStartMinute: Int? = null, // Minute from midnight (0..1439)
    val scheduledEndMinute: Int? = null,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val recurringRule: String? = null, // "DAILY", "WEEKDAYS", "WEEKLY"
    val isFixedCommitment: Boolean = false // Fixed meeting/class that cannot be rescheduled
)
