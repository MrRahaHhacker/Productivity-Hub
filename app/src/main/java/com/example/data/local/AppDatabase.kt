package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Database(
    entities = [
        AttendanceRecord::class,
        TimetableSlot::class,
        TaskItem::class,
        NoteItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
    abstract fun timetableDao(): TimetableDao
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "productivity_hub.db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val now = System.currentTimeMillis()
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE

            // 1. Initial Attendance Records (for current month calculation)
            val attDao = database.attendanceDao()
            // Log past 4 days of records in current month
            for (i in 1..4) {
                val pastDate = today.minusDays(i.toLong())
                // Skip Sunday if i happens to fall on weekend
                val checkIn = now - (i * 24L * 60 * 60 * 1000) + (9 * 60 * 60 * 1000) // ~9:00 AM
                val checkOut = checkIn + (8L * 60 * 60 * 1000) + (15 * 60 * 1000) // ~8h 15m
                attDao.insert(
                    AttendanceRecord(
                        dateIso = pastDate.format(formatter),
                        checkInTimeMillis = checkIn,
                        checkOutTimeMillis = checkOut,
                        note = "Regular work hours"
                    )
                )
            }

            // 2. Initial Timetable Slots (Monday to Friday + Weekend sample)
            val timeDao = database.timetableDao()
            // Monday
            timeDao.insert(
                TimetableSlot(
                    dayOfWeek = 1,
                    title = "Product Strategy & Standup",
                    startHour = 9,
                    startMinute = 0,
                    endHour = 10,
                    endMinute = 0,
                    description = "Review sprint objectives and team blockers",
                    category = "Meeting",
                    colorHex = 0xFF4F46E5
                )
            )
            timeDao.insert(
                TimetableSlot(
                    dayOfWeek = 1,
                    title = "Deep Focus: Core Architecture",
                    startHour = 10,
                    startMinute = 30,
                    endHour = 12,
                    endMinute = 30,
                    description = "Compose state refactoring and repository caching",
                    category = "Work",
                    colorHex = 0xFF0284C7
                )
            )
            timeDao.insert(
                TimetableSlot(
                    dayOfWeek = 1,
                    title = "AI Research & Learning",
                    startHour = 14,
                    startMinute = 0,
                    endHour = 15,
                    endMinute = 30,
                    description = "Study edge inference & vector indexing",
                    category = "Study",
                    colorHex = 0xFF7C3AED
                )
            )
            // Tuesday
            timeDao.insert(
                TimetableSlot(
                    dayOfWeek = 2,
                    title = "Client Sync & Review",
                    startHour = 11,
                    startMinute = 0,
                    endHour = 12,
                    endMinute = 0,
                    description = "Demo productivity milestones",
                    category = "Meeting",
                    colorHex = 0xFF0D9488
                )
            )
            timeDao.insert(
                TimetableSlot(
                    dayOfWeek = 2,
                    title = "Feature Development",
                    startHour = 13,
                    startMinute = 30,
                    endHour = 16,
                    endMinute = 0,
                    description = "Timetable interactive timeline widgets",
                    category = "Work",
                    colorHex = 0xFF2563EB
                )
            )
            // Wednesday
            timeDao.insert(
                TimetableSlot(
                    dayOfWeek = 3,
                    title = "UI/UX Design Alignment",
                    startHour = 10,
                    startMinute = 0,
                    endHour = 11,
                    endMinute = 30,
                    description = "Refining Material 3 color harmonies and animations",
                    category = "Design",
                    colorHex = 0xFFDB2777
                )
            )
            timeDao.insert(
                TimetableSlot(
                    dayOfWeek = 3,
                    title = "Code Review & Refactoring",
                    startHour = 14,
                    startMinute = 0,
                    endHour = 16,
                    endMinute = 30,
                    description = "Room DAO performance optimization",
                    category = "Work",
                    colorHex = 0xFF4F46E5
                )
            )
            // Thursday
            timeDao.insert(
                TimetableSlot(
                    dayOfWeek = 4,
                    title = "Project Milestone Check",
                    startHour = 9,
                    startMinute = 30,
                    endHour = 11,
                    endMinute = 0,
                    description = "Evaluate release checklist",
                    category = "Meeting",
                    colorHex = 0xFFEA580C
                )
            )
            // Friday
            timeDao.insert(
                TimetableSlot(
                    dayOfWeek = 5,
                    title = "Weekly Retrospective",
                    startHour = 15,
                    startMinute = 0,
                    endHour = 16,
                    endMinute = 0,
                    description = "Celebrate wins and identify action items",
                    category = "Review",
                    colorHex = 0xFF16A34A
                )
            )
            // Saturday
            timeDao.insert(
                TimetableSlot(
                    dayOfWeek = 6,
                    title = "Fitness & Recovery Session",
                    startHour = 8,
                    startMinute = 0,
                    endHour = 9,
                    endMinute = 30,
                    description = "Cardio workout and mobility stretching",
                    category = "Fitness",
                    colorHex = 0xFFE11D48
                )
            )
            // Sunday
            timeDao.insert(
                TimetableSlot(
                    dayOfWeek = 7,
                    title = "Weekly Planning & Habit Audit",
                    startHour = 18,
                    startMinute = 0,
                    endHour = 19,
                    endMinute = 0,
                    description = "Set goals and organize priority tasks for next week",
                    category = "Planning",
                    colorHex = 0xFF9333EA
                )
            )

            // 3. Initial Task Items
            val taskDao = database.taskDao()
            taskDao.insert(
                TaskItem(
                    title = "Finalize Q3 roadmap documentation",
                    description = "Include metrics, deliverables, and resource allocation",
                    priority = TaskPriority.HIGH.name,
                    dueDateMillis = now + (24L * 60 * 60 * 1000), // Tomorrow
                    isCompleted = false
                )
            )
            taskDao.insert(
                TaskItem(
                    title = "Review pull request for Room schema migration",
                    description = "Verify fallback destructive migration strategy and entity models",
                    priority = TaskPriority.HIGH.name,
                    dueDateMillis = now + (4 * 60 * 60 * 1000), // Today
                    isCompleted = false
                )
            )
            taskDao.insert(
                TaskItem(
                    title = "Schedule quarterly 1-on-1 feedback sessions",
                    description = "Send calendar invites to team leads",
                    priority = TaskPriority.MEDIUM.name,
                    dueDateMillis = now + (2 * 24L * 60 * 60 * 1000),
                    isCompleted = false
                )
            )
            taskDao.insert(
                TaskItem(
                    title = "Audit workspace stationery and digital subscriptions",
                    description = "Cancel redundant cloud dev tools",
                    priority = TaskPriority.LOW.name,
                    dueDateMillis = now + (5 * 24L * 60 * 60 * 1000),
                    isCompleted = false
                )
            )
            taskDao.insert(
                TaskItem(
                    title = "Complete Android Material 3 theme migration",
                    description = "Unified dynamic color scheme with dark mode support",
                    priority = TaskPriority.MEDIUM.name,
                    dueDateMillis = now - (24L * 60 * 60 * 1000),
                    isCompleted = true,
                    completedAtMillis = now - (12L * 60 * 60 * 1000)
                )
            )
            taskDao.insert(
                TaskItem(
                    title = "Prepare sprint demo slides",
                    description = "Captured interactive screenshots and demo walkthroughs",
                    priority = TaskPriority.HIGH.name,
                    dueDateMillis = now - (2 * 24L * 60 * 60 * 1000),
                    isCompleted = true,
                    completedAtMillis = now - (20L * 60 * 60 * 1000)
                )
            )

            // 4. Initial Notes
            val noteDao = database.noteDao()
            noteDao.insert(
                NoteItem(
                    title = "Productivity Principles & Daily Flow",
                    content = "1. Focus on high-leverage outcomes first.\n2. Protect deep work blocks during morning energy peaks.\n3. Track attendance consistently for personal accountability.\n4. Conduct quick 5-minute evening audits.",
                    category = "Strategy",
                    colorTagHex = 0xFF4F46E5,
                    updatedAtMillis = now - (10L * 60 * 60 * 1000)
                )
            )
            noteDao.insert(
                NoteItem(
                    title = "Architecture Decision: Jetpack Compose M3",
                    content = "Standardized state hoisting patterns across all 4 modules. Utilizing Kotlin Flow with StateFlow to maintain reactive UI updates without unnecessary recompositions.",
                    category = "Engineering",
                    colorTagHex = 0xFF059669,
                    updatedAtMillis = now - (5L * 60 * 60 * 1000)
                )
            )
            noteDao.insert(
                NoteItem(
                    title = "Book Notes: Atomic Habits by James Clear",
                    content = "Make good habits obvious, attractive, easy, and satisfying. The 1% compounding rule produces remarkable long-term dividends in focus and output.",
                    category = "Reading",
                    colorTagHex = 0xFFD97706,
                    updatedAtMillis = now - (2L * 24L * 60 * 60 * 1000)
                )
            )
            noteDao.insert(
                NoteItem(
                    title = "Meeting Takeaways: Product Design Sync",
                    content = "Agreed on minimalist cards with prominent action anchors. Ensure high-contrast dark theme readability for late-night productivity planning.",
                    category = "Work",
                    colorTagHex = 0xFFDB2777,
                    updatedAtMillis = now - (3L * 24L * 60 * 60 * 1000)
                )
            )
        }
    }
}
