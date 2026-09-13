package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateIso: String, // e.g. "2026-08-21"
    val checkInTimeMillis: Long, // timestamp
    val checkOutTimeMillis: Long? = null, // timestamp if checked out
    val note: String = ""
)

@Entity(tableName = "timetable_slots")
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, ..., 7 = Sunday
    val title: String,
    val startHour: Int, // 0..23
    val startMinute: Int, // 0..59
    val endHour: Int, // 0..23
    val endMinute: Int, // 0..59
    val description: String = "",
    val category: String = "General", // e.g. "Class", "Meeting", "Work", "Study", "Fitness"
    val colorHex: Long = 0xFF4F46E5 // Default Indigo
)

enum class TaskPriority(val label: String, val colorValue: Long) {
    HIGH("High", 0xFFEF4444),     // Red
    MEDIUM("Medium", 0xFFF59E0B), // Amber
    LOW("Low", 0xFF10B981)        // Emerald
}

@Entity(tableName = "task_items")
data class TaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: String = TaskPriority.MEDIUM.name,
    val dueDateMillis: Long,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "note_items")
data class NoteItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "General",
    val colorTagHex: Long = 0xFF6366F1,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val createdAtMillis: Long = System.currentTimeMillis()
)
