package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Productivity Hub", appName)
  }

  @Test
  fun `test google storage json serialization and validation`() {
    val root = org.json.JSONObject().apply {
      put("version", 1)
      put("app", "ProductivityHub")
      put("user_email", "beirshad32@gmail.com")
      put("role", "STUDENT")
      put("backup_timestamp", System.currentTimeMillis())

      val attendance = org.json.JSONArray().apply {
        put(org.json.JSONObject().apply {
          put("dateIso", "2026-09-13")
          put("checkIn", 1726210800000L)
          put("checkOut", 1726232400000L)
          put("note", "Morning classes")
        })
      }
      put("attendance", attendance)

      val timetable = org.json.JSONArray().apply {
        put(org.json.JSONObject().apply {
          put("dayOfWeek", 1)
          put("title", "Computer Networks")
          put("startHour", 9)
          put("startMin", 0)
          put("endHour", 10)
          put("endMin", 30)
          put("category", "Lecture")
        })
      }
      put("timetable", timetable)
    }

    assertEquals("beirshad32@gmail.com", root.getString("user_email"))
    assertEquals(1, root.getInt("version"))
    val jsonString = root.toString(2)
    org.junit.Assert.assertTrue(jsonString.contains("Computer Networks"))
  }

  @Test
  fun `test authentication view model initial state and role selection`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val authViewModel = com.example.ui.auth.AuthenticationViewModel(application)

    org.junit.Assert.assertEquals(com.example.ui.auth.AuthUiState.Idle, authViewModel.uiState.value)
    org.junit.Assert.assertEquals(com.example.data.sync.UserRole.STUDENT, authViewModel.selectedRole.value)

    authViewModel.selectRole(com.example.data.sync.UserRole.PROFESSIONAL)
    org.junit.Assert.assertEquals(com.example.data.sync.UserRole.PROFESSIONAL, authViewModel.selectedRole.value)

    val demoAccount = com.example.data.sync.GoogleAccountItem(
      email = "test@gmail.com",
      displayName = "Test User",
      role = com.example.data.sync.UserRole.PROFESSIONAL
    )
    authViewModel.signInWithAccountDirectly(demoAccount)
    val state = authViewModel.uiState.value
    org.junit.Assert.assertTrue(state is com.example.ui.auth.AuthUiState.Success)
    org.junit.Assert.assertEquals("test@gmail.com", (state as com.example.ui.auth.AuthUiState.Success).account.email)

    authViewModel.signOut()
    org.junit.Assert.assertEquals(com.example.ui.auth.AuthUiState.Idle, authViewModel.uiState.value)
  }

  @Test
  fun `test user roles properties and starter template loading`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = com.example.data.local.AppDatabase.getDatabase(context, this)
    val repository = com.example.data.repository.ProductivityRepository(
      database.attendanceDao(),
      database.timetableDao(),
      database.taskDao(),
      database.noteDao()
    )
    val syncManager = com.example.data.sync.GoogleSyncManager(context, repository)

    org.junit.Assert.assertEquals("Student", com.example.data.sync.UserRole.STUDENT.title)
    org.junit.Assert.assertEquals("Job Professional", com.example.data.sync.UserRole.PROFESSIONAL.title)
    org.junit.Assert.assertEquals("Teacher / Educator", com.example.data.sync.UserRole.TEACHER.title)

    syncManager.updateRole(com.example.data.sync.UserRole.TEACHER)
    org.junit.Assert.assertEquals(com.example.data.sync.UserRole.TEACHER, syncManager.userProfile.value.role)

    syncManager.loadRoleStarterTemplate(com.example.data.sync.UserRole.TEACHER)
    val tasks = repository.allTasks
    org.junit.Assert.assertNotNull(tasks)
  }

  @Test
  fun `test notification helper settings persistence and channel setup`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.NotificationHelper.initNotificationChannel(context)

    val defaultSettings = com.example.util.NotificationHelper.getSettings(context)
    org.junit.Assert.assertTrue(defaultSettings.checkInEnabled)

    val updatedSettings = defaultSettings.copy(
      checkInHour = 8,
      checkInMinute = 45,
      checkOutHour = 18,
      checkOutMinute = 15,
      timetableAlertsEnabled = true
    )
    com.example.util.NotificationHelper.saveSettings(context, updatedSettings)

    val reloaded = com.example.util.NotificationHelper.getSettings(context)
    org.junit.Assert.assertEquals(8, reloaded.checkInHour)
    org.junit.Assert.assertEquals(45, reloaded.checkInMinute)
    org.junit.Assert.assertEquals(18, reloaded.checkOutHour)
    org.junit.Assert.assertEquals(15, reloaded.checkOutMinute)
    org.junit.Assert.assertEquals("08:45 AM", reloaded.checkInTimeFormatted)
    org.junit.Assert.assertEquals("06:15 PM", reloaded.checkOutTimeFormatted)
  }
}
