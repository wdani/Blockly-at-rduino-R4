package ch.elekto.blocklyrduino.r4.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.elekto.blocklyrduino.r4.model.BlueprintRecord

@Composable
fun BlueprintSelectionBar(
    groupCount: Int,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Blueprint auswählen", fontWeight = FontWeight.Bold)
            Text(
                if (groupCount == 1) "1 Gruppe ausgewählt" else "$groupCount Gruppen ausgewählt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Abbrechen")
                }
                Button(
                    onClick = onCreate,
                    enabled = groupCount > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Blueprint erstellen")
                }
            }
        }
    }
}

@Composable
fun BlueprintNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val cleaned = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gib dem Blueprint einen kurzen Namen, damit du ihn später wiederfindest.")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Name") }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(cleaned) },
                enabled = cleaned.isNotBlank()
            ) {
                Text(confirmLabel)
            }
        }
    )
}

@Composable
fun BlueprintDeleteDialog(
    blueprintName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Blueprint löschen?") },
        text = { Text("„$blueprintName“ wird dauerhaft aus dieser App gelöscht.") },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Löschen") }
        }
    )
}

@Composable
fun BlueprintBrowserList(
    blueprints: List<BlueprintRecord>,
    onInsert: (BlueprintRecord) -> Unit,
    onRename: (BlueprintRecord) -> Unit,
    onDelete: (BlueprintRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    if (blueprints.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Noch keine Blueprints", fontWeight = FontWeight.Bold)
                Text(
                    "Halte einen Block länger gedrückt und wähle „Als Blueprint speichern“. Danach kannst du weitere Stapel hinzufügen.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(blueprints, key = { it.id }) { blueprint ->
            Card(
                onClick = { onInsert(blueprint) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        blueprint.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${blueprint.groupCount} ${if (blueprint.groupCount == 1) "Gruppe" else "Gruppen"} · ${blueprint.blockCount} ${if (blueprint.blockCount == 1) "Block" else "Blöcke"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Antippen zum Einfügen",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRename(blueprint) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Umbenennen")
                        }
                        OutlinedButton(
                            onClick = { onDelete(blueprint) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Löschen")
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
