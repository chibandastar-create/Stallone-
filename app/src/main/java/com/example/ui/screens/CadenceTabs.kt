package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CadenceType
import com.example.data.model.Reminder
import com.example.ui.components.EmptyRemindersState
import com.example.ui.components.FocusHeroCard
import com.example.ui.components.NotificationPermissionBanner
import com.example.ui.components.ReminderCard
import java.util.Calendar

@Composable
fun FocusTabContent(
    userDisplayName: String?,
    allReminders: List<Reminder>,
    filteredReminders: List<Reminder>,
    showNotificationBanner: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSelectCadenceTab: (CadenceType) -> Unit,
    onCreateClick: () -> Unit,
    onToggleComplete: (Reminder) -> Unit,
    onEdit: (Reminder) -> Unit,
    onSnoozeOneHour: (Reminder) -> Unit,
    onSnoozeTomorrow: (Reminder) -> Unit,
    onTogglePause: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val dueTodayOrOverdue = filteredReminders.filter {
        !it.paused && !it.isCompletedForCurrentCycle(now) && (it.isDueToday(now) || it.isOverdue(now))
    }
    val upcoming = filteredReminders.filter {
        !it.paused && !it.isCompletedForCurrentCycle(now) && !it.isDueToday(now) && !it.isOverdue(now)
    }.sortedBy { it.nextTriggerMillis(now) ?: Long.MAX_VALUE }
    val completed = filteredReminders.filter { it.isCompletedForCurrentCycle(now) }
    val paused = filteredReminders.filter { it.paused }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "hero") {
            FocusHeroCard(
                userDisplayName = userDisplayName,
                reminders = allReminders,
                onSelectTab = onSelectCadenceTab
            )
        }

        if (showNotificationBanner) {
            item(key = "notif_banner") {
                NotificationPermissionBanner(onRequestPermission = onRequestNotificationPermission)
            }
        }

        if (filteredReminders.isEmpty()) {
            item(key = "empty_focus") {
                EmptyRemindersState(
                    title = "Your schedule is clear",
                    subtitle = "Add reminders for daily routines, monthly bills, or yearly milestones so nothing slips through the cracks.",
                    buttonLabel = "Create First Reminder",
                    onCreateClick = onCreateClick
                )
            }
        } else {
            if (dueTodayOrOverdue.isNotEmpty()) {
                item(key = "header_today") {
                    SectionHeader(
                        title = "Needs Attention Today",
                        count = dueTodayOrOverdue.size,
                        icon = Icons.Rounded.Today
                    )
                }
                items(dueTodayOrOverdue, key = { "today_${it.id}" }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onToggleComplete = { onToggleComplete(reminder) },
                        onEdit = { onEdit(reminder) },
                        onSnoozeOneHour = { onSnoozeOneHour(reminder) },
                        onSnoozeTomorrow = { onSnoozeTomorrow(reminder) },
                        onTogglePause = { onTogglePause(reminder) },
                        onDelete = { onDelete(reminder) }
                    )
                }
            }

            if (upcoming.isNotEmpty()) {
                item(key = "header_upcoming") {
                    SectionHeader(
                        title = "Upcoming Horizons",
                        count = upcoming.size,
                        icon = Icons.Rounded.EventRepeat
                    )
                }
                items(upcoming, key = { "upcoming_${it.id}" }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onToggleComplete = { onToggleComplete(reminder) },
                        onEdit = { onEdit(reminder) },
                        onSnoozeOneHour = { onSnoozeOneHour(reminder) },
                        onSnoozeTomorrow = { onSnoozeTomorrow(reminder) },
                        onTogglePause = { onTogglePause(reminder) },
                        onDelete = { onDelete(reminder) }
                    )
                }
            }

            if (completed.isNotEmpty()) {
                item(key = "header_completed") {
                    SectionHeader(
                        title = "Completed This Cycle",
                        count = completed.size,
                        icon = null
                    )
                }
                items(completed, key = { "done_${it.id}" }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onToggleComplete = { onToggleComplete(reminder) },
                        onEdit = { onEdit(reminder) },
                        onSnoozeOneHour = { onSnoozeOneHour(reminder) },
                        onSnoozeTomorrow = { onSnoozeTomorrow(reminder) },
                        onTogglePause = { onTogglePause(reminder) },
                        onDelete = { onDelete(reminder) }
                    )
                }
            }

            if (paused.isNotEmpty()) {
                item(key = "header_paused") {
                    SectionHeader(
                        title = "Paused",
                        count = paused.size,
                        icon = null
                    )
                }
                items(paused, key = { "paused_${it.id}" }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onToggleComplete = { onToggleComplete(reminder) },
                        onEdit = { onEdit(reminder) },
                        onSnoozeOneHour = { onSnoozeOneHour(reminder) },
                        onSnoozeTomorrow = { onSnoozeTomorrow(reminder) },
                        onTogglePause = { onTogglePause(reminder) },
                        onDelete = { onDelete(reminder) }
                    )
                }
            }
        }
    }
}

@Composable
fun DailyTabContent(
    dailyReminders: List<Reminder>,
    onCreateClick: () -> Unit,
    onToggleComplete: (Reminder) -> Unit,
    onEdit: (Reminder) -> Unit,
    onSnoozeOneHour: (Reminder) -> Unit,
    onSnoozeTomorrow: (Reminder) -> Unit,
    onTogglePause: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    val morning = dailyReminders.filter { it.hour in 5..11 }
        .sortedWith(compareBy({ it.hour }, { it.minute }))
    val afternoon = dailyReminders.filter { it.hour in 12..16 }
        .sortedWith(compareBy({ it.hour }, { it.minute }))
    val evening = dailyReminders.filter { it.hour in 17..21 }
        .sortedWith(compareBy({ it.hour }, { it.minute }))
    val night = dailyReminders.filter { it.hour >= 22 || it.hour <= 4 }
        .sortedWith(compareBy({ it.hour }, { it.minute }))

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        if (dailyReminders.isEmpty()) {
            item(key = "empty_daily") {
                EmptyRemindersState(
                    title = "No daily reminders yet",
                    subtitle = "Track daily habits, medications, workouts, or today's top errands with timed notifications.",
                    buttonLabel = "Add Daily Reminder",
                    onCreateClick = onCreateClick
                )
            }
        } else {
            val blocks = listOf(
                Triple("Morning · 5 AM – 12 PM", Icons.Rounded.WbSunny, morning),
                Triple("Afternoon · 12 PM – 5 PM", Icons.Rounded.WbTwilight, afternoon),
                Triple("Evening · 5 PM – 10 PM", Icons.Rounded.NightsStay, evening),
                Triple("Late Night · 10 PM – 5 AM", Icons.Rounded.Bedtime, night)
            )
            blocks.forEach { (label, icon, list) ->
                if (list.isNotEmpty()) {
                    item(key = "daily_header_$label") {
                        SectionHeader(title = label, count = list.size, icon = icon)
                    }
                    items(list, key = { "daily_${it.id}" }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onToggleComplete = { onToggleComplete(reminder) },
                            onEdit = { onEdit(reminder) },
                            onSnoozeOneHour = { onSnoozeOneHour(reminder) },
                            onSnoozeTomorrow = { onSnoozeTomorrow(reminder) },
                            onTogglePause = { onTogglePause(reminder) },
                            onDelete = { onDelete(reminder) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyTabContent(
    monthlyReminders: List<Reminder>,
    selectedDay: Int?,
    onToggleDayFilter: (Int) -> Unit,
    onCreateClick: () -> Unit,
    onToggleComplete: (Reminder) -> Unit,
    onEdit: (Reminder) -> Unit,
    onSnoozeOneHour: (Reminder) -> Unit,
    onSnoozeTomorrow: (Reminder) -> Unit,
    onTogglePause: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayDay = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
    val daysWithItems = remember(monthlyReminders) {
        monthlyReminders.map { it.dayOfMonth.toInt() }.toSet()
    }
    val displayed = remember(monthlyReminders, selectedDay) {
        val base = if (selectedDay == null) monthlyReminders
        else monthlyReminders.filter { it.dayOfMonth.toInt() == selectedDay }
        base.sortedWith(compareBy({ it.dayOfMonth }, { it.hour }, { it.minute }))
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "monthly_rhythm_card") {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monthly Rhythm (Day 1–31)",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        if (selectedDay != null) {
                            TextButton(onClick = { onToggleDayFilter(selectedDay) }) {
                                Text("Clear day $selectedDay")
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (1..31).forEach { day ->
                            val hasItem = day in daysWithItems
                            val isSelected = selectedDay == day
                            val isToday = day == todayDay
                            val bg = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                hasItem -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            }
                            val fg = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                hasItem -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = bg,
                                border = if (isToday && !isSelected) {
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                                } else null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { onToggleDayFilter(day) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (hasItem || isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = fg
                                    )
                                    if (hasItem && !isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 3.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (displayed.isEmpty()) {
            item(key = "empty_monthly") {
                EmptyRemindersState(
                    title = if (selectedDay != null) {
                        "Nothing on day $selectedDay"
                    } else {
                        "No monthly reminders yet"
                    },
                    subtitle = "Schedule rent, utility bills, subscriptions, or monthly cleanups on any day of the month.",
                    buttonLabel = "Add Monthly Reminder",
                    onCreateClick = onCreateClick
                )
            }
        } else {
            item(key = "monthly_header") {
                SectionHeader(
                    title = if (selectedDay != null) {
                        "Day ${Reminder.formatOrdinal(selectedDay)} Reminders"
                    } else {
                        "All Monthly Responsibilities"
                    },
                    count = displayed.size,
                    icon = Icons.Rounded.CalendarMonth
                )
            }
            items(displayed, key = { "monthly_${it.id}" }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onToggleComplete = { onToggleComplete(reminder) },
                    onEdit = { onEdit(reminder) },
                    onSnoozeOneHour = { onSnoozeOneHour(reminder) },
                    onSnoozeTomorrow = { onSnoozeTomorrow(reminder) },
                    onTogglePause = { onTogglePause(reminder) },
                    onDelete = { onDelete(reminder) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YearlyTabContent(
    yearlyReminders: List<Reminder>,
    onCreateClick: () -> Unit,
    onToggleComplete: (Reminder) -> Unit,
    onEdit: (Reminder) -> Unit,
    onSnoozeOneHour: (Reminder) -> Unit,
    onSnoozeTomorrow: (Reminder) -> Unit,
    onTogglePause: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMonth by remember { mutableStateOf<Int?>(null) }
    val currentMonth = remember { Calendar.getInstance().get(Calendar.MONTH) + 1 }

    val countsByMonth = remember(yearlyReminders) {
        yearlyReminders.groupingBy { it.monthOfYear.toInt().coerceIn(1, 12) }.eachCount()
    }

    val displayed = remember(yearlyReminders, selectedMonth) {
        val base = if (selectedMonth == null) yearlyReminders
        else yearlyReminders.filter { it.monthOfYear.toInt() == selectedMonth }
        base.sortedBy { it.nextTriggerMillis() ?: Long.MAX_VALUE }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "yearly_horizon_card") {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.EventRepeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Annual Horizon",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        if (selectedMonth != null) {
                            TextButton(onClick = { selectedMonth = null }) {
                                Text("Show all")
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (1..12).forEach { month ->
                            val count = countsByMonth[month] ?: 0
                            val isSelected = selectedMonth == month
                            val isCurrent = month == currentMonth
                            val bg = when {
                                isSelected -> MaterialTheme.colorScheme.tertiary
                                count > 0 -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                            val fg = when {
                                isSelected -> MaterialTheme.colorScheme.onTertiary
                                count > 0 -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = bg,
                                border = if (isCurrent && !isSelected) {
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                                } else null,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable {
                                        selectedMonth = if (isSelected) null else month
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = Reminder.monthShortName(month),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (count > 0 || isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = fg
                                    )
                                    if (count > 0) {
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = fg.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (displayed.isEmpty()) {
            item(key = "empty_yearly") {
                EmptyRemindersState(
                    title = if (selectedMonth != null) {
                        "Nothing in ${Reminder.monthFullName(selectedMonth!!)}"
                    } else {
                        "No yearly milestones yet"
                    },
                    subtitle = "Save birthdays, passport renewals, insurance checks, tax prep, and annual traditions.",
                    buttonLabel = "Add Yearly Reminder",
                    onCreateClick = onCreateClick
                )
            }
        } else {
            item(key = "yearly_header") {
                SectionHeader(
                    title = if (selectedMonth != null) {
                        "${Reminder.monthFullName(selectedMonth!!)} Milestones"
                    } else {
                        "Annual Milestones & Renewals"
                    },
                    count = displayed.size,
                    icon = Icons.Rounded.EventRepeat
                )
            }
            items(displayed, key = { "yearly_${it.id}" }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onToggleComplete = { onToggleComplete(reminder) },
                    onEdit = { onEdit(reminder) },
                    onSnoozeOneHour = { onSnoozeOneHour(reminder) },
                    onSnoozeTomorrow = { onSnoozeTomorrow(reminder) },
                    onTogglePause = { onTogglePause(reminder) },
                    onDelete = { onDelete(reminder) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    icon: ImageVector?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}
