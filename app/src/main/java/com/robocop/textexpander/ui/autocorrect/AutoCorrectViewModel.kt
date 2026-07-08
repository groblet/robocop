package com.robocop.textexpander.ui.autocorrect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.robocop.textexpander.data.AutoCorrectEntry
import com.robocop.textexpander.data.SnippetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AutoCorrectViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SnippetRepository.get(application)

    val entries: StateFlow<List<AutoCorrectEntry>> = repository.observeAutoCorrectEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCustomEntry(typo: String, correction: String) {
        if (typo.isBlank() || correction.isBlank()) return
        viewModelScope.launch {
            repository.saveAutoCorrectEntry(AutoCorrectEntry(typo = typo.trim(), correction = correction.trim()))
        }
    }

    fun toggle(entry: AutoCorrectEntry) {
        viewModelScope.launch { repository.saveAutoCorrectEntry(entry.copy(enabled = !entry.enabled)) }
    }

    fun delete(entry: AutoCorrectEntry) {
        viewModelScope.launch { repository.deleteAutoCorrectEntry(entry) }
    }
}
