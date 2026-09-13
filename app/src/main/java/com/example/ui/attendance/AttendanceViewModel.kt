package com.example.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AttendanceRecord
import com.example.data.repository.ProductivityRepository
import com.example.util.AttendanceStats
import com.example.util.DateTimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AttendanceUiState(
    val records: List<AttendanceRecord> = emptyList(),
    val activeRecord: AttendanceRecord? = null,
    val isCheckedIn: Boolean = false,
    val stats: AttendanceStats = AttendanceStats(0, 0, 1, "Month", 30),
    val elapsedSessionText: String = "",
    val message: String? = null
)

class AttendanceViewModel(
    private val repository: ProductivityRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _elapsedTime = MutableStateFlow("")
    val elapsedTime: StateFlow<String> = _elapsedTime.asStateFlow()

    val uiState: StateFlow<AttendanceUiState> = combine(
        repository.allAttendanceRecords,
        repository.activeAttendanceRecord,
        _elapsedTime,
        _message
    ) { records, active, elapsed, msg ->
        val uniqueDates = records.map { it.dateIso }.toSet()
        val stats = DateTimeUtils.calculateMonthlyAttendance(uniqueDates)
        AttendanceUiState(
            records = records,
            activeRecord = active,
            isCheckedIn = active != null,
            stats = stats,
            elapsedSessionText = elapsed,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AttendanceUiState()
    )

    init {
        // Ticking loop for active session elapsed time
        viewModelScope.launch {
            while (isActive) {
                val active = uiState.value.activeRecord
                if (active != null) {
                    _elapsedTime.value = DateTimeUtils.formatDurationHoursMinutes(
                        active.checkInTimeMillis,
                        null
                    )
                } else {
                    _elapsedTime.value = ""
                }
                delay(1000)
            }
        }
    }

    fun checkIn(note: String = "") {
        viewModelScope.launch {
            repository.checkIn(note)
            _message.value = "Checked in successfully!"
        }
    }

    fun checkOut() {
        viewModelScope.launch {
            val active = uiState.value.activeRecord
            if (active != null) {
                repository.checkOut(active.id)
                _message.value = "Checked out successfully!"
            }
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteAttendanceRecord(id)
            _message.value = "Attendance record removed"
        }
    }

    fun addManualRecord(dateIso: String, checkIn: Long, checkOut: Long?, note: String) {
        viewModelScope.launch {
            repository.addManualAttendance(dateIso, checkIn, checkOut, note)
            _message.value = "Manual record added"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
