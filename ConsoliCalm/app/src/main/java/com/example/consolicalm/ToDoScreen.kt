package com.example.consolicalm

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import java.util.UUID

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)

private enum class PriorityType(val label: String, val emoji: String, val rank: Int) {
    URGENT("urgent", "\uD83D\uDEA8", 3),
    FOCUS("focus", "🧠", 2),
    LOW("nice to get to", "\uD83E\uDEB4", 1),
    NORMAL("normal", "✨", 0)
}

private fun detectPriority(text: String): PriorityType {
    val t = text.lowercase(Locale.getDefault())
    return when {
        listOf("exam", "test", "quiz", "due", "deadline", "asap", "urgent", "submit").any { it in t } ->
            PriorityType.URGENT

        listOf("study", "write", "project", "essay", "assignment", "code", "homework", "lab").any { it in t } ->
            PriorityType.FOCUS

        listOf("clean", "organize", "laundry", "dishes", "call", "email", "errand", "grocery").any { it in t } ->
            PriorityType.LOW

        else -> PriorityType.NORMAL
    }
}

private fun priorityBorder(priority: PriorityType): BorderStroke? {
    // soft “glow” look using semi-transparent borders
    return when (priority) {
        PriorityType.URGENT -> BorderStroke(2.dp, Color(0x66D32F2F))   // red glow
        PriorityType.FOCUS -> BorderStroke(2.dp, Color(0x664169E1))    // blue glow
        PriorityType.LOW -> BorderStroke(2.dp, Color(0x66D6C3B5))      // beige glow
        PriorityType.NORMAL -> null
    }
}

private fun priorityChipColors(priority: PriorityType): Pair<Color, Color> {
    // background, text (no MaterialTheme usage here, so no @Composable needed)
    return when (priority) {
        PriorityType.URGENT -> Color(0x1AD32F2F) to Color(0xFFB71C1C)
        PriorityType.FOCUS -> Color(0x1A4169E1) to Color(0xFF2F55C7)
        PriorityType.LOW -> Color(0x332E7D6E) to Color(0xFF2E7D6E)
        PriorityType.NORMAL -> Color(0x12FFFFFF) to Color(0xFF6E6E6E)
    }
}


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

    // ✅ IMPORTANT: this fixes “I have to leave and come back”
    // derivedStateOf recalculates whenever items changes.
    val sortedItems by remember(items) {
        derivedStateOf {
            items.sortedWith(
                compareBy<TodoItem> { it.isDone }
                    .thenByDescending { detectPriority(it.text).rank }
                    .thenBy { it.text.lowercase(Locale.getDefault()) }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("To-Do List ", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add task")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ⭐ Header card: more like inspo
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Soft plan for today \uD83D\uDCC5 ", fontWeight = FontWeight.Bold)
                    Text(
                        if (items.isEmpty()) "Add a small task you would like to get done"
                        else "$remaining left • $completed done",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Priority chips row (cute, neutral)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PriorityPill(priority = PriorityType.URGENT)
                        PriorityPill(priority = PriorityType.FOCUS)
                        PriorityPill(priority = PriorityType.LOW)
                    }
                }
            }

            if (items.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("No tasks yet.", fontWeight = FontWeight.SemiBold)
                        Text("Tap + to add one. Lets do this! ☺\uFE0F ")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 14.dp)
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

        // ADD dialog
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

        // EDIT dialog
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
private fun PriorityPill(priority: PriorityType) {
    val (bg, txt) = priorityChipColors(priority)
    Surface(
        color = bg,
        contentColor = txt,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(priority.emoji)
            Spacer(Modifier.width(6.dp))
            Text(priority.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
    val priority = detectPriority(item.text)
    val border = priorityBorder(priority)

    // ✅ Completion animation: when you check it, it pops and fades a bit
    val targetScale = if (item.isDone) 0.98f else 1f
    val targetAlpha = if (item.isDone) 0.70f else 1f

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(220),
        label = "alpha"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        border = border,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp), clip = false)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // checkbox in a soft circle background (more “designed”)
            Surface(
                color = Color(0x14FFFFFF),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Checkbox(
                        checked = item.isDone,
                        onCheckedChange = { onToggle(it) }
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() }
                    .padding(vertical = 4.dp)
            ) {

                // title row: emoji + text
                Text(
                    text = "${priority.emoji}  ${item.text}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.SemiBold
                )

                Spacer(Modifier.height(6.dp))

                // tiny pill tag (like inspo “in progress” vibe)
                val (bg, txt) = priorityChipColors(priority)
                Surface(
                    color = bg,
                    contentColor = txt,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (item.isDone) "completed" else priority.label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // little edit icon for clarity
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
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
    val priority = detectPriority(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: study for exam") },
                    singleLine = true
                )

                val (bg, txt) = priorityChipColors(priority)
                Surface(
                    color = bg,
                    contentColor = txt,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        "Detected: ${priority.emoji} ${priority.label}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
