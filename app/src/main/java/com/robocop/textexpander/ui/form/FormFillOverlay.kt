package com.robocop.textexpander.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.menuAnchor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.robocop.textexpander.expansion.FormFieldDef
import com.robocop.textexpander.expansion.FormFieldType
import com.robocop.textexpander.expansion.FormSchema

@Composable
fun FormFillOverlay(
    snippetName: String,
    schema: FormSchema,
    onSubmit: (Map<String, String>) -> Unit,
    onCancel: () -> Unit
) {
    val values = remember { mutableStateMapOf<String, String>().apply { schema.fields.forEach { put(it.key, it.default) } } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(snippetName, style = MaterialTheme.typography.titleMedium)
                schema.fields.forEach { field ->
                    FormFieldInput(
                        field = field,
                        value = values[field.key].orEmpty(),
                        onValueChange = { values[field.key] = it }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                    Button(onClick = { onSubmit(values.toMap()) }, modifier = Modifier.padding(start = 8.dp)) {
                        Text("Insert")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormFieldInput(field: FormFieldDef, value: String, onValueChange: (String) -> Unit) {
    when (field.type) {
        FormFieldType.TEXT -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(field.label) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true
            )
        }
        FormFieldType.DROPDOWN -> {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(field.label) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    field.options.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            onValueChange(option)
                            expanded = false
                        })
                    }
                }
            }
        }
    }
}
