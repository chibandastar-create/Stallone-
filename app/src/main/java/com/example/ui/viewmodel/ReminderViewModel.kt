package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CadenceType
import com.example.data.model.Reminder
import com.example.data.model.ReminderDraft
import com.example.data.repository.ReminderRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MainTab(val label: String) {
    FOCUS("Focus"),
    DAILY("Daily"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

enum class StatusFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    DONE("Done"),
    PAUSED("Paused")
}

class ReminderViewModel(
    private val repository: ReminderRepository,
    private val currentUserId: String
) : ViewModel() {

    val remindersState: StateFlow<UiState<List<Reminder>>> = repository
        .observeReminders(currentUserId)
        .map<List<Reminder>, UiState<List<Reminder>>> { list ->
            val sorted = list.sortedWith(
                compareBy<Reminder>(
                    { it.isCompletedForCurrentCycle() },
                    { it.paused },
                    { it.nextTriggerMillis() ?: Long.MAX_VALUE },
                    { it.title.lowercase() }
                )
            )
            UiState.Success(sorted)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing reminders", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load reminders"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _selectedTab = MutableStateFlow(MainTab.FOCUS)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    val statusFilter: StateFlow<StatusFilter> = _statusFilter.asStateFlow()

    private val _selectedMonthDay = MutableStateFlow<Int?>(null)
    val selectedMonthDay: StateFlow<Int?> = _selectedMonthDay.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun toggleMonthDayFilter(day: Int) {
        _selectedMonthDay.value = if (_selectedMonthDay.value == day) null else day
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun addReminder(draft: ReminderDraft, onSuccess: (Reminder) -> Unit = {}) {
        viewModelScope.launch {
            repository.addReminder(draft)
                .onSuccess { created ->
                    _snackbarMessage.value = "Scheduled \"${created.title}\""
                    onSuccess(created)
                }
                .onFailure { error ->
                    _snackbarMessage.value = error.localizedMessage ?: "Couldn't save reminder"
                }
        }
    }

    fun updateReminder(reminderId: String, draft: ReminderDraft, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateReminder(reminderId, draft)
                .onSuccess {
                    _snackbarMessage.value = "Updated \"${draft.title.trim()}\""
                    onSuccess()
                }
                .onFailure { error ->
                    _snackbarMessage.value = error.localizedMessage ?: "Couldn't update reminder"
                }
        }
    }

    fun toggleComplete(reminder: Reminder) {
        viewModelScope.launch {
            val wasCompleted = reminder.isCompletedForCurrentCycle()
            repository.toggleComplete(reminder)
                .onSuccess {
                    _snackbarMessage.value = if (wasCompleted) {
                        "Marked \"${reminder.title}\" active"
                    } else {
                        "Completed \"${reminder.title}\""
                    }
                }
                .onFailure { error ->
                    _snackbarMessage.value = error.localizedMessage ?: "Couldn't update completion"
                }
        }
    }

    fun snoozeOneHour(reminder: Reminder) {
        val target = System.currentTimeMillis() + 60L * 60L * 1000L
        snoozeUntil(reminder, target, "Snoozed 1 hour")
    }

    fun snoozeTomorrowMorning(reminder: Reminder) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        snoozeUntil(reminder, cal.timeInMillis, "Snoozed until tomorrow 9:00 AM")
    }

    private fun snoozeUntil(reminder: Reminder, targetMillis: Long, message: String) {
        viewModelScope.launch {
            repository.snoozeReminder(reminder.id, targetMillis)
                .onSuccess { _snackbarMessage.value = message }
                .onFailure { error ->
                    _snackbarMessage.value = error.localizedMessage ?: "Couldn't snooze reminder"
                }
        }
    }

    fun setPaused(reminder: Reminder, paused: Boolean) {
        viewModelScope.launch {
            repository.setPaused(reminder.id, paused)
                .onSuccess {
                    _snackbarMessage.value = if (paused) {
                        "Paused \"${reminder.title}\""
                    } else {
                        "Resumed \"${reminder.title}\""
                    }
                }
                .onFailure { error ->
                    _snackbarMessage.value = error.localizedMessage ?: "Couldn't update pause state"
                }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder.id)
                .onSuccess {
                    _snackbarMessage.value = "Deleted \"${reminder.title}\""
                }
                .onFailure { error ->
                    _snackbarMessage.value = error.localizedMessage ?: "Couldn't delete reminder"
                }
        }
    }

    fun defaultCadenceForCurrentTab(): CadenceType {
        return when (_selectedTab.value) {
            MainTab.DAILY -> CadenceType.DAILY
            MainTab.MONTHLY -> CadenceType.MONTHLY
            MainTab.YEARLY -> CadenceType.YEARLY
            MainTab.FOCUS -> CadenceType.DAILY
        }
    }

    private companion object {
        const val TAG = "ReminderVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
