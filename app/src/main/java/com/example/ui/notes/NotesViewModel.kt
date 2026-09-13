package com.example.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NoteItem
import com.example.data.repository.ProductivityRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<NoteItem> = emptyList(),
    val searchQuery: String = "",
    val isGridView: Boolean = true,
    val selectedCategory: String? = null,
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(
    private val repository: ProductivityRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<NotesUiState> = combine(
        _searchQuery.flatMapLatest { query -> repository.searchNotes(query) },
        _searchQuery,
        _isGridView,
        _selectedCategory,
        _message
    ) { notes, query, isGrid, cat, msg ->
        val filtered = if (cat == null) notes else notes.filter { it.category == cat }
        NotesUiState(
            notes = filtered,
            searchQuery = query,
            isGridView = isGrid,
            selectedCategory = cat,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun saveNote(
        id: Long = 0,
        title: String,
        content: String,
        category: String,
        colorHex: Long
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                val newNote = NoteItem(
                    title = title,
                    content = content,
                    category = category,
                    colorTagHex = colorHex,
                    updatedAtMillis = System.currentTimeMillis()
                )
                repository.insertNote(newNote)
                _message.value = "Note saved!"
            } else {
                val existing = NoteItem(
                    id = id,
                    title = title,
                    content = content,
                    category = category,
                    colorTagHex = colorHex,
                    updatedAtMillis = System.currentTimeMillis()
                )
                repository.updateNote(existing)
                _message.value = "Note updated!"
            }
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
            _message.value = "Note deleted"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
