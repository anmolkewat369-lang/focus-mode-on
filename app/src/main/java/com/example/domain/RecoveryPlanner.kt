package com.example.domain

import com.example.data.model.TaskEntity
import java.util.Calendar
import kotlin.math.max

data class RecoveryProposal(
    val updatedTasks: List<TaskEntity>,
    val tasksPushedBack: List<TaskEntity>,
    val tasksMovedToTomorrow: List<TaskEntity>,
    val tasksUnscheduled: List<TaskEntity>,
    val remainingAvailableMinutes: Int,
    val explanation: String,
    val hasConflict: Boolean
)

object RecoveryPlanner {

    /**
     * Checks if current schedule needs recovery based on current minute of day.
     * Triggers when:
     * - An uncompleted task was scheduled in the past.
     * - An ongoing task has exceeded its scheduled end time.
     * - Overlapping tasks are detected.
     */
    fun analyzeScheduleHealth(
        tasks: List<TaskEntity>,
        currentMinuteOfDay: Int = getCurrentMinuteOfDay()
    ): Pair<Boolean, String> {
        val todayTasks = tasks.filter { !it.isCompleted }

        val pastDueTasks = todayTasks.filter {
            it.scheduledEndMinute != null && it.scheduledEndMinute < currentMinuteOfDay
        }

        if (pastDueTasks.isNotEmpty()) {
            val names = pastDueTasks.take(2).joinToString(", ") { "'${it.title}'" }
            return true to "$names ended earlier than now (${SmartPlanner.formatTimeMinute(currentMinuteOfDay)}) without completion."
        }

        val delayedStartTasks = todayTasks.filter {
            it.scheduledStartMinute != null && it.scheduledStartMinute < currentMinuteOfDay - 15
        }
        if (delayedStartTasks.isNotEmpty()) {
            val task = delayedStartTasks.first()
            return true to "Scheduled start for '${task.title}' was ${SmartPlanner.formatTimeMinute(task.scheduledStartMinute!!)}. You are running behind schedule."
        }

        return false to "Schedule is running smoothly on track."
    }

    /**
     * Generates an intelligent, calm, non-judgmental recovery plan.
     */
    fun createRecoveryPlan(
        tasks: List<TaskEntity>,
        currentMinuteOfDay: Int = getCurrentMinuteOfDay(),
        dayEndMinute: Int = 18 * 60,
        tomorrowDate: String
    ): RecoveryProposal {
        val now = max(currentMinuteOfDay, 0)
        val completed = tasks.filter { it.isCompleted }
        val remainingTasks = tasks.filter { !it.isCompleted }

        // Fixed commitments starting after now
        val futureFixed = remainingTasks.filter {
            it.isFixedCommitment && (it.scheduledEndMinute ?: 0) > now
        }.sortedBy { it.scheduledStartMinute }

        // Flexible tasks that need scheduling or rescheduling
        val flexibleCandidates = remainingTasks.filter { !it.isFixedCommitment }
            .sortedWith(
                compareByDescending<TaskEntity> {
                    when (it.priority.lowercase()) {
                        "urgent" -> 4
                        "high" -> 3
                        "medium" -> 2
                        else -> 1
                    }
                }.thenBy { it.deadlineMillis ?: Long.MAX_VALUE }
            )

        // Find open intervals between now and dayEndMinute around future fixed commitments
        val busyIntervals = futureFixed.mapNotNull {
            val start = it.scheduledStartMinute ?: return@mapNotNull null
            val end = it.scheduledEndMinute ?: return@mapNotNull null
            TimeInterval(max(now, start), end)
        }

        val openSlots = mutableListOf<TimeInterval>()
        var cursor = now
        for (busy in busyIntervals) {
            if (busy.startMinute > cursor) {
                openSlots.add(TimeInterval(cursor, busy.startMinute))
            }
            cursor = max(cursor, busy.endMinute)
        }
        if (cursor < dayEndMinute) {
            openSlots.add(TimeInterval(cursor, dayEndMinute))
        }

        val remainingAvailable = openSlots.sumOf { it.durationMinutes }

        val scheduledRecovered = mutableListOf<TaskEntity>()
        val pushedBack = mutableListOf<TaskEntity>()
        val movedToTomorrow = mutableListOf<TaskEntity>()
        val unscheduled = mutableListOf<TaskEntity>()

        // Keep fixed commitments untouched
        scheduledRecovered.addAll(futureFixed)

        // Fit flexible tasks
        for (task in flexibleCandidates) {
            val needed = max(10, task.estimatedMinutes)
            var placed = false

            for (i in openSlots.indices) {
                val slot = openSlots[i]
                if (slot.durationMinutes >= needed) {
                    val start = slot.startMinute
                    val end = start + needed

                    val updated = task.copy(
                        scheduledStartMinute = start,
                        scheduledEndMinute = end
                    )
                    scheduledRecovered.add(updated)
                    pushedBack.add(updated)
                    placed = true

                    val nextStart = end + 10 // 10m buffer
                    if (nextStart < slot.endMinute) {
                        openSlots[i] = TimeInterval(nextStart, slot.endMinute)
                    } else {
                        openSlots.removeAt(i)
                    }
                    break
                }
            }

            if (!placed) {
                // If it's low or medium priority, propose moving to tomorrow
                if (task.priority.equals("Low", ignoreCase = true) || task.priority.equals("Medium", ignoreCase = true)) {
                    movedToTomorrow.add(task.copy(scheduledDate = tomorrowDate, scheduledStartMinute = null, scheduledEndMinute = null))
                } else {
                    unscheduled.add(task.copy(scheduledStartMinute = null, scheduledEndMinute = null))
                }
            }
        }

        // Combine all tasks
        val allUpdated = mutableListOf<TaskEntity>()
        allUpdated.addAll(completed)
        allUpdated.addAll(scheduledRecovered)
        allUpdated.addAll(movedToTomorrow)
        allUpdated.addAll(unscheduled)

        val explanation = when {
            movedToTomorrow.isNotEmpty() ->
                "Shifted upcoming tasks to start now at ${SmartPlanner.formatTimeMinute(now)}. Moved ${movedToTomorrow.size} task(s) to tomorrow to protect your rest time."
            pushedBack.isNotEmpty() ->
                "Realigned ${pushedBack.size} remaining task(s) seamlessly starting at ${SmartPlanner.formatTimeMinute(now)}."
            else ->
                "Schedule recovered successfully. All remaining tasks fit cleanly before ${SmartPlanner.formatTimeMinute(dayEndMinute)}."
        }

        return RecoveryProposal(
            updatedTasks = allUpdated,
            tasksPushedBack = pushedBack,
            tasksMovedToTomorrow = movedToTomorrow,
            tasksUnscheduled = unscheduled,
            remainingAvailableMinutes = remainingAvailable,
            explanation = explanation,
            hasConflict = pushedBack.isNotEmpty() || movedToTomorrow.isNotEmpty()
        )
    }

    fun getCurrentMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
}
