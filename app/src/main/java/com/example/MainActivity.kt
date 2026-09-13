package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.data.local.AppDatabase
import com.example.data.repository.ProductivityRepository
import com.example.ui.MainScreen
import com.example.ui.attendance.AttendanceViewModel
import com.example.ui.notes.NotesViewModel
import com.example.ui.tasks.TasksViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.timetable.TimetableViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = ProductivityRepository(
            attendanceDao = database.attendanceDao(),
            timetableDao = database.timetableDao(),
            taskDao = database.taskDao(),
            noteDao = database.noteDao()
        )

        val attendanceViewModel = AttendanceViewModel(repository)
        val timetableViewModel = TimetableViewModel(repository)
        val tasksViewModel = TasksViewModel(repository)
        val notesViewModel = NotesViewModel(repository)

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    attendanceViewModel = attendanceViewModel,
                    timetableViewModel = timetableViewModel,
                    tasksViewModel = tasksViewModel,
                    notesViewModel = notesViewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}
