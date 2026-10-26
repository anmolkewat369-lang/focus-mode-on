package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyReviewEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.TaskEntity
import com.example.domain.SmartPlanner
import com.example.ui.theme.FlowAmber
import com.example.ui.theme.FlowCyan
import com.example.ui.theme.FlowEmerald
import com.example.ui.theme.FlowPurple
import com.example.ui.theme.FlowRose
import com.example.util.DateUtils
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    tasks: List<TaskEntity>,
    sessions: List<FocusSessionEntity>,
    reviews: List<DailyReviewEntity>,
    onOpenDailyReview: () -> Unit
) {
    val totalActualMinutes = remember(sessions) {
        sessions.filter { it.sessionType == "FOCUS" }.sumOf { it.durationSeconds } / 60
    }

    val totalCompletedTasks = remember(tasks) { tasks.count { it.isCompleted } }

    // Estimate vs Actual accuracy analysis
    val tasksWithBoth = remember(tasks) {
        tasks.filter { it.isCompleted && it.actualMinutes > 0 }
    }
    val overrunTasks = tasksWithBoth.filter { it.actualMinutes > it.estimatedMinutes }
    val onTrackTasks = tasksWithBoth.filter { it.actualMinutes <= it.estimatedMinutes }

    val avgAccuracyPercent = remember(tasksWithBoth) {
        if (tasksWithBoth.isNotEmpty()) {
            val ratioSum = tasksWithBoth.sumOf {
                (it.estimatedMinutes.toDouble() / it.actualMinutes.toDouble()).coerceIn(0.2, 2.0)
            }
            ((ratioSum / tasksWithBoth.size) * 100).roundToInt()
        } else {
            100
        }
    }

    // Weekly day counts
    val weekDays = remember { DateUtils.getWeekDays() }
    val maxDayCount = remember(tasks, weekDays) {
        weekDays.maxOfOrNull { day ->
            tasks.count { it.completedAtMillis != null && it.scheduledDate == day.dateIso }
        }?.coerceAtLeast(1) ?: 1
    }

    // Category distribution
    val categoryMinutes = remember(tasks) {
        tasks.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.actualMinutes.coerceAtLeast(it.estimatedMinutes) } }
            .toList()
            .sortedByDescending { it.second }
    }
    val totalCategoryMinutes = categoryMinutes.sumOf { it.second }.coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Time Insights",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Learn from actual time to plan better tomorrow",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onOpenDailyReview,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("insights_daily_review_btn")
                ) {
                    Icon(imageVector = Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Review")
                }
            }
        }

        // Top Metric Grid Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Tracked Time",
                    value = SmartPlanner.formatMinutes(totalActualMinutes),
                    subtitle = "${sessions.size} sessions",
                    icon = Icons.Default.HourglassTop,
                    tint = FlowCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Completed",
                    value = "$totalCompletedTasks",
                    subtitle = "tasks done",
                    icon = Icons.Default.CheckCircle,
                    tint = FlowEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Planning Accuracy",
                    value = "$avgAccuracyPercent%",
                    subtitle = if (avgAccuracyPercent >= 85) "Highly realistic" else "Tends to overrun",
                    icon = Icons.Default.TrendingUp,
                    tint = if (avgAccuracyPercent >= 85) FlowEmerald else FlowAmber,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Deep Focus",
                    value = "${sessions.count { it.sessionType == "FOCUS" }}",
                    subtitle = "focused intervals",
                    icon = Icons.Default.Psychology,
                    tint = FlowPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Weekly Tasks Completed Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weekly Completion Rhythm",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weekDays.forEach { day ->
                            val count = tasks.count { it.isCompleted && it.scheduledDate == day.dateIso }
                            val heightFraction = (count.toFloat() / maxDayCount.toFloat()).coerceIn(0.1f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (count > 0) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height((80 * heightFraction).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (day.isToday) MaterialTheme.colorScheme.primary
                                            else if (count > 0) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day.dayName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Estimation vs Actual Time Comparison
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Estimate Realism",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (overrunTasks.isNotEmpty()) {
                            "${overrunTasks.size} tasks took longer than expected. Adjust estimates upwards by 15-20% when planning."
                        } else {
                            "Your time estimates are well-calibrated and realistic!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("On Track / Under", style = MaterialTheme.typography.bodySmall)
                        Text("${onTrackTasks.size} tasks", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = FlowEmerald)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (tasksWithBoth.isNotEmpty()) (onTrackTasks.size.toFloat() / tasksWithBoth.size.toFloat()) else 1f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = FlowEmerald,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Exceeded Estimate", style = MaterialTheme.typography.bodySmall)
                        Text("${overrunTasks.size} tasks", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = FlowAmber)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (tasksWithBoth.isNotEmpty()) (overrunTasks.size.toFloat() / tasksWithBoth.size.toFloat()) else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = FlowAmber,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Category Time Distribution
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Category Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (categoryMinutes.isEmpty()) {
                        Text("No tracked time by category yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        categoryMinutes.take(5).forEach { (category, mins) ->
                            val pct = (mins.toFloat() / totalCategoryMinutes.toFloat())
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = category, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                    Text(
                                        text = "${SmartPlanner.formatMinutes(mins)} (${(pct * 100).toInt()}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Historical Reflections Section
        if (reviews.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Daily Reflections",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(reviews.take(3), key = { it.date }) { rev ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = DateUtils.formatShortDisplayDate(rev.date),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${rev.completedCount} done • ${SmartPlanner.formatMinutes(rev.actualMinutes)} tracked",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (rev.suggestion.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 ${rev.suggestion}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = tint
            )
        }
    }
}
