package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TaskEntity
import com.example.ui.components.TaskCard
import com.example.util.DateUtils

@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    categories: List<String>,
    onStartFocus: (TaskEntity) -> Unit,
    onToggleCompleted: (String) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onDeleteTask: (String) -> Unit,
    onOpenCreateTask: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Scheduled, 2: Backlog, 3: Completed
    var selectedPriority by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("Priority") } // Priority, Duration, Title

    val tabs = listOf("All", "Scheduled", "Backlog", "Done")
    val priorities = listOf("Urgent", "High", "Medium", "Low")

    val todayDate = remember { DateUtils.getTodayDate() }

    val filteredTasks = tasks.filter { task ->
        // Search filter
        val matchesSearch = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true)

        // Tab filter
        val matchesTab = when (selectedTab) {
            1 -> task.scheduledDate != null && !task.isCompleted
            2 -> task.scheduledDate == null && !task.isCompleted
            3 -> task.isCompleted
            else -> true
        }

        // Priority filter
        val matchesPriority = selectedPriority == null || task.priority.equals(selectedPriority, ignoreCase = true)

        // Category filter
        val matchesCategory = selectedCategory == null || task.category.equals(selectedCategory, ignoreCase = true)

        matchesSearch && matchesTab && matchesPriority && matchesCategory
    }.sortedWith { a, b ->
        when (sortBy) {
            "Duration" -> a.estimatedMinutes.compareTo(b.estimatedMinutes)
            "Title" -> a.title.compareTo(b.title)
            else -> {
                val weightA = when (a.priority.lowercase()) { "urgent" -> 4; "high" -> 3; "medium" -> 2; else -> 1 }
                val weightB = when (b.priority.lowercase()) { "urgent" -> 4; "high" -> 3; "medium" -> 2; else -> 1 }
                weightB.compareTo(weightA)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("tasks_search_input"),
                placeholder = { Text("Search tasks...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("tasks_tab_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Horizontal Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority filter chips
                FilterChip(
                    selected = selectedPriority == null,
                    onClick = { selectedPriority = null },
                    label = { Text("All Priorities") }
                )
                priorities.forEach { p ->
                    FilterChip(
                        selected = selectedPriority == p,
                        onClick = { selectedPriority = if (selectedPriority == p) null else p },
                        label = { Text(p) }
                    )
                }

                // Category filter chips
                categories.take(4).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tasks List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredTasks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No matching tasks",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try clearing your filters or create a new task.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleCompleted = { onToggleCompleted(task.id) },
                            onStartFocus = { onStartFocus(task) },
                            onEdit = { onEditTask(task) },
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        }

        // Add Task FAB
        FloatingActionButton(
            onClick = onOpenCreateTask,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
                .testTag("fab_add_task_list")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
        }
    }
}
