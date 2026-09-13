package com.example.ui.timetable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TimetableSlot
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.PrimaryLavendar
import com.example.ui.theme.StatusActiveGreen
import com.example.util.DateTimeUtils
import java.time.LocalDate
import java.time.LocalDateTime

private val DAYS_OF_WEEK = listOf(
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat",
    7 to "Sun"
)

private val FULL_DAY_NAMES = listOf(
    1 to "Monday",
    2 to "Tuesday",
    3 to "Wednesday",
    4 to "Thursday",
    5 to "Friday",
    6 to "Saturday",
    7 to "Sunday"
)

private val CATEGORIES = listOf(
    "Work" to 0xFF2563EB,
    "Meeting" to 0xFF4F46E5,
    "Study" to 0xFF7C3AED,
    "Design" to 0xFFDB2777,
    "Fitness" to 0xFFE11D48,
    "Review" to 0xFF16A34A,
    "Planning" to 0xFF0D9488,
    "General" to 0xFF64748B
)

@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var slotToEdit by remember { mutableStateOf<TimetableSlot?>(null) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val todayDayOfWeek = LocalDate.now().dayOfWeek.value

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Title and Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Timetable Planner",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Weekly schedule & real-time ongoing slot detector",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Weekly Day Selector Bar (Mon - Sun)
            item {
                WeeklyDaySelectorBar(
                    selectedDay = uiState.selectedDayOfWeek,
                    todayDayOfWeek = todayDayOfWeek,
                    allSlots = uiState.allSlots,
                    onSelectDay = { viewModel.selectDay(it) },
                    modifier = Modifier.testTag("weekly_day_selector")
                )
            }

            // Day Header with count
            item {
                val fullDayName = FULL_DAY_NAMES.find { it.first == uiState.selectedDayOfWeek }?.second ?: "Day"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.selectedDayOfWeek == todayDayOfWeek) "$fullDayName (Today)" else fullDayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = "${uiState.currentDaySlots.size} slots",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Timeline Slots
            if (uiState.currentDaySlots.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No scheduled slots for this day",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the '+' button below to add classes, meetings, or focus blocks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.currentDaySlots,
                    key = { it.id }
                ) { slot ->
                    val isOngoing = DateTimeUtils.isSlotCurrent(
                        dayOfWeek = slot.dayOfWeek,
                        startHour = slot.startHour,
                        startMinute = slot.startMinute,
                        endHour = slot.endHour,
                        endMinute = slot.endMinute
                    )

                    TimetableSlotCard(
                        slot = slot,
                        isOngoing = isOngoing,
                        onEdit = { slotToEdit = slot },
                        onDelete = { viewModel.deleteSlot(slot.id) },
                        modifier = Modifier.testTag("timetable_slot_${slot.id}")
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(88.dp)) // padding for bottom nav and fab
            }
        }

        // FAB to add slot
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 80.dp)
                .testTag("add_timetable_slot_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add slot")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        )
    }

    // Add Slot Dialog
    if (showAddDialog) {
        SlotFormDialog(
            initialDayOfWeek = uiState.selectedDayOfWeek,
            onDismiss = { showAddDialog = false },
            onSave = { day, title, sHour, sMin, eHour, eMin, desc, cat, color ->
                viewModel.addSlot(day, title, sHour, sMin, eHour, eMin, desc, cat, color)
                showAddDialog = false
            }
        )
    }

    // Edit Slot Dialog
    slotToEdit?.let { slot ->
        SlotFormDialog(
            slotToEdit = slot,
            initialDayOfWeek = slot.dayOfWeek,
            onDismiss = { slotToEdit = null },
            onSave = { day, title, sHour, sMin, eHour, eMin, desc, cat, color ->
                viewModel.updateSlot(
                    slot.copy(
                        dayOfWeek = day,
                        title = title,
                        startHour = sHour,
                        startMinute = sMin,
                        endHour = eHour,
                        endMinute = eMin,
                        description = desc,
                        category = cat,
                        colorHex = color
                    )
                )
                slotToEdit = null
            }
        )
    }
}

@Composable
private fun WeeklyDaySelectorBar(
    selectedDay: Int,
    todayDayOfWeek: Int,
    allSlots: List<TimetableSlot>,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DAYS_OF_WEEK.forEach { (dayInt, dayLabel) ->
                val isSelected = selectedDay == dayInt
                val isToday = todayDayOfWeek == dayInt
                val slotCount = allSlots.count { it.dayOfWeek == dayInt }

                val bgColor by animateColorAsState(
                    targetValue = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else -> Color.Transparent
                    },
                    label = "dayBg"
                )

                val textColor = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { onSelectDay(dayInt) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Small indicator dot or count badge
                        if (slotCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableSlotCard(
    slot: TimetableSlot,
    isOngoing: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ongoingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val slotColor = Color(slot.colorHex)
    val startTimeFormatted = DateTimeUtils.formatTimeFromHourMinute(slot.startHour, slot.startMinute)
    val endTimeFormatted = DateTimeUtils.formatTimeFromHourMinute(slot.endHour, slot.endMinute)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isOngoing) {
            BorderStroke(2.dp, StatusActiveGreen)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isOngoing) {
                        Brush.horizontalGradient(
                            listOf(
                                AccentEmerald.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                slotColor.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    }
                )
                .padding(16.dp)
        ) {
            // Top Bar: Time and Ongoing Flag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOngoing) AccentEmerald else slotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$startTimeFormatted — $endTimeFormatted",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isOngoing) AccentEmerald else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isOngoing) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentEmerald,
                        modifier = Modifier.scale(pulseScale)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "NOW HAPPENING",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = slotColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = slot.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = slotColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Description
            Text(
                text = slot.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (slot.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = slot.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Shortcuts (Edit & Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit slot",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete slot",
                        tint = AccentRose.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotFormDialog(
    slotToEdit: TimetableSlot? = null,
    initialDayOfWeek: Int,
    onDismiss: () -> Unit,
    onSave: (
        day: Int,
        title: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        description: String,
        category: String,
        colorHex: Long
    ) -> Unit
) {
    var title by remember { mutableStateOf(slotToEdit?.title ?: "") }
    var selectedDay by remember { mutableIntStateOf(slotToEdit?.dayOfWeek ?: initialDayOfWeek) }
    var startHourStr by remember { mutableStateOf(slotToEdit?.startHour?.toString() ?: "9") }
    var startMinStr by remember { mutableStateOf(String.format("%02d", slotToEdit?.startMinute ?: 0)) }
    var endHourStr by remember { mutableStateOf(slotToEdit?.endHour?.toString() ?: "10") }
    var endMinStr by remember { mutableStateOf(String.format("%02d", slotToEdit?.endMinute ?: 30)) }
    var description by remember { mutableStateOf(slotToEdit?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(slotToEdit?.category ?: "Work") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = if (slotToEdit != null) "Edit Schedule Slot" else "Add Schedule Slot",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title / Subject *") },
                        placeholder = { Text("e.g. Deep Work, Mobile Architecture") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Day Selection Row
                item {
                    Text(
                        text = "Day of Week",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(DAYS_OF_WEEK) { (dayInt, label) ->
                            val isSel = selectedDay == dayInt
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedDay = dayInt }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Start & End Time Input Rows
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = startHourStr,
                            onValueChange = { startHourStr = it },
                            label = { Text("Start Hr (0-23)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = startMinStr,
                            onValueChange = { startMinStr = it },
                            label = { Text("Start Min") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = endHourStr,
                            onValueChange = { endHourStr = it },
                            label = { Text("End Hr (0-23)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endMinStr,
                            onValueChange = { endMinStr = it },
                            label = { Text("End Min") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Category selection chips
                item {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(CATEGORIES) { (cat, colorVal) ->
                            val isSel = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(colorVal) else Color(colorVal).copy(alpha = 0.15f),
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSel) Color.White else Color(colorVal),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Optional Description") },
                        placeholder = { Text("Room, meeting link, or goals") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    val sHour = (startHourStr.toIntOrNull() ?: 9).coerceIn(0, 23)
                                    val sMin = (startMinStr.toIntOrNull() ?: 0).coerceIn(0, 59)
                                    val eHour = (endHourStr.toIntOrNull() ?: (sHour + 1)).coerceIn(0, 23)
                                    val eMin = (endMinStr.toIntOrNull() ?: sMin).coerceIn(0, 59)
                                    val chosenColor = CATEGORIES.find { it.first == selectedCategory }?.second ?: 0xFF4F46E5

                                    onSave(
                                        selectedDay,
                                        title.trim(),
                                        sHour,
                                        sMin,
                                        eHour,
                                        eMin,
                                        description.trim(),
                                        selectedCategory,
                                        chosenColor
                                    )
                                }
                            },
                            modifier = Modifier.testTag("save_timetable_slot_button")
                        ) {
                            Text("Save Slot")
                        }
                    }
                }
            }
        }
    }
}
