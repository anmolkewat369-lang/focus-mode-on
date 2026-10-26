package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskId: String? = null,
    val taskTitle: String = "Focus Session",
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val sessionType: String = "FOCUS", // "FOCUS", "BREAK"
    val date: String // YYYY-MM-DD
)
