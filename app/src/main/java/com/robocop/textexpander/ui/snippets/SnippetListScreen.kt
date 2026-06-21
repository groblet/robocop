package com.robocop.textexpander.ui.snippets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robocop.textexpander.data.Snippet
import com.robocop.textexpander.data.SnippetType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetListScreen(
    onAddSnippet: () -> Unit,
    onEditSnippet: (Long) -> Unit
) {
    val viewModel: SnippetListViewModel = viewModel()
    val snippets by viewModel.snippets.collectAsState()
    var query by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSnippet) {
                Icon(Icons.Filled.Add, contentDescription = "Add snippet")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.setQuery(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search snippets") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(snippets, key = { it.id }) { snippet ->
                    SnippetRow(
                        snippet = snippet,
                        onClick = { onEditSnippet(snippet.id) },
                        onToggle = { viewModel.toggleEnabled(snippet) },
                        onDelete = { viewModel.delete(snippet) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SnippetRow(
    snippet: Snippet,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(snippet.trigger, fontWeight = FontWeight.Bold)
                Text(snippet.name)
                Text("${snippet.group} · ${typeLabel(snippet.type)}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
            Switch(checked = snippet.enabled, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

private fun typeLabel(type: SnippetType): String = when (type) {
    SnippetType.PLAIN_TEXT -> "Text"
    SnippetType.AI_PROMPT -> "AI prompt"
    SnippetType.IMAGE -> "Image"
    SnippetType.SHELL_SCRIPT -> "Shell script"
    SnippetType.PYTHON_SCRIPT -> "Python script"
    SnippetType.FORM -> "Form"
}
