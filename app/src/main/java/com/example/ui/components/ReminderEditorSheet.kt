package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import com.example.data.model.CadenceType
import com.example.data.model.Reminder
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderDraft
import com.example.data.model.ReminderPriority
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class QuickSuggestion(
    val title: String,
    val cadence: CadenceType,
    val category: ReminderCategory,
    val hour: Int,
    val minute: Int,
    val dayOfMonth: Int = 1,
    val monthOfYear: Int = 1
)

private val QUICK_SUGGESTIONS = listOf(
    QuickSuggestion("Drink water & stretch", CadenceType.DAILY, ReminderCategory.HEALTH, 8, 30),
    QuickSuggestion("Review daily priorities", CadenceType.DAILY, ReminderCategory.WORK, 9, 0),
    QuickSuggestion("Pay monthly bills", CadenceType.MONTHLY, ReminderCategory.FINANCE, 10, 0, dayOfMonth = 1),
    QuickSuggestion("Replace home air filter", CadenceType.MONTHLY, ReminderCategory.HOME, 11, 0, dayOfMonth = 15),
    QuickSuggestion("Annual health checkup", CadenceType.YEARLY, ReminderCategory.HEALTH, 9, 30, dayOfMonth = 10, monthOfYear = 3),
    QuickSuggestion("Renew vehicle registration", CadenceType.YEARLY, ReminderCategory.PERSONAL, 10, 0, dayOfMonth = 1, monthOfYear = 6)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorSheet(
    existingReminder: Reminder?,
    defaultCadence: CadenceType,
    onDismiss: () -> Unit,
    onSave: (ReminderDraft) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val nowCal = remember { Calendar.getInstance() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(existingReminder?.title ?: "") }
    var notes by remember { mutableStateOf(existingReminder?.notes ?: "") }
    var cadence by remember {
        mutableStateOf(existingReminder?.cadenceType() ?: defaultCadence)
    }
    var category by remember {
        mutableStateOf(existingReminder?.categoryType() ?: ReminderCategory.PERSONAL)
    }
    var priority by remember {
        mutableStateOf(existingReminder?.priorityType() ?: ReminderPriority.NORMAL)
    }
    var hour by remember {
        mutableIntStateOf(existingReminder?.hour?.toInt() ?: 9)
    }
    var minute by remember {
        mutableIntStateOf(existingReminder?.minute?.toInt() ?: 0)
    }
    var dayOfMonth by remember {
        mutableIntStateOf(
            existingReminder?.dayOfMonth?.toInt() ?: nowCal.get(Calendar.DAY_OF_MONTH)
        )
    }
    var monthOfYear by remember {
        mutableIntStateOf(
            existingReminder?.monthOfYear?.toInt() ?: (nowCal.get(Calendar.MONTH) + 1)
        )
    }
    var year by remember {
        mutableIntStateOf(
            existingReminder?.year?.toInt() ?: nowCal.get(Calendar.YEAR)
        )
    }
    var recurring by remember {
        mutableStateOf(existingReminder?.recurring ?: true)
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (existingReminder == null) "New Reminder" else "Edit Reminder",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (existingReminder != null && onDelete != null) {
                    IconButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.testTag("delete_reminder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete reminder",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Quick idea chips when creating
            if (existingReminder == null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quick starters",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QUICK_SUGGESTIONS.forEach { idea ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    title = idea.title
                                    cadence = idea.cadence
                                    category = idea.category
                                    hour = idea.hour
                                    minute = idea.minute
                                    dayOfMonth = idea.dayOfMonth
                                    monthOfYear = idea.monthOfYear
                                    recurring = true
                                    titleError = null
                                }
                            ) {
                                Text(
                                    text = idea.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Cadence Horizon Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Horizon",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CadenceType.entries.forEach { option ->
                        val selected = cadence == option
                        val accent = cadenceColor(option)
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) accent.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cadence_option_${option.name.lowercase()}")
                                .clickable { cadence = option }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp)
                            ) {
                                Icon(
                                    imageVector = cadenceIcon(option),
                                    contentDescription = null,
                                    tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) accent else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it.length <= 120) {
                        title = it
                        if (it.isNotBlank()) titleError = null
                    }
                },
                label = { Text("What do you need to do?") },
                placeholder = { Text("e.g., Pay electric bill or Call dentist") },
                isError = titleError != null,
                supportingText = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(titleError ?: "")
                        Text("${title.length}/120")
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_title_input")
            )

            // Notes input
            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 500) notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Account number, checklist, or helpful context") },
                minLines = 2,
                maxLines = 4,
                supportingText = {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${notes.length}/500")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_notes_input")
            )

            // Time & Repeat Card
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val timeLabel = remember(hour, minute) {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                        }
                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Reminder time",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Tap to change notification time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (cadence == CadenceType.YEARLY) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Month",
                                style = MaterialTheme.typography.titleSmall
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                (1..12).forEach { m ->
                                    val selected = monthOfYear == m
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            monthOfYear = m
                                            val maxD = Reminder.maxDaysInMonth(m, year)
                                            if (dayOfMonth > maxD) dayOfMonth = maxD
                                        },
                                        label = { Text(Reminder.monthShortName(m)) }
                                    )
                                }
                            }
                        }
                    }

                    if (cadence == CadenceType.MONTHLY || cadence == CadenceType.YEARLY) {
                        val maxDays = if (cadence == CadenceType.YEARLY) {
                            Reminder.maxDaysInMonth(monthOfYear, year)
                        } else {
                            31
                        }
                        val safeDay = dayOfMonth.coerceIn(1, maxDays)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Day of month",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (cadence == CadenceType.MONTHLY && safeDay >= 29) {
                                        "Shorter months notify on their last day"
                                    } else {
                                        "Notifies on the ${Reminder.formatOrdinal(safeDay)}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        dayOfMonth = if (safeDay <= 1) maxDays else safeDay - 1
                                    }
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Previous day")
                                }
                                Text(
                                    text = Reminder.formatOrdinal(safeDay),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = {
                                        dayOfMonth = if (safeDay >= maxDays) 1 else safeDay + 1
                                    }
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Next day")
                                }
                            }
                        }
                    }

                    if (cadence == CadenceType.YEARLY && !recurring) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Target year",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (year > 2024) year -= 1 }) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Previous year")
                                }
                                Text(
                                    text = year.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { if (year < 2099) year += 1 }) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Next year")
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (cadence) {
                                    CadenceType.DAILY -> "Repeat every day"
                                    CadenceType.MONTHLY -> "Repeat every month"
                                    CadenceType.YEARLY -> "Repeat every year"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (recurring) "Automatically resets for next cycle"
                                else "One-time reminder",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = recurring,
                            onCheckedChange = { recurring = it },
                            modifier = Modifier.testTag("recurring_switch")
                        )
                    }
                }
            }

            // Category chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReminderCategory.entries.forEach { cat ->
                        val selected = category == cat
                        val catColor = categoryColor(cat)
                        FilterChip(
                            selected = selected,
                            onClick = { category = cat },
                            label = { Text(cat.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.16f),
                                selectedLabelColor = catColor,
                                selectedLeadingIconColor = catColor
                            )
                        )
                    }
                }
            }

            // Priority toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Flag,
                        contentDescription = null,
                        tint = if (priority == ReminderPriority.HIGH) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "High priority",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Highlight in Focus view",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = priority == ReminderPriority.HIGH,
                    onCheckedChange = {
                        priority = if (it) ReminderPriority.HIGH else ReminderPriority.NORMAL
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val trimmed = title.trim()
                        if (trimmed.isEmpty()) {
                            titleError = "Please enter a reminder title"
                            return@Button
                        }
                        onSave(
                            ReminderDraft(
                                title = trimmed,
                                notes = notes.trim(),
                                cadence = cadence,
                                category = category,
                                priority = priority,
                                hour = hour,
                                minute = minute,
                                dayOfMonth = dayOfMonth.coerceIn(1, 31),
                                monthOfYear = monthOfYear.coerceIn(1, 12),
                                year = year.coerceIn(2000, 2100),
                                recurring = recurring
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("save_reminder_button")
                ) {
                    Text(if (existingReminder == null) "Save Reminder" else "Update")
                }
            }
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hour = pickerState.hour
                        minute = pickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete reminder?") },
            text = { Text("This will remove \"${existingReminder?.title}\" from your synced reminders.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Keep")
                }
            }
        )
    }
}
