package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.SmartPlanner
import com.example.ui.theme.FlowAmber
import com.example.ui.theme.FlowCyan
import com.example.ui.theme.FlowEmerald
import com.example.ui.theme.FlowRose

@Composable
fun TimeProgressIndicator(
    plannedMinutes: Int,
    completedMinutes: Int,
    availableMinutes: Int,
    completedTaskCount: Int,
    totalTaskCount: Int,
    modifier: Modifier = Modifier
) {
    val completionRatio = if (totalTaskCount > 0) {
        (completedTaskCount.toFloat() / totalTaskCount.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val capacityRatio = if (availableMinutes > 0) {
        (plannedMinutes.toFloat() / availableMinutes.toFloat()).coerceIn(0f, 2f)
    } else 0f

    val isOverloaded = plannedMinutes > availableMinutes && availableMinutes > 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Capacity",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isOverloaded) "Over planned capacity" else "Realistic & balanced",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverloaded) FlowAmber else FlowEmerald
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOverloaded) FlowRose.copy(alpha = 0.15f) else FlowCyan.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${(capacityRatio * 100).toInt()}% load",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isOverloaded) FlowRose else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Capacity bar
            LinearProgressIndicator(
                progress = { (capacityRatio).coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (isOverloaded) FlowAmber else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Planned",
                    value = SmartPlanner.formatMinutes(plannedMinutes)
                )
                MetricItem(
                    label = "Completed",
                    value = "${SmartPlanner.formatMinutes(completedMinutes)} ($completedTaskCount/$totalTaskCount)"
                )
                MetricItem(
                    label = "Available",
                    value = SmartPlanner.formatMinutes(availableMinutes)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
