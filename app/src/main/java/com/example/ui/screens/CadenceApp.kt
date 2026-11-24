package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CadenceType
import com.example.data.model.Reminder
import com.example.notifications.ReminderNotificationHelper
import com.example.ui.auth.signOut
import com.example.ui.components.ReminderEditorSheet
import com.example.ui.components.SettingsAccountSheet
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.ReminderViewModel
import com.example.ui.viewmodel.StatusFilter
import com.example.ui.viewmodel.UiState

private fun tabIcon(tab: MainTab): ImageVector = when (tab) {
    MainTab.FOCUS -> Icons.Rounded.AutoAwesomeMosaic
    MainTab.DAILY -> Icons.Rounded.Today
    MainTab.MONTHLY -> Icons.Rounded.CalendarMonth
    MainTab.YEARLY -> Icons.Rounded.EventRepeat
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadenceApp(
    viewModel: ReminderViewModel,
    userDisplayName: String?,
    userEmail: String?,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val uiState by viewModel.remindersState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val selectedMonthDay by viewModel.selectedMonthDay.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSearch by remember { mutableStateOf(false) }
    var editorTarget by remember { mutableStateOf<Reminder?>(null) }
    var isEditorOpen by remember { mutableStateOf(false) }
    var editorDefaultCadence by remember { mutableStateOf(CadenceType.DAILY) }
    var showAccountSheet by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(ReminderNotificationHelper.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            ReminderNotificationHelper.ensureChannel(context)
        }
    }

    fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Sync local AlarmManager schedules whenever Firestore reminders stream updates
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is UiState.Success) {
            ReminderNotificationHelper.syncAllAlarms(context.applicationContext, state.data)
        }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    fun openCreator(cadence: CadenceType = viewModel.defaultCadenceForCurrentTab()) {
        editorTarget = null
        editorDefaultCadence = cadence
        isEditorOpen = true
    }

    fun openEditor(reminder: Reminder) {
        editorTarget = reminder
        editorDefaultCadence = reminder.cadenceType()
        isEditorOpen = true
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.padding(top = 16.dp))
                    MainTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = tabIcon(tab),
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    Column {
                        CenterAlignedTopAppBar(
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                            title = {
                                Text(
                                    text = if (selectedTab == MainTab.FOCUS) {
                                        stringResource(R.string.app_name)
                                    } else {
                                        "${selectedTab.label} Reminders"
                                    },
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        showSearch = !showSearch
                                        if (!showSearch) viewModel.updateSearchQuery("")
                                    },
                                    modifier = Modifier.testTag("search_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                                        contentDescription = if (showSearch) "Close search" else "Search reminders"
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { showAccountSheet = true },
                                    modifier = Modifier.testTag("account_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountCircle,
                                        contentDescription = "Account and notification settings",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )

                        AnimatedVisibility(visible = showSearch) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = viewModel::updateSearchQuery,
                                placeholder = { Text(stringResource(R.string.search_reminders_hint)) },
                                singleLine = true,
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("search_input")
                            )
                        }

                        // Compact status filter chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            items(StatusFilter.entries) { filter ->
                                FilterChip(
                                    selected = statusFilter == filter,
                                    onClick = { viewModel.updateStatusFilter(filter) },
                                    label = { Text(filter.label) },
                                    modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            MainTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("nav_bar_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { openCreator() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null
                            )
                        },
                        text = { Text(stringResource(R.string.add_reminder)) },
                        modifier = Modifier.testTag("add_reminder_fab")
                    )
                }
            ) { innerPadding ->
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        when (val state = uiState) {
                            is UiState.Loading -> {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            is UiState.Error -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp)
                                ) {
                                    Text(
                                        text = "Unable to load reminders",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.size(16.dp))
                                    Button(onClick = { viewModel.selectTab(selectedTab) }) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Retry")
                                    }
                                }
                            }

                            is UiState.Success -> {
                                val all = state.data
                                val now = System.currentTimeMillis()
                                val filtered = remember(all, searchQuery, statusFilter) {
                                    val q = searchQuery.trim().lowercase()
                                    all.filter { item ->
                                        val matchesSearch = q.isEmpty() ||
                                            item.title.lowercase().contains(q) ||
                                            item.notes.lowercase().contains(q) ||
                                            item.categoryType().label.lowercase().contains(q)
                                        val done = item.isCompletedForCurrentCycle(now)
                                        val matchesStatus = when (statusFilter) {
                                            StatusFilter.ALL -> true
                                            StatusFilter.ACTIVE -> !done && !item.paused
                                            StatusFilter.DONE -> done
                                            StatusFilter.PAUSED -> item.paused
                                        }
                                        matchesSearch && matchesStatus
                                    }
                                }

                                when (selectedTab) {
                                    MainTab.FOCUS -> FocusTabContent(
                                        userDisplayName = userDisplayName,
                                        allReminders = all,
                                        filteredReminders = filtered,
                                        showNotificationBanner = !hasNotificationPermission,
                                        onRequestNotificationPermission = ::requestNotificationsIfNeeded,
                                        onSelectCadenceTab = { cadence ->
                                            viewModel.selectTab(
                                                when (cadence) {
                                                    CadenceType.DAILY -> MainTab.DAILY
                                                    CadenceType.MONTHLY -> MainTab.MONTHLY
                                                    CadenceType.YEARLY -> MainTab.YEARLY
                                                }
                                            )
                                        },
                                        onCreateClick = { openCreator(CadenceType.DAILY) },
                                        onToggleComplete = viewModel::toggleComplete,
                                        onEdit = ::openEditor,
                                        onSnoozeOneHour = viewModel::snoozeOneHour,
                                        onSnoozeTomorrow = viewModel::snoozeTomorrowMorning,
                                        onTogglePause = { r -> viewModel.setPaused(r, !r.paused) },
                                        onDelete = { r ->
                                            ReminderNotificationHelper.cancelAlarm(context, r.id)
                                            viewModel.deleteReminder(r)
                                        }
                                    )

                                    MainTab.DAILY -> DailyTabContent(
                                        dailyReminders = filtered.filter {
                                            it.cadenceType() == CadenceType.DAILY
                                        },
                                        onCreateClick = { openCreator(CadenceType.DAILY) },
                                        onToggleComplete = viewModel::toggleComplete,
                                        onEdit = ::openEditor,
                                        onSnoozeOneHour = viewModel::snoozeOneHour,
                                        onSnoozeTomorrow = viewModel::snoozeTomorrowMorning,
                                        onTogglePause = { r -> viewModel.setPaused(r, !r.paused) },
                                        onDelete = { r ->
                                            ReminderNotificationHelper.cancelAlarm(context, r.id)
                                            viewModel.deleteReminder(r)
                                        }
                                    )

                                    MainTab.MONTHLY -> MonthlyTabContent(
                                        monthlyReminders = filtered.filter {
                                            it.cadenceType() == CadenceType.MONTHLY
                                        },
                                        selectedDay = selectedMonthDay,
                                        onToggleDayFilter = viewModel::toggleMonthDayFilter,
                                        onCreateClick = { openCreator(CadenceType.MONTHLY) },
                                        onToggleComplete = viewModel::toggleComplete,
                                        onEdit = ::openEditor,
                                        onSnoozeOneHour = viewModel::snoozeOneHour,
                                        onSnoozeTomorrow = viewModel::snoozeTomorrowMorning,
                                        onTogglePause = { r -> viewModel.setPaused(r, !r.paused) },
                                        onDelete = { r ->
                                            ReminderNotificationHelper.cancelAlarm(context, r.id)
                                            viewModel.deleteReminder(r)
                                        }
                                    )

                                    MainTab.YEARLY -> YearlyTabContent(
                                        yearlyReminders = filtered.filter {
                                            it.cadenceType() == CadenceType.YEARLY
                                        },
                                        onCreateClick = { openCreator(CadenceType.YEARLY) },
                                        onToggleComplete = viewModel::toggleComplete,
                                        onEdit = ::openEditor,
                                        onSnoozeOneHour = viewModel::snoozeOneHour,
                                        onSnoozeTomorrow = viewModel::snoozeTomorrowMorning,
                                        onTogglePause = { r -> viewModel.setPaused(r, !r.paused) },
                                        onDelete = { r ->
                                            ReminderNotificationHelper.cancelAlarm(context, r.id)
                                            viewModel.deleteReminder(r)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isEditorOpen) {
        val target = editorTarget
        ReminderEditorSheet(
            existingReminder = target,
            defaultCadence = editorDefaultCadence,
            onDismiss = { isEditorOpen = false },
            onSave = { draft ->
                if (target == null) {
                    viewModel.addReminder(draft) { created ->
                        created.nextTriggerMillis()?.let { triggerAt ->
                            ReminderNotificationHelper.scheduleAlarm(context, created, triggerAt)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !hasNotificationPermission
                        ) {
                            requestNotificationsIfNeeded()
                        }
                    }
                } else {
                    viewModel.updateReminder(target.id, draft)
                }
                isEditorOpen = false
            },
            onDelete = if (target != null) {
                {
                    ReminderNotificationHelper.cancelAlarm(context, target.id)
                    viewModel.deleteReminder(target)
                    isEditorOpen = false
                }
            } else null
        )
    }

    if (showAccountSheet) {
        SettingsAccountSheet(
            userName = userDisplayName,
            userEmail = userEmail,
            hasNotificationPermission = hasNotificationPermission,
            onRequestNotificationPermission = ::requestNotificationsIfNeeded,
            onSendTestNotification = {
                ReminderNotificationHelper.showTestNotification(context)
                showAccountSheet = false
            },
            onSignOut = {
                showAccountSheet = false
                signOut(
                    credentialManager = credentialManager,
                    scope = scope,
                    onSignOutComplete = onSignedOut
                )
            },
            onDismiss = { showAccountSheet = false }
        )
    }
}
