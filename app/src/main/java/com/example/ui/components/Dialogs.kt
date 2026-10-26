package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import com.example.domain.PlanResult
import com.example.domain.RecoveryProposal
import com.example.domain.SmartPlanner
import com.example.ui.theme.FlowAmber
import com.example.ui.theme.FlowCyan
import com.example.ui.theme.FlowEmerald
import com.example.ui.theme.FlowRose

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateEditTaskDialog(
    initialTask: TaskEntity?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var category by remember { mutableStateOf(initialTask?.category ?: categories.firstOrNull() ?: "Work") }
    var priority by remember { mutableStateOf(initialTask?.priority ?: "Medium") }
    var estimatedMinutes by remember { mutableIntStateOf(initialTask?.estimatedMinutes ?: 30) }
    var isFixedCommitment by remember { mutableStateOf(initialTask?.isFixedCommitment ?: false) }
    var fixedStartHour by remember { mutableIntStateOf((initialTask?.scheduledStartMinute ?: 540) / 60) }
    var fixedStartMinute by remember { mutableIntStateOf((initialTask?.scheduledStartMinute ?: 540) % 60) }

    val priorities = listOf("Low", "Medium", "High", "Urgent")
    val durationPresets = listOf(15, 25, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTask != null) "Edit Task" else "Create New Task",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g., Prepare quarterly budget review") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Sub-steps (optional)") },
                    placeholder = { Text("Context, links, or specific checklist items") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_desc_input")
                )

                // Priority Selection
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    priorities.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) },
                            modifier = Modifier.testTag("priority_chip_$p")
                        )
                    }
                }

                // Estimated Duration Selection
                Text(
                    text = "Estimated Time: ${estimatedMinutes}m",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    durationPresets.forEach { mins ->
                        FilterChip(
                            selected = estimatedMinutes == mins,
                            onClick = { estimatedMinutes = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                // Category Selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                // Fixed Commitment Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fixed Commitment",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Meeting or event that cannot be moved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isFixedCommitment,
                        onCheckedChange = { isFixedCommitment = it },
                        modifier = Modifier.testTag("fixed_commitment_switch")
                    )
                }

                if (isFixedCommitment) {
                    val startMin = fixedStartHour * 60 + fixedStartMinute
                    val endMin = startMin + estimatedMinutes
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Start Time: ${SmartPlanner.formatTimeMinute(startMin)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Ends: ${SmartPlanner.formatTimeMinute(endMin)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (fixedStartHour > 6) fixedStartHour--
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Text("-1h", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Button(
                                    onClick = {
                                        if (fixedStartHour < 22) fixedStartHour++
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Text("+1h", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startMinute = if (isFixedCommitment) fixedStartHour * 60 + fixedStartMinute else initialTask?.scheduledStartMinute
                    val endMinute = if (isFixedCommitment) (startMinute ?: 0) + estimatedMinutes else initialTask?.scheduledEndMinute
                    onSave(
                        initialTask?.id,
                        title,
                        description,
                        category,
                        priority,
                        estimatedMinutes,
                        initialTask?.deadlineMillis,
                        initialTask?.scheduledDate,
                        startMinute,
                        endMinute,
                        isFixedCommitment,
                        initialTask?.isRecurring ?: false
                    )
                },
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SmartPlannerDialog(
    planResult: PlanResult?,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    if (planResult == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Smart Daily Schedule", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Explanation card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (planResult.isOverloaded) FlowAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (planResult.isOverloaded) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = FlowAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (planResult.isOverloaded) "Overload Warning" else "Balanced Plan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (planResult.isOverloaded) FlowAmber else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = planResult.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Workload: ${SmartPlanner.formatMinutes(planResult.plannedWorkloadMinutes)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Free Buffer: ${SmartPlanner.formatMinutes(planResult.remainingAvailableMinutes)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                HorizontalDivider()

                Text(
                    text = "Generated Timeline:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                planResult.scheduledTasks.forEach { task ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1
                                )
                                Text(
                                    text = "${task.priority} • ${task.category}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (task.scheduledStartMinute != null && task.scheduledEndMinute != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${SmartPlanner.formatTimeMinute(task.scheduledStartMinute)} - ${SmartPlanner.formatTimeMinute(task.scheduledEndMinute)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (planResult.unscheduledTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Unscheduled Tasks (Didn't fit today):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = FlowRose)
                    )
                    planResult.unscheduledTasks.forEach { task ->
                        Text(
                            text = "• ${task.title} (${task.estimatedMinutes}m)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                modifier = Modifier.testTag("apply_smart_plan_button")
            ) {
                Text("Apply Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RecoveryPlannerDialog(
    recoveryProposal: RecoveryProposal?,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    if (recoveryProposal == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = FlowCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Schedule Recovery", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = FlowCyan.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Gentle Adjustment",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = FlowCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = recoveryProposal.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (recoveryProposal.tasksPushedBack.isNotEmpty()) {
                    Text(
                        text = "Realigned Tasks (Starting Now):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    recoveryProposal.tasksPushedBack.forEach { task ->
                        Text(
                            text = "• ${task.title}: moved to ${SmartPlanner.formatTimeMinute(task.scheduledStartMinute ?: 0)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (recoveryProposal.tasksMovedToTomorrow.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Deferred to Tomorrow (To Prevent Burnout):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = FlowAmber)
                    )
                    recoveryProposal.tasksMovedToTomorrow.forEach { task ->
                        Text(
                            text = "• ${task.title} (${task.priority} priority)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                modifier = Modifier.testTag("apply_recovery_button")
            ) {
                Text("Apply Recovery")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Schedule")
            }
        }
    )
}

@Composable
fun DailyReviewDialog(
    completedCount: Int,
    unfinishedCount: Int,
    plannedMinutes: Int,
    actualMinutes: Int,
    onDismiss: () -> Unit,
    onSubmit: (wentWell: String, interruptedBy: String, changeTomorrow: String) -> Unit
) {
    var wentWell by remember { mutableStateOf("") }
    var interruptedBy by remember { mutableStateOf("") }
    var changeTomorrow by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Daily Reflection & Review", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today's summary card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Done", style = MaterialTheme.typography.labelSmall)
                            Text("$completedCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Unfinished", style = MaterialTheme.typography.labelSmall)
                            Text("$unfinishedCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Planned", style = MaterialTheme.typography.labelSmall)
                            Text(SmartPlanner.formatMinutes(plannedMinutes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Actual", style = MaterialTheme.typography.labelSmall)
                            Text(SmartPlanner.formatMinutes(actualMinutes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = wentWell,
                    onValueChange = { wentWell = it },
                    label = { Text("What went well today?") },
                    placeholder = { Text("Focused deep session on project deliverable") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = interruptedBy,
                    onValueChange = { interruptedBy = it },
                    label = { Text("What interrupted your flow?") },
                    placeholder = { Text("Unplanned call, context switching") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = changeTomorrow,
                    onValueChange = { changeTomorrow = it },
                    label = { Text("What should change tomorrow?") },
                    placeholder = { Text("Add 15m buffers between meetings") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(wentWell, interruptedBy, changeTomorrow) },
                modifier = Modifier.testTag("save_daily_review_button")
            ) {
                Text("Save Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExportDataDialog(
    jsonContent: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data (JSON)", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Here is your full TimeFlow backup data:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = jsonContent,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("TimeFlow Backup", jsonContent)
                    clipboard.setPrimaryClip(clip)
                    onDismiss()
                }
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy to Clipboard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
