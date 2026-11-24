package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CadenceType
import com.example.data.model.Reminder
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderPriority
import com.example.ui.theme.CelebrationRose
import com.example.ui.theme.FinanceIndigo
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.HomeOchre
import com.example.ui.theme.PersonalPlum
import com.example.ui.theme.WorkSlate

fun categoryIcon(category: ReminderCategory): ImageVector = when (category) {
    ReminderCategory.PERSONAL -> Icons.Rounded.PersonOutline
    ReminderCategory.WORK -> Icons.Rounded.WorkOutline
    ReminderCategory.FINANCE -> Icons.Rounded.AccountBalanceWallet
    ReminderCategory.HEALTH -> Icons.Rounded.FavoriteBorder
    ReminderCategory.HOME -> Icons.Rounded.Home
    ReminderCategory.CELEBRATION -> Icons.Rounded.Celebration
}

fun categoryColor(category: ReminderCategory): Color = when (category) {
    ReminderCategory.PERSONAL -> PersonalPlum
    ReminderCategory.WORK -> WorkSlate
    ReminderCategory.FINANCE -> FinanceIndigo
    ReminderCategory.HEALTH -> HealthGreen
    ReminderCategory.HOME -> HomeOchre
    ReminderCategory.CELEBRATION -> CelebrationRose
}

fun cadenceIcon(cadence: CadenceType): ImageVector = when (cadence) {
    CadenceType.DAILY -> Icons.Rounded.Today
    CadenceType.MONTHLY -> Icons.Rounded.CalendarMonth
    CadenceType.YEARLY -> Icons.Rounded.EventRepeat
}

@Composable
fun cadenceColor(cadence: CadenceType): Color = when (cadence) {
    CadenceType.DAILY -> MaterialTheme.colorScheme.secondary
    CadenceType.MONTHLY -> MaterialTheme.colorScheme.primary
    CadenceType.YEARLY -> MaterialTheme.colorScheme.tertiary
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderCard(
    reminder: Reminder,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onSnoozeOneHour: () -> Unit,
    onSnoozeTomorrow: () -> Unit,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val isDone = reminder.isCompletedForCurrentCycle(now)
    val isOverdue = reminder.isOverdue(now)
    val isSnoozed = reminder.isSnoozed(now)
    val cadence = reminder.cadenceType()
    val category = reminder.categoryType()
    val accentColor = cadenceColor(cadence)

    var menuExpanded by remember { mutableStateOf(false) }

    val checkBg by animateColorAsState(
        targetValue = if (isDone) accentColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "checkBg"
    )
    val cardContainer by animateColorAsState(
        targetValue = when {
            isDone || reminder.paused -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "cardContainer"
    )

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = cardContainer),
        border = BorderStroke(
            width = if (isOverdue && !isDone && !reminder.paused) 1.2.dp else 1.dp,
            color = if (isOverdue && !isDone && !reminder.paused) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}")
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Cadence accent stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        if (isDone || reminder.paused) accentColor.copy(alpha = 0.35f)
                        else accentColor
                    )
            )

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
            ) {
                // Accessible 48dp touch target checkbox
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .testTag("toggle_complete_${reminder.id}")
                        .clickable(
                            role = Role.Checkbox,
                            onClickLabel = if (isDone) "Mark active" else "Mark completed",
                            onClick = onToggleComplete
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(checkBg)
                            .border(
                                width = 2.dp,
                                color = if (isDone) accentColor else accentColor.copy(alpha = 0.75f),
                                shape = CircleShape
                            )
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (isDone || reminder.paused) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (reminder.priorityType() == ReminderPriority.HIGH && !isDone) {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = "High priority",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (reminder.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reminder.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Schedule badge
                        Surface(
                            color = accentColor.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = cadenceIcon(cadence),
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = reminder.formatScheduleLabel(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor
                                )
                            }
                        }

                        // Countdown / Status badge
                        val countdownText = reminder.formatCountdownLabel(now)
                        val statusBg = when {
                            isDone -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            isOverdue -> MaterialTheme.colorScheme.secondaryContainer
                            isSnoozed -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val statusFg = when {
                            isDone -> MaterialTheme.colorScheme.onPrimaryContainer
                            isOverdue -> MaterialTheme.colorScheme.onSecondaryContainer
                            isSnoozed -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            color = statusBg,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSnoozed) Icons.Rounded.Snooze else Icons.Rounded.Alarm,
                                    contentDescription = null,
                                    tint = statusFg,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = countdownText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = statusFg
                                )
                            }
                        }

                        // Category chip
                        val catColor = categoryColor(category)
                        Surface(
                            color = catColor.copy(alpha = 0.10f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = categoryIcon(category),
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = category.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = catColor
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("reminder_menu_${reminder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Reminder options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Edit, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        if (!isDone && !reminder.paused) {
                            DropdownMenuItem(
                                text = { Text("Snooze 1 hour") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Snooze, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSnoozeOneHour()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tomorrow 9:00 AM") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.WbSunny, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSnoozeTomorrow()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (reminder.paused) "Resume" else "Pause") },
                            leadingIcon = {
                                Icon(
                                    if (reminder.paused) Icons.Rounded.PlayCircleOutline
                                    else Icons.Rounded.PauseCircleOutline,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onTogglePause()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
