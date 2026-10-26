package com.example.domain

import com.example.data.model.TaskEntity
import kotlin.math.max

data class TimeInterval(
    val startMinute: Int,
    val endMinute: Int
) {
    val durationMinutes: Int get() = endMinute - startMinute
}

data class PlanResult(
    val scheduledTasks: List<TaskEntity>,
    val unscheduledTasks: List<TaskEntity>,
    val totalAvailableMinutes: Int,
    val plannedWorkloadMinutes: Int,
    val remainingAvailableMinutes: Int,
    val isOverloaded: Boolean,
    val overloadMinutes: Int,
    val conflictsDetected: List<String>,
    val explanation: String
)

object SmartPlanner {

    /**
     * Priority weight for deterministic sorting:
     * Urgent = 4, High = 3, Medium = 2, Low = 1
     */
    private fun priorityWeight(priority: String): Int = when (priority.lowercase()) {
        "urgent" -> 4
        "high" -> 3
        "medium" -> 2
        else -> 1
    }

    /**
     * Deterministically schedules tasks for a given date within the user's window [dayStartMin, dayEndMin].
     * Fixed commitments are strictly respected and cannot be moved.
     */
    fun generateSchedule(
        tasks: List<TaskEntity>,
        dayStartMin: Int,
        dayEndMin: Int,
        bufferMinutes: Int = 10,
        breakDurationMinutes: Int = 5,
        minWorkBeforeBreak: Int = 90
    ): PlanResult {
        val totalDayMinutes = max(0, dayEndMin - dayStartMin)

        // 1. Separate fixed commitments from flexible tasks
        val fixedCommitments = tasks.filter {
            it.isFixedCommitment && it.scheduledStartMinute != null && it.scheduledEndMinute != null
        }.sortedBy { it.scheduledStartMinute }

        // Find conflicts among fixed commitments
        val conflicts = mutableListOf<String>()
        for (i in 0 until fixedCommitments.size - 1) {
            val curr = fixedCommitments[i]
            val next = fixedCommitments[i + 1]
            if ((curr.scheduledEndMinute ?: 0) > (next.scheduledStartMinute ?: 0)) {
                conflicts.add("Commitment overlap: '${curr.title}' clashes with '${next.title}'")
            }
        }

        // Calculate available free time intervals within [dayStartMin, dayEndMin]
        val busyIntervals = fixedCommitments.map {
            TimeInterval(it.scheduledStartMinute!!, it.scheduledEndMinute!!)
        }.sortedBy { it.startMinute }

        val freeIntervals = mutableListOf<TimeInterval>()
        var cursor = dayStartMin

        for (interval in busyIntervals) {
            val busyStart = max(dayStartMin, interval.startMinute)
            val busyEnd = interval.endMinute
            if (busyStart > cursor) {
                freeIntervals.add(TimeInterval(cursor, busyStart))
            }
            cursor = max(cursor, busyEnd)
        }
        if (cursor < dayEndMin) {
            freeIntervals.add(TimeInterval(cursor, dayEndMin))
        }

        val totalAvailableMinutes = freeIntervals.sumOf { it.durationMinutes }

        // 2. Identify flexible tasks to schedule
        // Flexible tasks are not fixed, and not already completed
        val flexibleTasks = tasks.filter { !it.isFixedCommitment && !it.isCompleted }

        // 3. Sort flexible tasks:
        // Priority desc, then deadline (soonest first), then duration
        val sortedCandidates = flexibleTasks.sortedWith(
            compareByDescending<TaskEntity> { priorityWeight(it.priority) }
                .thenBy { it.deadlineMillis ?: Long.MAX_VALUE }
                .thenBy { it.estimatedMinutes }
        )

        val totalFlexibleWorkload = sortedCandidates.sumOf { it.estimatedMinutes }
        val fixedWorkload = fixedCommitments.sumOf { (it.scheduledEndMinute ?: 0) - (it.scheduledStartMinute ?: 0) }
        val totalWorkload = totalFlexibleWorkload + fixedWorkload

        val scheduledResults = mutableListOf<TaskEntity>()
        scheduledResults.addAll(fixedCommitments) // Keep fixed unchanged

        val unscheduledResults = mutableListOf<TaskEntity>()

        // Mutable copy of free intervals
        val openSlots = freeIntervals.map { TimeInterval(it.startMinute, it.endMinute) }.toMutableList()

        var continuousWorkMinutes = 0

        for (task in sortedCandidates) {
            val neededDuration = max(10, task.estimatedMinutes)
            var placed = false

            for (i in openSlots.indices) {
                val slot = openSlots[i]
                val effectiveBreak = if (continuousWorkMinutes >= minWorkBeforeBreak) breakDurationMinutes else 0
                val totalNeeded = neededDuration + effectiveBreak + bufferMinutes

                if (slot.durationMinutes >= neededDuration) {
                    val startMin = slot.startMinute + effectiveBreak
                    val endMin = startMin + neededDuration

                    if (endMin <= slot.endMinute) {
                        val scheduledTask = task.copy(
                            scheduledStartMinute = startMin,
                            scheduledEndMinute = endMin
                        )
                        scheduledResults.add(scheduledTask)
                        placed = true

                        // Update or replace this slot
                        val newStart = endMin + bufferMinutes
                        if (newStart < slot.endMinute) {
                            openSlots[i] = TimeInterval(newStart, slot.endMinute)
                        } else {
                            openSlots.removeAt(i)
                        }

                        continuousWorkMinutes += neededDuration
                        if (effectiveBreak > 0) {
                            continuousWorkMinutes = 0
                        }
                        break
                    }
                }
            }

            if (!placed) {
                unscheduledResults.add(task.copy(scheduledStartMinute = null, scheduledEndMinute = null))
            }
        }

        // Add already completed tasks that had schedules preserved
        val completedTasks = tasks.filter { it.isCompleted }
        scheduledResults.addAll(completedTasks.filter { !scheduledResults.any { res -> res.id == it.id } })

        val isOverloaded = totalWorkload > totalDayMinutes || unscheduledResults.isNotEmpty()
        val overloadMinutes = max(0, totalWorkload - totalDayMinutes)

        val explanation = when {
            conflicts.isNotEmpty() -> "Conflicting commitments detected. Please check overlapping fixed events."
            isOverloaded -> "Day is packed! Total workload (${formatMinutes(totalWorkload)}) exceeds available day window (${formatMinutes(totalDayMinutes)}) by ${formatMinutes(overloadMinutes)}. ${unscheduledResults.size} lower priority tasks kept unscheduled."
            unscheduledResults.isNotEmpty() -> "${unscheduledResults.size} tasks could not fit into open slots between fixed commitments."
            else -> "Realistic plan generated! All ${scheduledResults.size} planned items fit comfortably with buffers and breaks."
        }

        return PlanResult(
            scheduledTasks = scheduledResults.sortedBy { it.scheduledStartMinute ?: 9999 },
            unscheduledTasks = unscheduledResults,
            totalAvailableMinutes = totalAvailableMinutes,
            plannedWorkloadMinutes = totalWorkload,
            remainingAvailableMinutes = max(0, totalAvailableMinutes - (scheduledResults.filter { !it.isFixedCommitment }.sumOf { it.estimatedMinutes })),
            isOverloaded = isOverloaded,
            overloadMinutes = overloadMinutes,
            conflictsDetected = conflicts,
            explanation = explanation
        )
    }

    fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    fun formatTimeMinute(minuteOfDay: Int): String {
        val normalized = ((minuteOfDay % 1440) + 1440) % 1440
        val h = normalized / 60
        val m = normalized % 60
        val amPm = if (h < 12) "AM" else "PM"
        val displayH = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format("%d:%02d %s", displayH, m, amPm)
    }
}
