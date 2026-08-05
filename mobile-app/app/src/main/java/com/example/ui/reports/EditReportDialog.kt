package com.example.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.models.PotholeReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReportDialog(
    report: PotholeReport,
    onDismiss: () -> Unit,
    onSave: (description: String, severity: String) -> Unit
) {
    var description by remember { mutableStateOf(report.description ?: "") }
    var severity by remember { mutableStateOf(report.severity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Report") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Text("Severity")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LOW", "MEDIUM", "HIGH").forEach { level ->
                        FilterChip(
                            selected = severity.equals(level, true),
                            onClick = { severity = level },
                            label = { Text(level) }
                        )
                    }
                }
                Text("Photo editing requires the manual report screen (not fully implemented in this dialog yet).", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(description, severity) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
