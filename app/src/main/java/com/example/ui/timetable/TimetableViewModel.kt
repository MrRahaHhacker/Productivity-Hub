package com.example.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TimetableSlot
import com.example.data.repository.ProductivityRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class TimetableUiState(
    val selectedDayOfWeek: Int = LocalDate.now().dayOfWeek.value, // 1 = Mon .. 7 = Sun
    val allSlots: List<TimetableSlot> = emptyList(),
    val currentDaySlots: List<TimetableSlot> = emptyList(),
    val currentDateTime: LocalDateTime = LocalDateTime.now(),
    val message: String? = null
)

class TimetableViewModel(
    private val repository: ProductivityRepository
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek.value)
    private val _currentDateTime = MutableStateFlow(LocalDateTime.now())
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<TimetableUiState> = combine(
        repository.allTimetableSlots,
        _selectedDay,
        _currentDateTime,
        _message
    ) { slots, day, time, msg ->
        val filtered = slots.filter { it.dayOfWeek == day }
            .sortedWith(compareBy({ it.startHour }, { it.startMinute }))
        TimetableUiState(
            selectedDayOfWeek = day,
            allSlots = slots,
            currentDaySlots = filtered,
            currentDateTime = time,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimetableUiState()
    )

    init {
        // Ticking loop for real-time clock checks
        viewModelScope.launch {
            while (isActive) {
                _currentDateTime.value = LocalDateTime.now()
                delay(10000) // update every 10 seconds for real-time timeline highlight
            }
        }
    }

    fun selectDay(dayOfWeek: Int) {
        _selectedDay.value = dayOfWeek
    }

    fun addSlot(
        dayOfWeek: Int,
        title: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        description: String,
        category: String,
        colorHex: Long
    ) {
        viewModelScope.launch {
            val slot = TimetableSlot(
                dayOfWeek = dayOfWeek,
                title = title,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                description = description,
                category = category,
                colorHex = colorHex
            )
            repository.insertTimetableSlot(slot)
            _message.value = "Schedule slot added"
        }
    }

    fun updateSlot(slot: TimetableSlot) {
        viewModelScope.launch {
            repository.updateTimetableSlot(slot)
            _message.value = "Schedule slot updated"
        }
    }

    fun deleteSlot(id: Long) {
        viewModelScope.launch {
            repository.deleteTimetableSlot(id)
            _message.value = "Slot removed"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
