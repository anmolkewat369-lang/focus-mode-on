# TimeFlow

TimeFlow is an intelligent, calm, local-first Android time management application built with Kotlin and Jetpack Compose. Unlike basic to-do lists, TimeFlow answers five essential daily questions:

1. **What should I do today?** (Today Dashboard: planned capacity, available day hours, remaining workload, timeline)
2. **What should I do next?** ("Start Next Task" one-tap focus entry)
3. **How much time should I spend?** (Estimated task duration with live countdown and actual time recording)
4. **What should I do when my plan fails?** (Recovery Planner: intelligent, non-judgmental schedule adjustment)
5. **How can I improve my planning tomorrow?** (Daily Review + Insights: comparing estimated vs. actual time)

---

## Architecture & Technology Stack

- **Platform:** Android (minSdk 24, targetSdk 36)
- **Language:** Kotlin 2.0+ (Coroutine, Flow, MVVM architecture)
- **UI Toolkit:** Jetpack Compose with Material Design 3 (M3)
- **Local Persistence:** Room Database (KSP) + SharedPreferences for local-first zero-latency storage
- **Scheduling Engine:** Deterministic constraint solver (`SmartPlanner`)
- **Recovery Engine:** Non-judgmental timeline realignment (`RecoveryPlanner`)
- **Precision Time Tracking:** Timestamp delta calculation resilient to rerenders and configuration changes

---

## Core Features

### 1. Onboarding & Personalization
- Supports personalized profiles: **Professional**, **Student**, **Freelancer**, **Personal**, and **Other**.
- Configurable daily start time (e.g. 08:00 AM) and end time (e.g. 06:00 PM).
- Focus rhythm configuration (default 25m Focus / 5m Break).
- Seeds contextual starter tasks tailored to the selected persona on first launch.

### 2. Today Dashboard
- Personalized greeting and current date display.
- **Daily Capacity Indicator:** Visual bar showing planned workload vs. available day hours and completed tasks.
- **Up Next Hero Card:** Highlights the immediate next scheduled task with a single-tap "Start Task Now" button.
- **Recovery Alert Banner:** Activates automatically when behind schedule to offer instant rebalancing.
- Chronological timeline distinguishing fixed commitments from flexible tasks.

### 3. Smart Daily Planner
- Deterministic schedule-generation algorithm:
  1. Preserves fixed commitments (classes, client calls, standups).
  2. Respects task deadlines and priority (Urgent > High > Medium > Low).
  3. Inserts realistic buffer time and breaks between intense work periods.
  4. Detects and reports overloaded schedules without deleting any tasks.
  5. One-tap schedule application.

### 4. Recovery Planner
- Triggered when tasks run longer than estimated or when running behind schedule.
- Calculates remaining available time window between now and configured day end.
- Realigns remaining tasks to start immediately.
- Proposes moving low-priority tasks to tomorrow to protect evening rest.
- Non-judgmental, encouraging explanations.

### 5. Focus Mode & Actual Time Tracking
- Full-screen focused task execution.
- Circular progress dial with animated progress arc.
- Controls: Start, Pause, Resume, Reset, +5m, Switch to Rest Break.
- Automatically records actual seconds into focus session history and increments task actual tracked time.
- Gentle haptic feedback on completion and task status changes.

### 6. Tasks Repository & Management
- Full CRUD: Create, Edit, Delete, Complete, Reopen.
- Filter by tabs (All, Scheduled, Backlog, Done).
- Filter by priority chips (Urgent, High, Medium, Low) and categories.
- Real-time text search across titles and notes.
- Custom task categories support.

### 7. Calendar View
- Interactive 7-day week carousel with today indicator and task counts.
- Timeline separating fixed commitments (locked slots) and flexible tasks.
- Jump to today shortcut.

### 8. Insights & Daily Review
- **Planning Accuracy:** Percentage ratio of estimated vs. actual time.
- **Weekly Completion Rhythm:** Bar chart showing completed tasks across the week.
- **Category Distribution:** Time breakdown across work, study, health, and custom categories.
- **Daily Review Dialog:** End-of-day reflection capturing what went well, flow interruptions, and tomorrow's adjustments with personalized tips.

### 9. Settings, Theme, & Data Export
- Theme switcher: Light, Dark, or System default.
- Workday hour sliders and focus duration controls.
- Local JSON export to clipboard.
- Safe data reset with confirmation dialog.

---

## Implemented Limitations

- **Local-First Storage:** Stored on-device via Room; cloud synchronization and multi-device sync are deferred by design for the MVP.
- **External Calendar Sync:** Does not sync to external Google Calendar or Outlook APIs in the MVP without user-provided OAuth configuration.
- **Device-Level App Blocking:** Focus mode tracks session time without modifying system-level application lock privileges.

---

## Test Report

All local unit and Robolectric JVM tests were executed via Gradle (`gradle :app:testDebugUnitTest`) with **BUILD SUCCESSFUL**:

1. `testSmartPlanner_preservesFixedCommitments` — PASSED: Verified that fixed commitments strictly preserve their scheduled time intervals and urgent tasks fill open morning slots.
2. `testSmartPlanner_detectsOverloadedSchedule` — PASSED: Verified that the planner detects 10 hours of workload in an 8 hour day, correctly flags overload, and preserves all tasks without silent deletion.
3. `testRecoveryPlanner_realignsAfterDelay` — PASSED: Verified that overdue tasks are shifted to start at or after current time and low priority tasks are cleanly proposed for tomorrow.
4. `testDateUtils_formatting` — PASSED: Verified 12-hour AM/PM formatting and duration string representations.
5. `ExampleRobolectricTest.read string from context` — PASSED: Verified `app_name` string resource resolves to `"TimeFlow"`.
