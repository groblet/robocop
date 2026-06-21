package com.robocop.textexpander.ui.snippets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.robocop.textexpander.data.Snippet
import com.robocop.textexpander.data.SnippetRepository
import com.robocop.textexpander.data.SnippetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SnippetEditViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SnippetRepository.get(application)

    private val _snippet = MutableStateFlow<Snippet?>(null)
    val snippet: StateFlow<Snippet?> = _snippet

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun load(id: Long?) {
        if (id == null) {
            _snippet.value = Snippet(trigger = "", name = "", type = SnippetType.PLAIN_TEXT, content = "")
            return
        }
        viewModelScope.launch {
            _snippet.value = repository.getSnippet(id)
        }
    }

    fun save(updated: Snippet, onSaved: () -> Unit) {
        viewModelScope.launch {
            if (updated.trigger.isBlank()) {
                _error.value = "Trigger can't be empty"
                return@launch
            }
            if (repository.isTriggerTaken(updated.trigger, updated.id)) {
                _error.value = "Another snippet already uses this trigger"
                return@launch
            }
            repository.saveSnippet(updated.copy(updatedAt = System.currentTimeMillis()))
            onSaved()
        }
    }

    fun delete(snippet: Snippet, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteSnippet(snippet)
            onDeleted()
        }
    }

    fun clearError() {
        _error.value = null
    }
}
