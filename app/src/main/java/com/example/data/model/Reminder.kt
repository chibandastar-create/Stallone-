package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class CadenceType(val label: String, val subtitle: String) {
    DAILY("Daily", "Every day or today's routine"),
    MONTHLY("Monthly", "Same day each month"),
    YEARLY("Yearly", "Annual dates & milestones")
}

enum class ReminderCategory(val label: String) {
    PERSONAL("Personal"),
    WORK("Work"),
    FINANCE("Finance"),
    HEALTH("Health"),
    HOME("Home"),
    CELEBRATION("Celebration")
}

enum class ReminderPriority(val label: String) {
    NORMAL("Normal"),
    HIGH("High")
}

@Keep
@IgnoreExtraProperties
data class Reminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String = "",
    val cadence: String = CadenceType.DAILY.name,
    val category: String = ReminderCategory.PERSONAL.name,
    val priority: String = ReminderPriority.NORMAL.name,
    val hour: Long = 9L,
    val minute: Long = 0L,
    val dayOfMonth: Long = 1L,
    val monthOfYear: Long = 1L,
    val year: Long = 2026L,
    val recurring: Boolean = true,
    val paused: Boolean = false,
    val completedOnce: Boolean = false,
    val lastCompletedCycleKey: String = "",
    val completedCount: Long = 0L,
    val snoozedUntilEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun cadenceType(): CadenceType =
        enumValues<CadenceType>().firstOrNull { it.name == cadence } ?: CadenceType.DAILY

    fun categoryType(): ReminderCategory =
        enumValues<ReminderCategory>().firstOrNull { it.name == category } ?: ReminderCategory.PERSONAL

    fun priorityType(): ReminderPriority =
        enumValues<ReminderPriority>().firstOrNull { it.name == priority } ?: ReminderPriority.NORMAL

    fun currentCycleKey(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return when (cadenceType()) {
            CadenceType.DAILY -> String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
            CadenceType.MONTHLY -> String.format(Locale.US, "%04d-%02d", y, m)
            CadenceType.YEARLY -> String.format(Locale.US, "%04d", y)
        }
    }

    fun isCompletedForCurrentCycle(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return if (recurring) {
            lastCompletedCycleKey.isNotEmpty() && lastCompletedCycleKey == currentCycleKey(nowMillis)
        } else {
            completedOnce
        }
    }

    fun isSnoozed(nowMillis: Long = System.currentTimeMillis()): Boolean =
        snoozedUntilEpochMs > nowMillis

    fun currentCycleTargetMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val safeHour = hour.toInt().coerceIn(0, 23)
        val safeMinute = minute.toInt().coerceIn(0, 59)
        return when (cadenceType()) {
            CadenceType.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, safeHour)
                cal.set(Calendar.MINUTE, safeMinute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            CadenceType.MONTHLY -> {
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val safeDay = dayOfMonth.toInt().coerceIn(1, maxDay)
                cal.set(Calendar.DAY_OF_MONTH, safeDay)
                cal.set(Calendar.HOUR_OF_DAY, safeHour)
                cal.set(Calendar.MINUTE, safeMinute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            CadenceType.YEARLY -> {
                val targetYear = if (recurring) cal.get(Calendar.YEAR) else year.toInt().coerceIn(2000, 2100)
                val safeMonth = (monthOfYear.toInt() - 1).coerceIn(0, 11)
                cal.set(Calendar.YEAR, targetYear)
                cal.set(Calendar.MONTH, safeMonth)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val safeDay = dayOfMonth.toInt().coerceIn(1, maxDay)
                cal.set(Calendar.DAY_OF_MONTH, safeDay)
                cal.set(Calendar.HOUR_OF_DAY, safeHour)
                cal.set(Calendar.MINUTE, safeMinute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
        }
    }

    fun nextTriggerMillis(nowMillis: Long = System.currentTimeMillis()): Long? {
        if (paused) return null
        if (!recurring && completedOnce) return null
        if (snoozedUntilEpochMs > nowMillis) return snoozedUntilEpochMs

        val safeHour = hour.toInt().coerceIn(0, 23)
        val safeMinute = minute.toInt().coerceIn(0, 59)
        val completedNow = isCompletedForCurrentCycle(nowMillis)
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }

        return when (cadenceType()) {
            CadenceType.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, safeHour)
                cal.set(Calendar.MINUTE, safeMinute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                if (completedNow || cal.timeInMillis <= nowMillis) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                cal.timeInMillis
            }
            CadenceType.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.toInt().coerceIn(1, maxDay))
                cal.set(Calendar.HOUR_OF_DAY, safeHour)
                cal.set(Calendar.MINUTE, safeMinute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                if (completedNow || cal.timeInMillis <= nowMillis) {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.add(Calendar.MONTH, 1)
                    val nextMax = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.toInt().coerceIn(1, nextMax))
                }
                cal.timeInMillis
            }
            CadenceType.YEARLY -> {
                val safeMonth = (monthOfYear.toInt() - 1).coerceIn(0, 11)
                if (!recurring) {
                    cal.set(Calendar.YEAR, year.toInt().coerceIn(2000, 2100))
                    cal.set(Calendar.MONTH, safeMonth)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.toInt().coerceIn(1, maxDay))
                    cal.set(Calendar.HOUR_OF_DAY, safeHour)
                    cal.set(Calendar.MINUTE, safeMinute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    return if (cal.timeInMillis > nowMillis) cal.timeInMillis else null
                }
                cal.set(Calendar.MONTH, safeMonth)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.toInt().coerceIn(1, maxDay))
                cal.set(Calendar.HOUR_OF_DAY, safeHour)
                cal.set(Calendar.MINUTE, safeMinute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                if (completedNow || cal.timeInMillis <= nowMillis) {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.add(Calendar.YEAR, 1)
                    val nextMax = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.toInt().coerceIn(1, nextMax))
                }
                cal.timeInMillis
            }
        }
    }

    fun isDueToday(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (paused || isCompletedForCurrentCycle(nowMillis)) return false
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val targetMillis = if (isSnoozed(nowMillis)) snoozedUntilEpochMs else currentCycleTargetMillis(nowMillis)
        val targetCal = Calendar.getInstance().apply { timeInMillis = targetMillis }
        return nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
    }

    fun isOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (paused || isCompletedForCurrentCycle(nowMillis)) return false
        if (isSnoozed(nowMillis)) return false
        val target = currentCycleTargetMillis(nowMillis)
        return target < nowMillis
    }

    fun formatTime(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.toInt().coerceIn(0, 23))
            set(Calendar.MINUTE, minute.toInt().coerceIn(0, 59))
        }
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
    }

    fun formatScheduleLabel(): String {
        val timeText = formatTime()
        return when (cadenceType()) {
            CadenceType.DAILY -> if (recurring) "Daily · $timeText" else "Today · $timeText"
            CadenceType.MONTHLY -> {
                val ordinal = formatOrdinal(dayOfMonth.toInt().coerceIn(1, 31))
                if (recurring) "Every $ordinal · $timeText" else "This month ($ordinal) · $timeText"
            }
            CadenceType.YEARLY -> {
                val monthName = monthShortName(monthOfYear.toInt().coerceIn(1, 12))
                val day = dayOfMonth.toInt().coerceIn(1, 31)
                if (recurring) "Every $monthName $day · $timeText" else "$monthName $day, $year · $timeText"
            }
        }
    }

    fun formatCountdownLabel(nowMillis: Long = System.currentTimeMillis()): String {
        if (paused) return "Paused"
        if (isCompletedForCurrentCycle(nowMillis)) {
            return when (cadenceType()) {
                CadenceType.DAILY -> if (recurring) "Done today" else "Completed"
                CadenceType.MONTHLY -> if (recurring) "Done this month" else "Completed"
                CadenceType.YEARLY -> if (recurring) "Done this year" else "Completed"
            }
        }
        if (isSnoozed(nowMillis)) {
            val diffMin = max(1L, (snoozedUntilEpochMs - nowMillis) / 60_000L)
            return if (diffMin < 60L) "Snoozed ${diffMin}m"
            else "Snoozed ${diffMin / 60L}h"
        }
        if (isOverdue(nowMillis)) {
            return "Due now"
        }
        val next = nextTriggerMillis(nowMillis) ?: return "Upcoming"
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        val dayDiff = daysBetween(nowCal, nextCal)
        return when {
            dayDiff <= 0 -> {
                val mins = max(1L, (next - nowMillis) / 60_000L)
                if (mins < 60L) "In ${mins}m" else "In ${mins / 60L}h"
            }
            dayDiff == 1 -> "Tomorrow"
            dayDiff < 30 -> "In $dayDiff days"
            dayDiff < 365 -> {
                val months = max(1, dayDiff / 30)
                if (months == 1) "In 1 month" else "In $months months"
            }
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(next))
        }
    }

    companion object {
        fun formatOrdinal(day: Int): String {
            val mod100 = day % 100
            val suffix = if (mod100 in 11..13) "th" else when (day % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
            return "$day$suffix"
        }

        fun monthShortName(month1To12: Int): String {
            val names = listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            return names[(month1To12 - 1).coerceIn(0, 11)]
        }

        fun monthFullName(month1To12: Int): String {
            val names = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            return names[(month1To12 - 1).coerceIn(0, 11)]
        }

        private fun daysBetween(start: Calendar, end: Calendar): Int {
            val s = (start.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val e = (end.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffMs = e.timeInMillis - s.timeInMillis
            return max(0, (diffMs / 86_400_000L).toInt())
        }

        fun maxDaysInMonth(month1To12: Int, year: Int = Calendar.getInstance().get(Calendar.YEAR)): Int {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, (month1To12 - 1).coerceIn(0, 11))
                set(Calendar.DAY_OF_MONTH, 1)
            }
            return min(31, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        }
    }
}

data class ReminderDraft(
    val title: String,
    val notes: String,
    val cadence: CadenceType,
    val category: ReminderCategory,
    val priority: ReminderPriority,
    val hour: Int,
    val minute: Int,
    val dayOfMonth: Int,
    val monthOfYear: Int,
    val year: Int,
    val recurring: Boolean
)
