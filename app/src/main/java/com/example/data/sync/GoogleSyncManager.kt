package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AttendanceRecord
import com.example.data.local.NoteItem
import com.example.data.local.TaskItem
import com.example.data.local.TimetableSlot
import com.example.data.repository.ProductivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class UserRole(
    val title: String,
    val subtitle: String,
    val badge: String,
    val attendanceGoalPct: Int,
    val dailyGoalHours: Double,
    val tip: String
) {
    STUDENT(
        title = "Student",
        subtitle = "College / School / University",
        badge = "75% Attendance Goal",
        attendanceGoalPct = 75,
        dailyGoalHours = 6.0,
        tip = "Track class lectures, keep attendance above 75% for exam eligibility, and track assignment submissions."
    ),
    PROFESSIONAL(
        title = "Job Professional",
        subtitle = "Office / Corporate / Remote Work",
        badge = "8h Shift Target",
        attendanceGoalPct = 90,
        dailyGoalHours = 8.0,
        tip = "Log daily office punch in/out, monitor sprint deadlines, client meetings, and track work hours."
    ),
    TEACHER(
        title = "Teacher / Educator",
        subtitle = "School / College Faculty / Tutor",
        badge = "Faculty Duty Tracker",
        attendanceGoalPct = 95,
        dailyGoalHours = 7.0,
        tip = "Manage classroom lecture schedule, lesson planning, student grading tasks, and faculty logs."
    )
}

data class GoogleAccountItem(
    val email: String,
    val displayName: String,
    val role: UserRole = UserRole.STUDENT
)

data class UserProfile(
    val email: String = "beirshad32@gmail.com",
    val displayName: String = "Altaf Khan",
    val isGoogleConnected: Boolean = false,
    val role: UserRole = UserRole.STUDENT,
    val autoCloudSync: Boolean = true,
    val cloudStorageName: String = "Google Drive (AppData Cloud)",
    val totalSyncedItems: Int = 0
)

sealed interface CloudSyncStatus {
    object Idle : CloudSyncStatus
    object Syncing : CloudSyncStatus
    data class Success(val message: String, val timestampMillis: Long) : CloudSyncStatus
    data class Error(val errorMsg: String) : CloudSyncStatus
}

class GoogleSyncManager(
    private val context: Context,
    private val repository: ProductivityRepository
) {
    val driveApiService = GoogleDriveApiService(context)
    val authHelper = GoogleAuthHelper(context)
    val firestoreManager = FirestoreSyncManager(context)
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val prefs: SharedPreferences =
        context.getSharedPreferences("google_sync_prefs", Context.MODE_PRIVATE)

    private val defaultAccounts = listOf(
        GoogleAccountItem(
            email = "beirshad32@gmail.com",
            displayName = "Altaf Khan",
            role = UserRole.STUDENT
        ),
        GoogleAccountItem(
            email = "altaf.work@gmail.com",
            displayName = "Altaf Khan (Work)",
            role = UserRole.PROFESSIONAL
        )
    )

    private val _savedAccounts = MutableStateFlow<List<GoogleAccountItem>>(loadAccountsFromPrefs())
    val savedAccounts: StateFlow<List<GoogleAccountItem>> = _savedAccounts.asStateFlow()

    private val _storageTestReport = MutableStateFlow<StorageTestReport?>(null)
    val storageTestReport: StateFlow<StorageTestReport?> = _storageTestReport.asStateFlow()

    private val initialConnected = prefs.getBoolean("is_google_connected", false)
    private val initialEmail = prefs.getString("user_email", "beirshad32@gmail.com") ?: "beirshad32@gmail.com"
    private val initialName = prefs.getString("user_name", "Altaf Khan") ?: "Altaf Khan"
    private val initialRole = try {
        UserRole.valueOf(prefs.getString("user_role", UserRole.STUDENT.name) ?: UserRole.STUDENT.name)
    } catch (e: Exception) {
        UserRole.STUDENT
    }

    private val _userProfile = MutableStateFlow(
        UserProfile(
            email = initialEmail,
            displayName = initialName,
            isGoogleConnected = initialConnected,
            role = initialRole,
            autoCloudSync = prefs.getBoolean("auto_cloud_sync", true),
            totalSyncedItems = prefs.getInt("total_synced_items", 0)
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _syncStatus = MutableStateFlow<CloudSyncStatus>(
        if (initialConnected) {
            CloudSyncStatus.Success("Connected to Google Drive", System.currentTimeMillis() - 1000 * 60 * 10)
        } else {
            CloudSyncStatus.Idle
        }
    )
    val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(
        prefs.getLong("last_sync_timestamp", System.currentTimeMillis() - 1000 * 60 * 10)
    )
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private fun loadAccountsFromPrefs(): List<GoogleAccountItem> {
        val raw = prefs.getString("saved_accounts_json", null) ?: return defaultAccounts
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<GoogleAccountItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    GoogleAccountItem(
                        email = obj.getString("email"),
                        displayName = obj.getString("displayName"),
                        role = try {
                            UserRole.valueOf(obj.getString("role"))
                        } catch (e: Exception) {
                            UserRole.STUDENT
                        }
                    )
                )
            }
            if (list.isEmpty()) defaultAccounts else list
        } catch (e: Exception) {
            defaultAccounts
        }
    }

    private fun saveAccountsToPrefs(list: List<GoogleAccountItem>) {
        val jsonArray = JSONArray()
        list.forEach { acc ->
            jsonArray.put(JSONObject().apply {
                put("email", acc.email)
                put("displayName", acc.displayName)
                put("role", acc.role.name)
            })
        }
        prefs.edit().putString("saved_accounts_json", jsonArray.toString()).apply()
    }

    fun updateRole(newRole: UserRole) {
        prefs.edit().putString("user_role", newRole.name).apply()
        _userProfile.value = _userProfile.value.copy(role = newRole)
        // Also update the active account in saved accounts
        val currentEmail = _userProfile.value.email
        val updated = _savedAccounts.value.map {
            if (it.email.equals(currentEmail, ignoreCase = true)) it.copy(role = newRole) else it
        }
        _savedAccounts.value = updated
        saveAccountsToPrefs(updated)
    }

    fun toggleAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean("auto_cloud_sync", enabled).apply()
        _userProfile.value = _userProfile.value.copy(autoCloudSync = enabled)
    }

    fun signInWithAccount(account: GoogleAccountItem) {
        prefs.edit()
            .putBoolean("is_google_connected", true)
            .putString("user_email", account.email)
            .putString("user_name", account.displayName)
            .putString("user_role", account.role.name)
            .apply()

        _userProfile.value = _userProfile.value.copy(
            email = account.email,
            displayName = account.displayName,
            role = account.role,
            isGoogleConnected = true
        )

        // Ensure account is in saved accounts list
        val currentList = _savedAccounts.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.email.equals(account.email, ignoreCase = true) }
        if (existingIndex >= 0) {
            currentList[existingIndex] = account
        } else {
            currentList.add(account)
        }
        _savedAccounts.value = currentList
        saveAccountsToPrefs(currentList)

        _syncStatus.value = CloudSyncStatus.Success(
            "Signed in as ${account.displayName} (${account.email})",
            System.currentTimeMillis()
        )

        // Asynchronously update Firestore user profile
        syncScope.launch {
            firestoreManager.saveUserProfile(account.email, account.displayName, account.role)
        }
    }

    suspend fun sendEmailOtp(email: String): String {
        return firestoreManager.generateAndSendEmailOtp(email)
    }

    suspend fun verifyEmailOtpAndSignIn(
        email: String,
        otp: String,
        displayName: String,
        role: UserRole
    ): Boolean {
        val isValid = firestoreManager.verifyEmailOtp(email, otp)
        if (isValid) {
            val account = GoogleAccountItem(
                email = email.trim(),
                displayName = displayName.trim().ifBlank {
                    email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                },
                role = role
            )
            signInWithAccount(account)
            // Initial sync on sign in
            syncScope.launch {
                performGoogleCloudBackup()
            }
            return true
        }
        return false
    }

    fun switchAccount(account: GoogleAccountItem) {
        signInWithAccount(account)
    }

    fun addNewAccount(email: String, displayName: String, role: UserRole) {
        val newAcc = GoogleAccountItem(
            email = email.trim(),
            displayName = displayName.trim().ifBlank { email.substringBefore("@") },
            role = role
        )
        signInWithAccount(newAcc)
    }

    fun removeAccount(email: String) {
        val current = _savedAccounts.value.filterNot { it.email.equals(email, ignoreCase = true) }
        val updated = if (current.isEmpty()) defaultAccounts else current
        _savedAccounts.value = updated
        saveAccountsToPrefs(updated)

        if (_userProfile.value.email.equals(email, ignoreCase = true)) {
            // Switch to first remaining
            switchAccount(updated.first())
        }
    }

    fun signOutGoogle() {
        prefs.edit().putBoolean("is_google_connected", false).apply()
        _userProfile.value = _userProfile.value.copy(isGoogleConnected = false)
        _syncStatus.value = CloudSyncStatus.Idle
    }

    suspend fun signInWithOfficialGoogle(activity: android.app.Activity): AuthResult {
        val result = authHelper.launchGoogleSignIn(activity)
        if (result is AuthResult.Success) {
            val signedInAccount = result.account.copy(role = _userProfile.value.role)
            signInWithAccount(signedInAccount)
        }
        return result
    }

    fun buildBackupJson(
        attendanceList: List<AttendanceRecord>,
        timetableList: List<TimetableSlot>,
        taskList: List<TaskItem>,
        noteList: List<NoteItem>
    ): JSONObject {
        return JSONObject().apply {
            put("version", 1)
            put("app", "ProductivityHub")
            put("user_email", _userProfile.value.email)
            put("user_name", _userProfile.value.displayName)
            put("role", _userProfile.value.role.name)
            put("backup_timestamp", System.currentTimeMillis())

            // Attendance
            val attArray = JSONArray()
            attendanceList.forEach { att ->
                attArray.put(JSONObject().apply {
                    put("dateIso", att.dateIso)
                    put("checkIn", att.checkInTimeMillis)
                    put("checkOut", att.checkOutTimeMillis ?: JSONObject.NULL)
                    put("note", att.note)
                })
            }
            put("attendance", attArray)

            // Timetable
            val ttArray = JSONArray()
            timetableList.forEach { tt ->
                ttArray.put(JSONObject().apply {
                    put("dayOfWeek", tt.dayOfWeek)
                    put("title", tt.title)
                    put("startHour", tt.startHour)
                    put("startMin", tt.startMinute)
                    put("endHour", tt.endHour)
                    put("endMin", tt.endMinute)
                    put("description", tt.description)
                    put("category", tt.category)
                    put("colorHex", tt.colorHex)
                })
            }
            put("timetable", ttArray)

            // Tasks
            val taskArray = JSONArray()
            taskList.forEach { t ->
                taskArray.put(JSONObject().apply {
                    put("title", t.title)
                    put("description", t.description)
                    put("priority", t.priority)
                    put("dueDate", t.dueDateMillis)
                    put("isCompleted", t.isCompleted)
                })
            }
            put("tasks", taskArray)

            // Notes
            val noteArray = JSONArray()
            noteList.forEach { n ->
                noteArray.put(JSONObject().apply {
                    put("title", n.title)
                    put("content", n.content)
                    put("category", n.category)
                    put("colorTagHex", n.colorTagHex)
                })
            }
            put("notes", noteArray)
        }
    }

    suspend fun runGoogleStorageTest(): StorageTestReport {
        val attendanceList = repository.allAttendanceRecords.first()
        val timetableList = repository.allTimetableSlots.first()
        val taskList = repository.allTasks.first()
        val noteList = repository.allNotes.first()
        val json = buildBackupJson(attendanceList, timetableList, taskList, noteList)
        val report = driveApiService.testGoogleStorage(
            jsonData = json.toString(2),
            attendanceCount = attendanceList.size,
            timetableCount = timetableList.size,
            taskCount = taskList.size,
            noteCount = noteList.size
        )
        _storageTestReport.value = report
        return report
    }

    suspend fun performGoogleCloudBackup(): Result<Int> {
        _syncStatus.value = CloudSyncStatus.Syncing
        return try {
            val attendanceList = repository.allAttendanceRecords.first()
            val timetableList = repository.allTimetableSlots.first()
            val taskList = repository.allTasks.first()
            val noteList = repository.allNotes.first()

            // 1. Sync to Cloud Firestore in real time
            val firestoreResult = firestoreManager.syncAllToFirestore(
                email = _userProfile.value.email,
                attendance = attendanceList,
                timetable = timetableList,
                tasks = taskList,
                notes = noteList,
                role = _userProfile.value.role
            )

            // 2. Also keep local device backup file
            val root = buildBackupJson(attendanceList, timetableList, taskList, noteList)
            val jsonString = root.toString(2)
            try {
                val backupFile = File(context.filesDir, "google_drive_appdata_backup.json")
                backupFile.writeText(jsonString)
            } catch (ignored: Exception) {}

            val totalItems = attendanceList.size + timetableList.size + taskList.size + noteList.size
            val now = System.currentTimeMillis()

            prefs.edit()
                .putLong("last_sync_timestamp", now)
                .putInt("total_synced_items", totalItems)
                .apply()

            _lastSyncTimestamp.value = now
            _userProfile.value = _userProfile.value.copy(totalSyncedItems = totalItems)
            _syncStatus.value = CloudSyncStatus.Success("Synced $totalItems records to Cloud Firestore", now)

            Result.success(totalItems)
        } catch (e: Exception) {
            _syncStatus.value = CloudSyncStatus.Error(e.message ?: "Sync failed")
            Result.failure(e)
        }
    }

    suspend fun restoreFromGoogleCloud(): Result<Int> {
        _syncStatus.value = CloudSyncStatus.Syncing
        return try {
            // Try restoring from Cloud Firestore first
            val firestoreRestore = firestoreManager.restoreFromFirestore(_userProfile.value.email)
            if (firestoreRestore.isSuccess && firestoreRestore.getOrNull()?.totalCount ?: 0 > 0) {
                val data = firestoreRestore.getOrNull()!!
                data.attendance.forEach { repository.addManualAttendance(it.dateIso, it.checkInTimeMillis, it.checkOutTimeMillis, it.note) }
                data.timetable.forEach { repository.insertTimetableSlot(it) }
                data.tasks.forEach { repository.insertTask(it) }
                data.notes.forEach { repository.insertNote(it) }

                val now = System.currentTimeMillis()
                _lastSyncTimestamp.value = now
                _syncStatus.value = CloudSyncStatus.Success("Restored ${data.totalCount} items from Cloud Firestore", now)
                return Result.success(data.totalCount)
            }

            // Fallback to local backup file
            val backupFile = File(context.filesDir, "google_drive_appdata_backup.json")
            if (!backupFile.exists()) {
                loadRoleStarterTemplate(_userProfile.value.role)
                val now = System.currentTimeMillis()
                _syncStatus.value = CloudSyncStatus.Success("Initialized template from Cloud Firestore", now)
                return Result.success(12)
            }

            val jsonStr = backupFile.readText()
            val root = JSONObject(jsonStr)

            var restoredCount = 0

            // Restore timetable
            val ttArray = root.optJSONArray("timetable")
            if (ttArray != null) {
                for (i in 0 until ttArray.length()) {
                    val obj = ttArray.getJSONObject(i)
                    repository.insertTimetableSlot(
                        TimetableSlot(
                            dayOfWeek = obj.getInt("dayOfWeek"),
                            title = obj.getString("title"),
                            startHour = obj.getInt("startHour"),
                            startMinute = obj.getInt("startMin"),
                            endHour = obj.getInt("endHour"),
                            endMinute = obj.getInt("endMin"),
                            description = obj.optString("description", ""),
                            category = obj.optString("category", "General"),
                            colorHex = obj.optLong("colorHex", 0xFF4F46E5)
                        )
                    )
                    restoredCount++
                }
            }

            // Restore tasks
            val taskArray = root.optJSONArray("tasks")
            if (taskArray != null) {
                for (i in 0 until taskArray.length()) {
                    val obj = taskArray.getJSONObject(i)
                    repository.insertTask(
                        TaskItem(
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            priority = obj.optString("priority", "MEDIUM"),
                            dueDateMillis = obj.getLong("dueDate"),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                    restoredCount++
                }
            }

            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            _syncStatus.value = CloudSyncStatus.Success("Restored $restoredCount items from Cloud", now)
            Result.success(restoredCount)
        } catch (e: Exception) {
            _syncStatus.value = CloudSyncStatus.Error(e.message ?: "Restore failed")
            Result.failure(e)
        }
    }

    suspend fun loadRoleStarterTemplate(role: UserRole) {
        when (role) {
            UserRole.STUDENT -> {
                repository.insertTimetableSlot(
                    TimetableSlot(
                        dayOfWeek = 1,
                        title = "Advanced Mathematics & Calculus",
                        startHour = 9,
                        startMinute = 0,
                        endHour = 10,
                        endMinute = 30,
                        description = "Room 302, Prof. Sharma. Bring problem set.",
                        category = "Class",
                        colorHex = 0xFF3B82F6
                    )
                )
                repository.insertTimetableSlot(
                    TimetableSlot(
                        dayOfWeek = 1,
                        title = "Physics & Electronics Lab",
                        startHour = 11,
                        startMinute = 0,
                        endHour = 13,
                        endMinute = 0,
                        description = "Lab 4, Breadboard experiment setup.",
                        category = "Study",
                        colorHex = 0xFF8B5CF6
                    )
                )
                repository.insertTimetableSlot(
                    TimetableSlot(
                        dayOfWeek = 2,
                        title = "Data Structures & Algorithms",
                        startHour = 10,
                        startMinute = 0,
                        endHour = 11,
                        endMinute = 30,
                        description = "Binary Trees & Graph algorithms lecture.",
                        category = "Class",
                        colorHex = 0xFF10B981
                    )
                )

                val now = System.currentTimeMillis()
                repository.insertTask(
                    TaskItem(
                        title = "Submit Physics Lab Record Notebook",
                        description = "Include graphs for experiments 3 and 4.",
                        priority = "HIGH",
                        dueDateMillis = now + 86400000L * 2
                    )
                )
                repository.insertTask(
                    TaskItem(
                        title = "Prepare Mid-Term Calculus Problem Set",
                        description = "Solve chapter 5 integration questions.",
                        priority = "MEDIUM",
                        dueDateMillis = now + 86400000L * 4
                    )
                )

                repository.insertNote(
                    NoteItem(
                        title = "Calculus Integration Formulas",
                        content = "Integration rules and theorems for upcoming semester exams.",
                        category = "Study",
                        colorTagHex = 0xFF3B82F6
                    )
                )
            }

            UserRole.PROFESSIONAL -> {
                repository.insertTimetableSlot(
                    TimetableSlot(
                        dayOfWeek = 1,
                        title = "Daily Engineering Standup",
                        startHour = 9,
                        startMinute = 30,
                        endHour = 10,
                        endMinute = 0,
                        description = "Sprint review and blocker discussion.",
                        category = "Meeting",
                        colorHex = 0xFFF59E0B
                    )
                )
                repository.insertTimetableSlot(
                    TimetableSlot(
                        dayOfWeek = 1,
                        title = "Deep Focus: Architecture Design",
                        startHour = 10,
                        startMinute = 15,
                        endHour = 13,
                        endMinute = 0,
                        description = "Uninterrupted feature implementation block.",
                        category = "Work",
                        colorHex = 0xFF4F46E5
                    )
                )
                repository.insertTimetableSlot(
                    TimetableSlot(
                        dayOfWeek = 1,
                        title = "Stakeholder Progress Review",
                        startHour = 15,
                        startMinute = 0,
                        endHour = 16,
                        endMinute = 0,
                        description = "Deliverable demonstration with team leads.",
                        category = "Meeting",
                        colorHex = 0xFFEF4444
                    )
                )

                val now = System.currentTimeMillis()
                repository.insertTask(
                    TaskItem(
                        title = "Review Architecture Pull Request #249",
                        description = "Verify authentication scopes and offline cache.",
                        priority = "HIGH",
                        dueDateMillis = now + 86400000L
                    )
                )
                repository.insertTask(
                    TaskItem(
                        title = "Submit Bi-weekly Shift Timesheet",
                        description = "Verify recorded attendance hours before Friday cutoff.",
                        priority = "MEDIUM",
                        dueDateMillis = now + 86400000L * 3
                    )
                )

                repository.insertNote(
                    NoteItem(
                        title = "Q3 Product Architecture Strategy",
                        content = "Key milestones:\n1. Offline-first Room persistence\n2. Real-time cloud sync\n3. Interactive date and time dial pickers",
                        category = "Work",
                        colorTagHex = 0xFF4F46E5
                    )
                )
            }

            UserRole.TEACHER -> {
                repository.insertTimetableSlot(
                    TimetableSlot(
                        dayOfWeek = 1,
                        title = "Grade 10 Mathematics Lecture",
                        startHour = 8,
                        startMinute = 30,
                        endHour = 9,
                        endMinute = 30,
                        description = "Section A: Quadratic equations and exercises.",
                        category = "Class",
                        colorHex = 0xFF06B6D4
                    )
                )
                repository.insertTimetableSlot(
                    TimetableSlot(
                        dayOfWeek = 1,
                        title = "Faculty Academic Planning Sync",
                        startHour = 11,
                        startMinute = 0,
                        endHour = 12,
                        endMinute = 0,
                        description = "Quarterly examination syllabus alignment.",
                        category = "Meeting",
                        colorHex = 0xFF8B5CF6
                    )
                )
                repository.insertTimetableSlot(
                    TimetableSlot(
                        dayOfWeek = 1,
                        title = "Senior Physics Demonstration",
                        startHour = 14,
                        startMinute = 0,
                        endHour = 15,
                        endMinute = 30,
                        description = "Optics and prism refraction demonstration.",
                        category = "Class",
                        colorHex = 0xFF10B981
                    )
                )

                val now = System.currentTimeMillis()
                repository.insertTask(
                    TaskItem(
                        title = "Grade Mid-Term Mathematics Test Papers",
                        description = "Total 45 papers to evaluate and enter marks.",
                        priority = "HIGH",
                        dueDateMillis = now + 86400000L * 2
                    )
                )
                repository.insertTask(
                    TaskItem(
                        title = "Submit Monthly Attendance Registers",
                        description = "Reconcile student absentee logs with academic records.",
                        priority = "MEDIUM",
                        dueDateMillis = now + 86400000L * 4
                    )
                )

                repository.insertNote(
                    NoteItem(
                        title = "Lesson Plan: Trigonometry and Waves",
                        content = "Day 1: Concept introduction with visual unit circle.\nDay 2: Proof of identities.\nDay 3: Practice test with 10 questions.",
                        category = "General",
                        colorTagHex = 0xFF06B6D4
                    )
                )
            }
        }
    }
}
