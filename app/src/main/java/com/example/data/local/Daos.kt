package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY checkInTimeMillis DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getRecordForDate(dateIso: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE checkOutTimeMillis IS NULL ORDER BY checkInTimeMillis DESC LIMIT 1")
    fun getActiveRecordFlow(): Flow<AttendanceRecord?>

    @Query("SELECT * FROM attendance_records WHERE checkOutTimeMillis IS NULL ORDER BY checkInTimeMillis DESC LIMIT 1")
    suspend fun getActiveRecord(): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AttendanceRecord): Long

    @Update
    suspend fun update(record: AttendanceRecord)

    @Delete
    suspend fun delete(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    fun getAllSlots(): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY startHour ASC, startMinute ASC")
    fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(slot: TimetableSlot): Long

    @Update
    suspend fun update(slot: TimetableSlot)

    @Delete
    suspend fun delete(slot: TimetableSlot)

    @Query("DELETE FROM timetable_slots WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_items ORDER BY isCompleted ASC, dueDateMillis ASC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM task_items WHERE isCompleted = 0 ORDER BY dueDateMillis ASC")
    fun getPendingTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM task_items WHERE isCompleted = 1 ORDER BY completedAtMillis DESC")
    fun getCompletedTasks(): Flow<List<TaskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskItem): Long

    @Update
    suspend fun update(task: TaskItem)

    @Delete
    suspend fun delete(task: TaskItem)

    @Query("DELETE FROM task_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE task_items SET isCompleted = :isCompleted, completedAtMillis = :completedAt WHERE id = :id")
    suspend fun setTaskCompletion(id: Long, isCompleted: Boolean, completedAt: Long?)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM note_items ORDER BY updatedAtMillis DESC")
    fun getAllNotes(): Flow<List<NoteItem>>

    @Query("SELECT * FROM note_items WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteItem?

    @Query("SELECT * FROM note_items WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAtMillis DESC")
    fun searchNotes(query: String): Flow<List<NoteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteItem): Long

    @Update
    suspend fun update(note: NoteItem)

    @Delete
    suspend fun delete(note: NoteItem)

    @Query("DELETE FROM note_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
