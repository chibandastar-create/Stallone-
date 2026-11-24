package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.Reminder
import com.example.data.model.ReminderDraft
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ReminderRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing reminders.")
    }

    fun observeReminders(userId: String): Flow<List<Reminder>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/reminders"
        emitAll(
            db.collection("users").document(uid).collection("reminders")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Reminder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun fetchRemindersOnce(): Result<List<Reminder>> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reminders"
        return try {
            val uid = requireUserId()
            path = "users/$uid/reminders"
            val snapshot = db.collection("users").document(uid).collection("reminders")
                .get()
                .await()
            val items = snapshot.toObjects(
                Reminder::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun addReminder(draft: ReminderDraft): Result<Reminder> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reminders"
        return try {
            val uid = requireUserId()
            val cleanTitle = draft.title.trim()
            val cleanNotes = draft.notes.trim()
            require(cleanTitle.length in 1..120) { "Title must be between 1 and 120 characters." }
            require(cleanNotes.length <= 500) { "Notes cannot exceed 500 characters." }

            val docRef = db.collection("users").document(uid).collection("reminders").document()
            docPath = docRef.path

            val safeHour = draft.hour.coerceIn(0, 23)
            val safeMinute = draft.minute.coerceIn(0, 59)
            val safeDay = draft.dayOfMonth.coerceIn(1, 31)
            val safeMonth = draft.monthOfYear.coerceIn(1, 12)
            val safeYear = draft.year.coerceIn(2000, 2100)

            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "notes" to cleanNotes,
                "cadence" to draft.cadence.name,
                "category" to draft.category.name,
                "priority" to draft.priority.name,
                "hour" to safeHour.toLong(),
                "minute" to safeMinute.toLong(),
                "dayOfMonth" to safeDay.toLong(),
                "monthOfYear" to safeMonth.toLong(),
                "year" to safeYear.toLong(),
                "recurring" to draft.recurring,
                "paused" to false,
                "completedOnce" to false,
                "lastCompletedCycleKey" to "",
                "completedCount" to 0L,
                "snoozedUntilEpochMs" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val created = Reminder(
                id = docRef.id,
                userId = uid,
                title = cleanTitle,
                notes = cleanNotes,
                cadence = draft.cadence.name,
                category = draft.category.name,
                priority = draft.priority.name,
                hour = safeHour.toLong(),
                minute = safeMinute.toLong(),
                dayOfMonth = safeDay.toLong(),
                monthOfYear = safeMonth.toLong(),
                year = safeYear.toLong(),
                recurring = draft.recurring
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateReminder(reminderId: String, draft: ReminderDraft): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reminders/$reminderId"
        return try {
            val uid = requireUserId()
            require(reminderId.isNotBlank()) { "Reminder ID must not be blank." }
            val cleanTitle = draft.title.trim()
            val cleanNotes = draft.notes.trim()
            require(cleanTitle.length in 1..120) { "Title must be between 1 and 120 characters." }
            require(cleanNotes.length <= 500) { "Notes cannot exceed 500 characters." }

            val docRef = db.collection("users").document(uid)
                .collection("reminders").document(reminderId)
            docPath = docRef.path

            val payload = mapOf(
                "title" to cleanTitle,
                "notes" to cleanNotes,
                "cadence" to draft.cadence.name,
                "category" to draft.category.name,
                "priority" to draft.priority.name,
                "hour" to draft.hour.coerceIn(0, 23).toLong(),
                "minute" to draft.minute.coerceIn(0, 59).toLong(),
                "dayOfMonth" to draft.dayOfMonth.coerceIn(1, 31).toLong(),
                "monthOfYear" to draft.monthOfYear.coerceIn(1, 12).toLong(),
                "year" to draft.year.coerceIn(2000, 2100).toLong(),
                "recurring" to draft.recurring,
                "paused" to false,
                "completedOnce" to false,
                "lastCompletedCycleKey" to "",
                "snoozedUntilEpochMs" to 0L,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleComplete(reminder: Reminder, nowMillis: Long = System.currentTimeMillis()): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reminders/${reminder.id}"
        return try {
            val uid = requireUserId()
            require(reminder.id.isNotBlank()) { "Reminder ID must not be blank." }
            val docRef = db.collection("users").document(uid)
                .collection("reminders").document(reminder.id)
            docPath = docRef.path

            val currentlyCompleted = reminder.isCompletedForCurrentCycle(nowMillis)
            val newCompletedOnce: Boolean
            val newCycleKey: String
            val newCount: Long

            if (currentlyCompleted) {
                newCompletedOnce = false
                newCycleKey = ""
                newCount = reminder.completedCount
            } else {
                newCompletedOnce = !reminder.recurring
                newCycleKey = reminder.currentCycleKey(nowMillis)
                newCount = (reminder.completedCount + 1L).coerceAtMost(1_000_000L)
            }

            docRef.update(
                mapOf(
                    "completedOnce" to newCompletedOnce,
                    "lastCompletedCycleKey" to newCycleKey,
                    "completedCount" to newCount,
                    "snoozedUntilEpochMs" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markCompleteById(reminderId: String, nowMillis: Long = System.currentTimeMillis()): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reminders/$reminderId"
        return try {
            val uid = requireUserId()
            require(reminderId.isNotBlank()) { "Reminder ID must not be blank." }
            val docRef = db.collection("users").document(uid)
                .collection("reminders").document(reminderId)
            docPath = docRef.path
            val snapshot = docRef.get().await()
            val reminder = snapshot.toObject(
                Reminder::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("Reminder not found"))
            if (reminder.isCompletedForCurrentCycle(nowMillis)) {
                return Result.success(Unit)
            }
            toggleComplete(reminder, nowMillis)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun snoozeReminder(reminderId: String, snoozeUntilEpochMs: Long): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reminders/$reminderId"
        return try {
            val uid = requireUserId()
            require(reminderId.isNotBlank()) { "Reminder ID must not be blank." }
            val docRef = db.collection("users").document(uid)
                .collection("reminders").document(reminderId)
            docPath = docRef.path

            docRef.update(
                mapOf(
                    "snoozedUntilEpochMs" to snoozeUntilEpochMs.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setPaused(reminderId: String, paused: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reminders/$reminderId"
        return try {
            val uid = requireUserId()
            require(reminderId.isNotBlank()) { "Reminder ID must not be blank." }
            val docRef = db.collection("users").document(uid)
                .collection("reminders").document(reminderId)
            docPath = docRef.path

            docRef.update(
                mapOf(
                    "paused" to paused,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(reminderId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reminders/$reminderId"
        return try {
            val uid = requireUserId()
            require(reminderId.isNotBlank()) { "Reminder ID must not be blank." }
            val docRef = db.collection("users").document(uid)
                .collection("reminders").document(reminderId)
            docPath = docRef.path

            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
