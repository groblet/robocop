package com.robocop.textexpander.ui.snippets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.robocop.textexpander.data.Snippet
import com.robocop.textexpander.data.SnippetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SnippetListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SnippetRepository.get(application)
    private val query = MutableStateFlow("")

    val snippets: StateFlow<List<Snippet>> = combine(repository.observeSnippets(), query) { all, q ->
        if (q.isBlank()) all
        else all.filter {
            it.trigger.contains(q, ignoreCase = true) ||
                it.name.contains(q, ignoreCase = true) ||
                it.group.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleEnabled(snippet: Snippet) {
        viewModelScope.launch { repository.saveSnippet(snippet.copy(enabled = !snippet.enabled)) }
    }

    fun delete(snippet: Snippet) {
        viewModelScope.launch { repository.deleteSnippet(snippet) }
    }
}
