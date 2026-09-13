package com.example.data.repository

import com.example.data.local.AttendanceDao
import com.example.data.local.AttendanceRecord
import com.example.data.local.NoteDao
import com.example.data.local.NoteItem
import com.example.data.local.TaskDao
import com.example.data.local.TaskItem
import com.example.data.local.TimetableDao
import com.example.data.local.TimetableSlot
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ProductivityRepository(
    private val attendanceDao: AttendanceDao,
    private val timetableDao: TimetableDao,
    private val taskDao: TaskDao,
    private val noteDao: NoteDao
) {
    // --- Attendance Operations ---
    val allAttendanceRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllRecords()
    val activeAttendanceRecord: Flow<AttendanceRecord?> = attendanceDao.getActiveRecordFlow()

    suspend fun checkIn(note: String = ""): Long {
        val now = System.currentTimeMillis()
        val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val active = attendanceDao.getActiveRecord()
        if (active != null) {
            // Already checked in
            return active.id
        }
        val newRecord = AttendanceRecord(
            dateIso = todayIso,
            checkInTimeMillis = now,
            checkOutTimeMillis = null,
            note = note
        )
        return attendanceDao.insert(newRecord)
    }

    suspend fun checkOut(recordId: Long? = null) {
        val now = System.currentTimeMillis()
        val recordToClose = if (recordId != null) {
            val all = attendanceDao.getActiveRecord()
            if (all?.id == recordId) all else null
        } else {
            attendanceDao.getActiveRecord()
        }

        recordToClose?.let {
            attendanceDao.update(it.copy(checkOutTimeMillis = now))
        }
    }

    suspend fun deleteAttendanceRecord(id: Long) {
        attendanceDao.deleteById(id)
    }

    suspend fun addManualAttendance(dateIso: String, checkIn: Long, checkOut: Long?, note: String = "") {
        attendanceDao.insert(
            AttendanceRecord(
                dateIso = dateIso,
                checkInTimeMillis = checkIn,
                checkOutTimeMillis = checkOut,
                note = note
            )
        )
    }

    // --- Timetable Operations ---
    val allTimetableSlots: Flow<List<TimetableSlot>> = timetableDao.getAllSlots()

    fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlot>> {
        return timetableDao.getSlotsForDay(dayOfWeek)
    }

    suspend fun insertTimetableSlot(slot: TimetableSlot): Long {
        return timetableDao.insert(slot)
    }

    suspend fun updateTimetableSlot(slot: TimetableSlot) {
        timetableDao.update(slot)
    }

    suspend fun deleteTimetableSlot(id: Long) {
        timetableDao.deleteById(id)
    }

    // --- Task Operations ---
    val allTasks: Flow<List<TaskItem>> = taskDao.getAllTasks()
    val pendingTasks: Flow<List<TaskItem>> = taskDao.getPendingTasks()
    val completedTasks: Flow<List<TaskItem>> = taskDao.getCompletedTasks()

    suspend fun insertTask(task: TaskItem): Long {
        return taskDao.insert(task)
    }

    suspend fun updateTask(task: TaskItem) {
        taskDao.update(task)
    }

    suspend fun setTaskCompletion(id: Long, isCompleted: Boolean) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        taskDao.setTaskCompletion(id, isCompleted, completedAt)
    }

    suspend fun deleteTask(id: Long) {
        taskDao.deleteById(id)
    }

    // --- Note Operations ---
    val allNotes: Flow<List<NoteItem>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<NoteItem>> {
        return if (query.isBlank()) {
            noteDao.getAllNotes()
        } else {
            noteDao.searchNotes(query.trim())
        }
    }

    suspend fun insertNote(note: NoteItem): Long {
        return noteDao.insert(note)
    }

    suspend fun updateNote(note: NoteItem) {
        noteDao.update(note.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun deleteNote(id: Long) {
        noteDao.deleteById(id)
    }
}
