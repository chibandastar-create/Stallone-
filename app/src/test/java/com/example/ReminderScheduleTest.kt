package com.example

import com.example.data.model.CadenceType
import com.example.data.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderScheduleTest {

    private fun calAt(year: Int, month1To12: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month1To12 - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `daily recurring cycle key resets next day`() {
        val reminder = Reminder(
            id = "r1",
            title = "Vitamins",
            cadence = CadenceType.DAILY.name,
            hour = 8,
            minute = 0,
            recurring = true,
            lastCompletedCycleKey = "2026-11-24"
        )
        val sameDay = calAt(2026, 11, 24, 14, 0)
        val nextDay = calAt(2026, 11, 25, 7, 0)

        assertTrue(reminder.isCompletedForCurrentCycle(sameDay))
        assertFalse(reminder.isCompletedForCurrentCycle(nextDay))

        val nextTrigger = reminder.nextTriggerMillis(sameDay)
        assertNotNull(nextTrigger)
        assertEquals(calAt(2026, 11, 25, 8, 0), nextTrigger)
    }

    @Test
    fun `monthly reminder on 31st clamps to 28th in February`() {
        val reminder = Reminder(
            id = "r2",
            title = "Month end report",
            cadence = CadenceType.MONTHLY.name,
            hour = 10,
            minute = 30,
            dayOfMonth = 31,
            recurring = true
        )
        val jan31After = calAt(2026, 1, 31, 12, 0)
        val nextTrigger = reminder.nextTriggerMillis(jan31After)
        assertEquals(calAt(2026, 2, 28, 10, 30), nextTrigger)
    }

    @Test
    fun `yearly recurring reminder advances to next year after completion`() {
        val reminder = Reminder(
            id = "r3",
            title = "Passport check",
            cadence = CadenceType.YEARLY.name,
            hour = 9,
            minute = 0,
            dayOfMonth = 15,
            monthOfYear = 5,
            recurring = true,
            lastCompletedCycleKey = "2026"
        )
        val beforeMay = calAt(2026, 4, 1, 9, 0)
        assertTrue(reminder.isCompletedForCurrentCycle(beforeMay))
        assertEquals(calAt(2027, 5, 15, 9, 0), reminder.nextTriggerMillis(beforeMay))
    }

    @Test
    fun `paused reminder returns null trigger`() {
        val reminder = Reminder(
            id = "r4",
            title = "Paused habit",
            cadence = CadenceType.DAILY.name,
            paused = true
        )
        assertNull(reminder.nextTriggerMillis(calAt(2026, 11, 24, 8, 0)))
    }
}
