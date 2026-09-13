package com.example.util

import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val dayNameFormatter = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    private val fullDateTimeFormatter = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

    fun formatTime(millis: Long): String {
        return timeFormatter.format(Date(millis))
    }

    fun formatDate(millis: Long): String {
        return dateFormatter.format(Date(millis))
    }

    fun formatDayAndDate(millis: Long): String {
        return dayNameFormatter.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        return fullDateTimeFormatter.format(Date(millis))
    }

    fun formatDurationHoursMinutes(startMillis: Long, endMillis: Long?): String {
        val end = endMillis ?: System.currentTimeMillis()
        val diff = (end - startMillis).coerceAtLeast(0)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    fun formatTimeFromHourMinute(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val displayMinute = String.format(Locale.US, "%02d", minute)
        return "$displayHour:$displayMinute $amPm"
    }

    /**
     * Checks if given slot is currently ongoing on the specified day of week
     */
    fun isSlotCurrent(dayOfWeek: Int, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): Boolean {
        val now = LocalDateTime.now()
        val currentDayValue = now.dayOfWeek.value // 1 = Monday, 7 = Sunday
        if (currentDayValue != dayOfWeek) return false

        val currentTotalMinutes = now.hour * 60 + now.minute
        val startTotalMinutes = startHour * 60 + startMinute
        val endTotalMinutes = endHour * 60 + endMinute

        return currentTotalMinutes in startTotalMinutes..endTotalMinutes
    }

    /**
     * Calculate monthly attendance percentage based on days attended vs total days elapsed in current month
     */
    fun calculateMonthlyAttendance(datesAttended: Set<String>): AttendanceStats {
        val today = LocalDate.now()
        val currentYearMonth = YearMonth.now()
        val currentMonthPrefix = String.format(Locale.US, "%04d-%02d", today.year, today.monthValue)
        val daysInMonth = currentYearMonth.lengthOfMonth()
        val dayOfMonth = today.dayOfMonth

        // Count attended days in current month
        val attendedInMonth = datesAttended.count { it.startsWith(currentMonthPrefix) }

        // Working weekdays or days elapsed so far:
        var totalWorkingDaysSoFar = 0
        for (day in 1..dayOfMonth) {
            val date = today.withDayOfMonth(day)
            if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
                totalWorkingDaysSoFar++
            }
        }
        if (totalWorkingDaysSoFar == 0) totalWorkingDaysSoFar = 1

        val percentage = ((attendedInMonth.toDouble() / totalWorkingDaysSoFar) * 100.0)
            .coerceIn(0.0, 100.0)

        return AttendanceStats(
            percentage = percentage.toInt(),
            attendedDays = attendedInMonth,
            totalWorkDays = totalWorkingDaysSoFar,
            monthName = today.month.name.lowercase().replaceFirstChar { it.uppercase() },
            daysInMonth = daysInMonth
        )
    }
}

data class AttendanceStats(
    val percentage: Int,
    val attendedDays: Int,
    val totalWorkDays: Int,
    val monthName: String,
    val daysInMonth: Int
)
