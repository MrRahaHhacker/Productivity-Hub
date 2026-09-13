package com.example.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TaskItem
import com.example.data.local.TaskPriority
import com.example.data.repository.ProductivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TasksUiState(
    val pendingTasks: List<TaskItem> = emptyList(),
    val completedTasks: List<TaskItem> = emptyList(),
    val selectedFilterPriority: String? = null, // null = all
    val message: String? = null
)

class TasksViewModel(
    private val repository: ProductivityRepository
) : ViewModel() {

    private val _selectedFilterPriority = MutableStateFlow<String?>(null)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<TasksUiState> = combine(
        repository.allTasks,
        _selectedFilterPriority,
        _message
    ) { allTasks, filterPriority, msg ->
        val filtered = if (filterPriority == null) {
            allTasks
        } else {
            allTasks.filter { it.priority == filterPriority }
        }

        val pending = filtered.filter { !it.isCompleted }
            .sortedWith(
                compareBy(
                    { getPriorityWeight(it.priority) },
                    { it.dueDateMillis }
                )
            )

        val completed = filtered.filter { it.isCompleted }
            .sortedByDescending { it.completedAtMillis ?: it.dueDateMillis }

        TasksUiState(
            pendingTasks = pending,
            completedTasks = completed,
            selectedFilterPriority = filterPriority,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState()
    )

    private fun getPriorityWeight(priority: String): Int {
        return when (priority) {
            TaskPriority.HIGH.name -> 0
            TaskPriority.MEDIUM.name -> 1
            TaskPriority.LOW.name -> 2
            else -> 3
        }
    }

    fun setPriorityFilter(priority: String?) {
        _selectedFilterPriority.value = priority
    }

    fun toggleTaskCompletion(task: TaskItem) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            repository.setTaskCompletion(task.id, newCompleted)
            _message.value = if (newCompleted) "Task marked completed!" else "Task moved to pending"
        }
    }

    fun addTask(
        title: String,
        description: String,
        priority: TaskPriority,
        dueDateMillis: Long
    ) {
        viewModelScope.launch {
            val newTask = TaskItem(
                title = title,
                description = description,
                priority = priority.name,
                dueDateMillis = dueDateMillis,
                isCompleted = false
            )
            repository.insertTask(newTask)
            _message.value = "Task created"
        }
    }

    fun updateTask(task: TaskItem) {
        viewModelScope.launch {
            repository.updateTask(task)
            _message.value = "Task updated"
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            repository.deleteTask(id)
            _message.value = "Task deleted"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
