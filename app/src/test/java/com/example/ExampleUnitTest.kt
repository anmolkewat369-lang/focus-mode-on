package com.example

import com.example.data.model.TaskEntity
import com.example.domain.RecoveryPlanner
import com.example.domain.SmartPlanner
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testSmartPlanner_preservesFixedCommitments() {
        val fixedMeeting = TaskEntity(
            title = "All Hands Sync",
            isFixedCommitment = true,
            estimatedMinutes = 60,
            scheduledStartMinute = 10 * 60, // 10:00 AM
            scheduledEndMinute = 11 * 60    // 11:00 AM
        )

        val urgentTask = TaskEntity(
            title = "Critical bug fix",
            priority = "Urgent",
            estimatedMinutes = 45
        )

        val plan = SmartPlanner.generateSchedule(
            tasks = listOf(fixedMeeting, urgentTask),
            dayStartMin = 9 * 60,
            dayEndMin = 17 * 60,
            bufferMinutes = 10
        )

        // Fixed meeting must retain exact slot
        val scheduledFixed = plan.scheduledTasks.find { it.isFixedCommitment }
        assertNotNull(scheduledFixed)
        assertEquals(10 * 60, scheduledFixed?.scheduledStartMinute)
        assertEquals(11 * 60, scheduledFixed?.scheduledEndMinute)

        // Urgent task should fit in open morning slot (9:00 - 9:45)
        val scheduledBug = plan.scheduledTasks.find { it.title == "Critical bug fix" }
        assertNotNull(scheduledBug)
        assertEquals(9 * 60, scheduledBug?.scheduledStartMinute)
        assertEquals(9 * 60 + 45, scheduledBug?.scheduledEndMinute)
        assertFalse(plan.isOverloaded)
    }

    @Test
    fun testSmartPlanner_detectsOverloadedSchedule() {
        // Create 10 hours of tasks for an 8 hour day
        val tasks = (1..10).map { i ->
            TaskEntity(
                title = "Task $i",
                priority = if (i <= 3) "High" else "Low",
                estimatedMinutes = 60
            )
        }

        val plan = SmartPlanner.generateSchedule(
            tasks = tasks,
            dayStartMin = 9 * 60,  // 09:00 AM
            dayEndMin = 17 * 60,   // 05:00 PM = 8 hours
            bufferMinutes = 10
        )

        assertTrue("Plan should flag overload", plan.isOverloaded)
        assertTrue("Some tasks must be unscheduled", plan.unscheduledTasks.isNotEmpty())
        assertEquals("Total tasks preserved", 10, plan.scheduledTasks.size + plan.unscheduledTasks.size)
    }

    @Test
    fun testRecoveryPlanner_realignsAfterDelay() {
        val currentMinute = 11 * 60 + 30 // 11:30 AM
        val delayedTask = TaskEntity(
            title = "Sprint Planning",
            priority = "High",
            estimatedMinutes = 45,
            scheduledStartMinute = 10 * 60, // scheduled in past
            scheduledEndMinute = 10 * 60 + 45
        )

        val proposal = RecoveryPlanner.createRecoveryPlan(
            tasks = listOf(delayedTask),
            currentMinuteOfDay = currentMinute,
            dayEndMinute = 18 * 60,
            tomorrowDate = "2026-10-27"
        )

        val recoveredTask = proposal.updatedTasks.find { it.title == "Sprint Planning" }
        assertNotNull(recoveredTask)
        // Should be shifted forward to start at or after currentMinute
        assertTrue(
            "Task should start at or after current time",
            recoveredTask!!.scheduledStartMinute!! >= currentMinute
        )
    }

    @Test
    fun testDateUtils_formatting() {
        val formatted = SmartPlanner.formatTimeMinute(9 * 60 + 5)
        assertEquals("9:05 AM", formatted)

        val formattedPm = SmartPlanner.formatTimeMinute(14 * 60 + 30)
        assertEquals("2:30 PM", formattedPm)

        val durationFmt = SmartPlanner.formatMinutes(135)
        assertEquals("2h 15m", durationFmt)
    }
}
