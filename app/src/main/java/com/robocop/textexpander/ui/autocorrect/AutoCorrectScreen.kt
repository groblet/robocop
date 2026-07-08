package com.robocop.textexpander.ui.autocorrect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robocop.textexpander.data.AutoCorrectEntry

@Composable
fun AutoCorrectScreen() {
    val viewModel: AutoCorrectViewModel = viewModel()
    val entries by viewModel.entries.collectAsState()

    var newTypo by remember { mutableStateOf("") }
    var newCorrection by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Custom correction", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = newTypo, onValueChange = { newTypo = it }, label = { Text("Typo") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = newCorrection, onValueChange = { newCorrection = it }, label = { Text("Correction") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Button(
            onClick = {
                viewModel.addCustomEntry(newTypo, newCorrection)
                newTypo = ""
                newCorrection = ""
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Add correction")
        }

        Text("All corrections", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        LazyColumn {
            items(entries, key = { it.id }) { entry ->
                AutoCorrectRow(entry = entry, onToggle = { viewModel.toggle(entry) }, onDelete = { viewModel.delete(entry) })
            }
        }
    }
}

@Composable
private fun AutoCorrectRow(entry: AutoCorrectEntry, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${entry.typo} → ${entry.correction}")
                if (entry.isBuiltIn) {
                    Text("Built-in", style = MaterialTheme.typography.bodySmall)
                }
            }
            Switch(checked = entry.enabled, onCheckedChange = { onToggle() })
            if (!entry.isBuiltIn) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
