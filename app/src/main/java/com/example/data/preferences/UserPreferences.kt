package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettings(
    val primaryUseCase: String = "Professional",
    val dayStartMinute: Int = 8 * 60, // 08:00 AM
    val dayEndMinute: Int = 18 * 60,  // 06:00 PM
    val preferredFocusMinutes: Int = 25,
    val preferredBreakMinutes: Int = 5,
    val bufferMinutes: Int = 10,
    val isDarkTheme: Boolean = false, // false = Light Mode (default)
    val onboardingCompleted: Boolean = false,
    val categories: List<String> = listOf("Work", "Study", "Personal", "Health", "Deep Work"),
    val hapticsEnabled: Boolean = true
)

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("timeflow_preferences", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): UserSettings {
        val useCase = prefs.getString("use_case", "Professional") ?: "Professional"
        val startMin = prefs.getInt("day_start_min", 8 * 60)
        val endMin = prefs.getInt("day_end_min", 18 * 60)
        val focusMin = prefs.getInt("focus_min", 25)
        val breakMin = prefs.getInt("break_min", 5)
        val bufferMin = prefs.getInt("buffer_min", 10)
        val isDark = prefs.getBoolean("is_dark_theme", false)
        val onboarding = prefs.getBoolean("onboarding_completed", false)
        val categoriesStr = prefs.getString("categories", "Work,Study,Personal,Health,Deep Work")
            ?: "Work,Study,Personal,Health,Deep Work"
        val categories = categoriesStr.split(",").filter { it.isNotBlank() }
        val haptics = prefs.getBoolean("haptics_enabled", true)

        return UserSettings(
            primaryUseCase = useCase,
            dayStartMinute = startMin,
            dayEndMinute = endMin,
            preferredFocusMinutes = focusMin,
            preferredBreakMinutes = breakMin,
            bufferMinutes = bufferMin,
            isDarkTheme = isDark,
            onboardingCompleted = onboarding,
            categories = categories,
            hapticsEnabled = haptics
        )
    }

    fun updateSettings(update: (UserSettings) -> UserSettings) {
        val current = _settingsFlow.value
        val updated = update(current)

        prefs.edit().apply {
            putString("use_case", updated.primaryUseCase)
            putInt("day_start_min", updated.dayStartMinute)
            putInt("day_end_min", updated.dayEndMinute)
            putInt("focus_min", updated.preferredFocusMinutes)
            putInt("break_min", updated.preferredBreakMinutes)
            putInt("buffer_min", updated.bufferMinutes)
            putBoolean("is_dark_theme", updated.isDarkTheme)
            putBoolean("onboarding_completed", updated.onboardingCompleted)
            putString("categories", updated.categories.joinToString(","))
            putBoolean("haptics_enabled", updated.hapticsEnabled)
            apply()
        }

        _settingsFlow.value = updated
    }

    fun resetPreferences() {
        prefs.edit().clear().apply()
        _settingsFlow.value = loadSettings()
    }
}
