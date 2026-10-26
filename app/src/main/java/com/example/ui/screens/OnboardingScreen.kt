package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.SmartPlanner

@Composable
fun OnboardingScreen(
    onComplete: (useCase: String, dayStartMin: Int, dayEndMin: Int, focusMin: Int, breakMin: Int) -> Unit
) {
    var selectedUseCase by remember { mutableStateOf("Professional") }
    var dayStartHour by remember { mutableFloatStateOf(8f) } // 8 AM
    var dayEndHour by remember { mutableFloatStateOf(18f) }  // 6 PM
    var focusDuration by remember { mutableIntStateOf(25) }
    var breakDuration by remember { mutableIntStateOf(5) }

    val personas = listOf(
        PersonaOption("Professional", "Professional", "Full-time work, meetings & deliverables", Icons.Default.BusinessCenter),
        PersonaOption("Student", "Student", "Lectures, study sessions & assignments", Icons.Default.School),
        PersonaOption("Freelancer", "Freelancer", "Client projects, billable hours & sprints", Icons.Default.LaptopMac),
        PersonaOption("Personal", "Personal", "Habits, health, goals & chores", Icons.Default.Person),
        PersonaOption("Other", "Other", "Custom daily structure & flexible focus", Icons.Default.Star)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Logo & Title
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to TimeFlow",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Plan realistically. Start immediately.\nRecover intelligently when life happens.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Persona selector
            Text(
                text = "1. What is your primary focus?",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                personas.forEach { item ->
                    val isSelected = selectedUseCase == item.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedUseCase = item.id }
                            .testTag("persona_${item.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Day Schedule window
            Text(
                text = "2. Your typical day window",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Starts: ${SmartPlanner.formatTimeMinute(dayStartHour.toInt() * 60)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Ends: ${SmartPlanner.formatTimeMinute(dayEndHour.toInt() * 60)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Day Start Hour", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = dayStartHour,
                        onValueChange = { dayStartHour = it },
                        valueRange = 5f..12f,
                        steps = 6
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Day End Hour", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = dayEndHour,
                        onValueChange = { dayEndHour = it },
                        valueRange = 14f..23f,
                        steps = 8
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Focus & Break duration
            Text(
                text = "3. Focus rhythm",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(20, 25, 45, 50).forEach { mins ->
                    FilterChip(
                        selected = focusDuration == mins,
                        onClick = { focusDuration = mins },
                        label = { Text("${mins}m Focus") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Button(
                onClick = {
                    onComplete(
                        selectedUseCase,
                        dayStartHour.toInt() * 60,
                        dayEndHour.toInt() * 60,
                        focusDuration,
                        breakDuration
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("get_started_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Get Started with TimeFlow", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = {
                    onComplete("Professional", 8 * 60, 18 * 60, 25, 5)
                }
            ) {
                Text("Skip with Recommended Defaults", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class PersonaOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)
