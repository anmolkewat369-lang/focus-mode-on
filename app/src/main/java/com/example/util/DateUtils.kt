package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    private val shortDisplayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val dayNumFormat = SimpleDateFormat("d", Locale.getDefault())

    fun getTodayDate(): String = isoFormat.format(Date())

    fun getTomorrowDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return isoFormat.format(cal.time)
    }

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val d = isoFormat.parse(dateStr) ?: return dateStr
            displayFormat.format(d)
        } catch (_: Exception) {
            dateStr
        }
    }

    fun formatShortDisplayDate(dateStr: String): String {
        return try {
            val d = isoFormat.parse(dateStr) ?: return dateStr
            shortDisplayFormat.format(d)
        } catch (_: Exception) {
            dateStr
        }
    }

    fun getWeekDays(centerDateStr: String = getTodayDate()): List<WeekDayInfo> {
        val cal = Calendar.getInstance()
        try {
            val d = isoFormat.parse(centerDateStr)
            if (d != null) cal.time = d
        } catch (_: Exception) {}

        // Go to start of week (Monday)
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val list = mutableListOf<WeekDayInfo>()
        val today = getTodayDate()
        for (i in 0 until 7) {
            val iso = isoFormat.format(cal.time)
            list.add(
                WeekDayInfo(
                    dateIso = iso,
                    dayName = dayNameFormat.format(cal.time),
                    dayNumber = dayNumFormat.format(cal.time),
                    isToday = iso == today,
                    isSelected = iso == centerDateStr
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    fun formatSecondsToMinutesSeconds(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    fun getGreeting(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String {
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }
}

data class WeekDayInfo(
    val dateIso: String,
    val dayName: String,
    val dayNumber: String,
    val isToday: Boolean,
    val isSelected: Boolean
)
