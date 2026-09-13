package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import com.example.ui.auth.AuthenticationViewModel
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.sync.GoogleSyncManager
import com.example.data.sync.UserRole
import com.example.ui.attendance.AttendanceScreen
import com.example.ui.attendance.AttendanceViewModel
import com.example.ui.notes.NotesScreen
import com.example.ui.notes.NotesViewModel
import com.example.ui.profile.RoleTemplateDialog
import com.example.ui.reminder.ReminderDialog
import com.example.ui.tasks.TasksScreen
import com.example.ui.tasks.TasksViewModel
import com.example.ui.timetable.TimetableScreen
import com.example.ui.timetable.TimetableViewModel
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import kotlinx.coroutines.launch

enum class NavigationDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    ATTENDANCE(
        title = "Attendance",
        selectedIcon = Icons.Filled.AccessTime,
        unselectedIcon = Icons.Outlined.AccessTime,
        testTag = "nav_attendance_tab"
    ),
    TIMETABLE(
        title = "Timetable",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        testTag = "nav_timetable_tab"
    ),
    TASKS(
        title = "Tasks",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle,
        testTag = "nav_tasks_tab"
    ),
    NOTES(
        title = "Notes",
        selectedIcon = Icons.Filled.NoteAlt,
        unselectedIcon = Icons.Outlined.NoteAlt,
        testTag = "nav_notes_tab"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    attendanceViewModel: AttendanceViewModel,
    timetableViewModel: TimetableViewModel,
    tasksViewModel: TasksViewModel,
    notesViewModel: NotesViewModel,
    syncManager: GoogleSyncManager,
    authenticationViewModel: AuthenticationViewModel? = null,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(NavigationDestination.ATTENDANCE) }
    var showRoleTemplateDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    val userProfile by syncManager.userProfile.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showRoleTemplateDialog) {
        RoleTemplateDialog(
            syncManager = syncManager,
            onDismiss = { showRoleTemplateDialog = false },
            onRoleApplied = { role, withTemplate ->
                scope.launch {
                    val msg = if (withTemplate) {
                        "Applied ${role.title} & loaded starter timetable, tasks and notes!"
                    } else {
                        "Switched active role to ${role.title}"
                    }
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
    }

    if (showReminderDialog) {
        ReminderDialog(
            userRole = userProfile.role,
            onDismiss = { showReminderDialog = false },
            onMessage = { msg ->
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
    }

    val roleIcon = when (userProfile.role) {
        UserRole.STUDENT -> Icons.Default.School
        UserRole.PROFESSIONAL -> Icons.Default.BusinessCenter
        UserRole.TEACHER -> Icons.Default.MenuBook
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showRoleTemplateDialog = true }
                            .testTag("top_bar_role_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = roleIcon,
                                contentDescription = userProfile.role.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = userProfile.role.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Choose role template",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = "Productivity Hub",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    // Notification Reminder Action
                    IconButton(
                        onClick = { showReminderDialog = true },
                        modifier = Modifier.testTag("notification_reminder_button")
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notification Reminders",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            // Subtle active indicator badge
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleDarkTheme,
                        modifier = Modifier.testTag("dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier
                    .drawBehind {
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .testTag("main_navigation_bar")
            ) {
                NavigationDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(destination.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    val isForward = targetState.ordinal > initialState.ordinal
                    if (isForward) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "ScreenTransition"
            ) { destination ->
                when (destination) {
                    NavigationDestination.ATTENDANCE -> AttendanceScreen(
                        viewModel = attendanceViewModel
                    )
                    NavigationDestination.TIMETABLE -> TimetableScreen(
                        viewModel = timetableViewModel
                    )
                    NavigationDestination.TASKS -> TasksScreen(
                        viewModel = tasksViewModel
                    )
                    NavigationDestination.NOTES -> NotesScreen(
                        viewModel = notesViewModel
                    )
                }
            }
        }
    }
}
