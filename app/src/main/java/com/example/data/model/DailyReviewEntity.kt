package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_reviews")
data class DailyReviewEntity(
    @PrimaryKey
    val date: String, // YYYY-MM-DD
    val completedCount: Int = 0,
    val unfinishedCount: Int = 0,
    val plannedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val wentWell: String = "",
    val interruptedBy: String = "",
    val changeTomorrow: String = "",
    val suggestion: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)
