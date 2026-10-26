package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.preferences.UserSettings
import com.example.domain.SmartPlanner
import com.example.ui.theme.FlowRose

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    userSettings: UserSettings,
    onUpdateTheme: (Boolean) -> Unit,
    onUpdatePlanningPreferences: (startMin: Int, endMin: Int, focusMin: Int, breakMin: Int, bufferMin: Int) -> Unit,
    onAddCategory: (String) -> Unit,
    onExportData: () -> Unit,
    onResetData: () -> Unit
) {
    var dayStartHour by remember(userSettings.dayStartMinute) {
        mutableFloatStateOf((userSettings.dayStartMinute / 60).toFloat())
    }
    var dayEndHour by remember(userSettings.dayEndMinute) {
        mutableFloatStateOf((userSettings.dayEndMinute / 60).toFloat())
    }
    var focusDuration by remember(userSettings.preferredFocusMinutes) {
        mutableIntStateOf(userSettings.preferredFocusMinutes)
    }
    var breakDuration by remember(userSettings.preferredBreakMinutes) {
        mutableIntStateOf(userSettings.preferredBreakMinutes)
    }
    var bufferMinutes by remember(userSettings.bufferMinutes) {
        mutableIntStateOf(userSettings.bufferMinutes)
    }

    var newCategoryText by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        // 1. Appearance / Theme
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (!userSettings.isDarkTheme) Icons.Default.Brightness4 else Icons.Default.Brightness4,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (!userSettings.isDarkTheme) "Light Mode (Active)" else "Dark Mode (Active)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (!userSettings.isDarkTheme) "Calm, clean light aesthetic" else "Soft, deep-slate dark palette",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = !userSettings.isDarkTheme,
                        onCheckedChange = { isLight -> onUpdateTheme(!isLight) },
                        modifier = Modifier.testTag("light_mode_switch")
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !userSettings.isDarkTheme,
                        onClick = { onUpdateTheme(false) },
                        label = { Text("Light Mode") },
                        modifier = Modifier.weight(1f).testTag("theme_light_chip")
                    )
                    FilterChip(
                        selected = userSettings.isDarkTheme,
                        onClick = { onUpdateTheme(true) },
                        label = { Text("Dark Mode") },
                        modifier = Modifier.weight(1f).testTag("theme_dark_chip")
                    )
                }
            }
        }

        // 2. Planning Window & Daily Capacity
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Daily Schedule Window", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Day Starts: ${SmartPlanner.formatTimeMinute(dayStartHour.toInt() * 60)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text("Day Ends: ${SmartPlanner.formatTimeMinute(dayEndHour.toInt() * 60)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }

                Slider(
                    value = dayStartHour,
                    onValueChange = {
                        dayStartHour = it
                        onUpdatePlanningPreferences(
                            dayStartHour.toInt() * 60,
                            dayEndHour.toInt() * 60,
                            focusDuration,
                            breakDuration,
                            bufferMinutes
                        )
                    },
                    valueRange = 5f..12f,
                    steps = 6
                )

                Slider(
                    value = dayEndHour,
                    onValueChange = {
                        dayEndHour = it
                        onUpdatePlanningPreferences(
                            dayStartHour.toInt() * 60,
                            dayEndHour.toInt() * 60,
                            focusDuration,
                            breakDuration,
                            bufferMinutes
                        )
                    },
                    valueRange = 14f..23f,
                    steps = 8
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Focus & Break durations
                Text("Preferred Focus Duration: ${focusDuration}m", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(20, 25, 45, 50).forEach { mins ->
                        FilterChip(
                            selected = focusDuration == mins,
                            onClick = {
                                focusDuration = mins
                                onUpdatePlanningPreferences(
                                    dayStartHour.toInt() * 60,
                                    dayEndHour.toInt() * 60,
                                    focusDuration,
                                    breakDuration,
                                    bufferMinutes
                                )
                            },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Preferred Rest Break: ${breakDuration}m", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15).forEach { mins ->
                        FilterChip(
                            selected = breakDuration == mins,
                            onClick = {
                                breakDuration = mins
                                onUpdatePlanningPreferences(
                                    dayStartHour.toInt() * 60,
                                    dayEndHour.toInt() * 60,
                                    focusDuration,
                                    breakDuration,
                                    bufferMinutes
                                )
                            },
                            label = { Text("${mins}m") }
                        )
                    }
                }
            }
        }

        // 3. Category Management
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Task Categories", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    userSettings.categories.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryText,
                        onValueChange = { newCategoryText = it },
                        placeholder = { Text("Add custom category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCategoryText.isNotBlank()) {
                                onAddCategory(newCategoryText)
                                newCategoryText = ""
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Category", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // 4. Data Management & Export
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Data & Privacy", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "TimeFlow is strictly local-first. All your tasks, schedules, and time tracking sessions are stored securely on this device without third-party tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onExportData,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("export_data_btn")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export All Data as JSON")
                }

                Button(
                    onClick = { showResetConfirmDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowRose.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth().testTag("reset_data_btn")
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = FlowRose, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset All TimeFlow Data", color = FlowRose)
                }
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset all data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all tasks, tracked focus sessions, daily reviews, and preferences on this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlowRose)
                ) {
                    Text("Yes, Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
