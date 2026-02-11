package com.example.consolicalm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.UUID
import androidx.compose.material3.ExperimentalMaterial3Api


data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    items: List<TodoItem>,
    onAdd: (String) -> Unit,
    onToggle: (id: String, checked: Boolean) -> Unit,
    onEdit: (id: String, newText: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TodoItem?>(null) }

    val remaining = items.count { !it.isDone }
    val completed = items.count { it.isDone }

    val sortedItems = remember(items) {
        items.sortedWith(compareBy<TodoItem> { it.isDone }.thenBy { it.text.lowercase() })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("To-Do List", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Soft plan for today ✿", fontWeight = FontWeight.Bold)
                    Text(
                        if (items.isEmpty()) "Add something tiny to start (even 2 minutes counts)."
                        else "$remaining left • $completed done"
                    )
                }
            }

            if (items.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("No tasks yet.", fontWeight = FontWeight.SemiBold)
                        Text("Tap the + to add one. Keep it small and gentle.")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedItems, key = { it.id }) { item ->
                        TodoCard(
                            item = item,
                            onToggle = { checked -> onToggle(item.id, checked) },
                            onEdit = { editing = item },
                            onDelete = { onDelete(item.id) }
                        )
                    }
                }
            }
        }

        // Add dialog
        if (showAddDialog) {
            TaskDialog(
                title = "Add a task",
                confirmText = "Add",
                initial = "",
                onDismiss = { showAddDialog = false },
                onConfirm = { text ->
                    onAdd(text)
                    showAddDialog = false
                }
            )
        }

        // Edit dialog
        editing?.let { item ->
            TaskDialog(
                title = "Edit task",
                confirmText = "Save",
                initial = item.text,
                onDismiss = { editing = null },
                onConfirm = { newText ->
                    onEdit(item.id, newText)
                    editing = null
                }
            )
        }
    }
}

@Composable
private fun TodoCard(
    item: TodoItem,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isDone,
                onCheckedChange = { onToggle(it) }
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() }
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.SemiBold
                )
                Text(
                    text = if (item.isDone) "Done ✿ (tap to edit)" else "Tap to edit",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun TaskDialog(
    title: String,
    confirmText: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: open Canvas, write 1 sentence…") },
                    singleLine = true
                )
                Text("Tiny tasks feel safer. You can always add more later.")
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim().isNotEmpty()
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
