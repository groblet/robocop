package com.robocop.textexpander.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.robocop.textexpander.data.Snippet
import com.robocop.textexpander.data.SnippetRepository
import com.robocop.textexpander.expansion.TemplateRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * Floating snippet search shown over other apps by [com.robocop.textexpander.service.QuickSearchBubbleService].
 * Tapping a result renders its template (no clipboard/field substitution, since there's no
 * specific focused field context here yet) and hands the text back to [onInsert].
 */
@Composable
fun QuickSearchOverlay(onDismiss: () -> Unit, onInsert: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { SnippetRepository.get(context) }
    var query by remember { mutableStateOf("") }

    val queryFlow = remember { MutableStateFlow("") }
    LaunchedEffect(query) { queryFlow.value = query }

    val filtered by remember {
        combine(repository.observeEnabledSnippets(), queryFlow) { snippets, q ->
            if (q.isBlank()) snippets
            else snippets.filter { it.trigger.contains(q, true) || it.name.contains(q, true) }
        }
    }.collectAsState(initial = emptyList())

    Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search snippets…") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(filtered.take(20), key = { it.id }) { snippet ->
                        SearchResultRow(snippet) {
                            val rendered = TemplateRenderer.render(snippet.content, "")
                            onInsert(rendered.text)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(snippet: Snippet, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        Text(snippet.trigger, style = MaterialTheme.typography.bodyMedium)
        Text(snippet.name, style = MaterialTheme.typography.bodySmall)
    }
}
