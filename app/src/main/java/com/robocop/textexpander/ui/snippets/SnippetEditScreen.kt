package com.robocop.textexpander.ui.snippets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.menuAnchor
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robocop.textexpander.data.Snippet
import com.robocop.textexpander.data.SnippetType
import com.robocop.textexpander.expansion.FormFieldDef
import com.robocop.textexpander.expansion.FormFieldType
import com.robocop.textexpander.expansion.FormSchema

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetEditScreen(
    snippetId: Long?,
    onBack: () -> Unit
) {
    val viewModel: SnippetEditViewModel = viewModel()
    LaunchedEffect(snippetId) { viewModel.load(snippetId) }

    val loaded by viewModel.snippet.collectAsState()
    val error by viewModel.error.collectAsState()
    val current = loaded ?: return

    var trigger by remember(current.id) { mutableStateOf(current.trigger) }
    var name by remember(current.id) { mutableStateOf(current.name) }
    var type by remember(current.id) { mutableStateOf(current.type) }
    var content by remember(current.id) { mutableStateOf(current.content) }
    var group by remember(current.id) { mutableStateOf(current.group) }
    var caseSensitive by remember(current.id) { mutableStateOf(current.caseSensitive) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (snippetId == null) "Add snippet" else "Edit snippet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (snippetId != null) {
                        IconButton(onClick = { viewModel.delete(current, onBack) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = trigger,
                onValueChange = { trigger = it },
                label = { Text("Trigger, e.g. :sig") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = group,
                onValueChange = { group = it },
                label = { Text("Group") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Case sensitive")
                Checkbox(checked = caseSensitive, onCheckedChange = { caseSensitive = it })
            }

            TypeSelector(selected = type, onSelected = { type = it })

            when (type) {
                SnippetType.PLAIN_TEXT, SnippetType.AI_PROMPT, SnippetType.SHELL_SCRIPT, SnippetType.PYTHON_SCRIPT -> {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text(contentLabelFor(type)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        minLines = 4
                    )
                }
                SnippetType.IMAGE -> {
                    ImagePickerField(currentUri = content, onPicked = { content = it })
                }
                SnippetType.FORM -> {
                    FormBuilderField(content = content, onContentChange = { content = it })
                }
            }

            if (error != null) {
                Text(error.orEmpty(), color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            Button(
                onClick = {
                    viewModel.save(
                        current.copy(
                            trigger = trigger.trim(),
                            name = name.trim().ifBlank { trigger.trim() },
                            type = type,
                            content = content,
                            group = group.trim().ifBlank { "General" },
                            caseSensitive = caseSensitive
                        ),
                        onSaved = onBack
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Save")
            }
        }
    }
}

private fun contentLabelFor(type: SnippetType): String = when (type) {
    SnippetType.SHELL_SCRIPT -> "Shell script (output replaces the trigger)"
    SnippetType.PYTHON_SCRIPT -> "Python script (requires Termux + Termux:API)"
    SnippetType.AI_PROMPT -> "Prompt text"
    else -> "Content (supports \${cursor}, \${date}, \${time}, \${clipboard})"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSelector(selected: SnippetType, onSelected: (SnippetType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SnippetType.entries.forEach { option ->
                DropdownMenuItem(text = { Text(option.displayName()) }, onClick = {
                    onSelected(option)
                    expanded = false
                })
            }
        }
    }
}

private fun SnippetType.displayName(): String = when (this) {
    SnippetType.PLAIN_TEXT -> "Plain text"
    SnippetType.AI_PROMPT -> "AI prompt"
    SnippetType.IMAGE -> "Image"
    SnippetType.SHELL_SCRIPT -> "Shell script"
    SnippetType.PYTHON_SCRIPT -> "Python script (Termux)"
    SnippetType.FORM -> "Interactive form"
}

@Composable
private fun ImagePickerField(currentUri: String, onPicked: (String) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri.toString())
    }
    Column(modifier = Modifier.padding(top = 8.dp)) {
        if (currentUri.isNotBlank()) {
            Text("Selected: $currentUri", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
        Button(onClick = {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) {
            Text("Choose image")
        }
    }
}

@Composable
private fun FormBuilderField(content: String, onContentChange: (String) -> Unit) {
    var schema by remember(content) { mutableStateOf(FormSchema.fromJson(content.ifBlank { FormSchema().toJson() })) }

    fun push(newSchema: FormSchema) {
        schema = newSchema
        onContentChange(newSchema.toJson())
    }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text("Fields")
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(schema.fields, key = { it.key }) { field ->
                FormFieldRow(
                    field = field,
                    onChange = { updated -> push(schema.copy(fields = schema.fields.map { if (it.key == field.key) updated else it })) },
                    onRemove = { push(schema.copy(fields = schema.fields.filterNot { it.key == field.key })) }
                )
            }
        }
        TextButton(onClick = {
            val newKey = "field${schema.fields.size + 1}"
            push(schema.copy(fields = schema.fields + FormFieldDef(key = newKey, label = "Label")))
        }) {
            Text("+ Add field")
        }
        OutlinedTextField(
            value = schema.template,
            onValueChange = { push(schema.copy(template = it)) },
            label = { Text("Template, e.g. Hi \${field:newKey}, ...") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            minLines = 3
        )
    }
}

@Composable
private fun FormFieldRow(field: FormFieldDef, onChange: (FormFieldDef) -> Unit, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("\${field:${field.key}}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove field")
                }
            }
            OutlinedTextField(
                value = field.label,
                onValueChange = { onChange(field.copy(label = it)) },
                label = { Text("Label") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Dropdown")
                Checkbox(
                    checked = field.type == FormFieldType.DROPDOWN,
                    onCheckedChange = { isDropdown ->
                        onChange(field.copy(type = if (isDropdown) FormFieldType.DROPDOWN else FormFieldType.TEXT))
                    }
                )
            }
            if (field.type == FormFieldType.DROPDOWN) {
                OutlinedTextField(
                    value = field.options.joinToString(", "),
                    onValueChange = { onChange(field.copy(options = it.split(",").map { o -> o.trim() }.filter { o -> o.isNotEmpty() })) },
                    label = { Text("Options (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}
