package com.example.consolicalm

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.UUID

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)

private enum class PriorityType(val label: String, val emoji: String, val rank: Int) {
    URGENT("urgent", "🚨", 3),
    FOCUS("focus", "🧠", 2),
    LOW("nice to get to", "🌿", 1),
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
    return when (priority) {
        PriorityType.URGENT -> BorderStroke(2.dp, Color(0x66D32F2F))
        PriorityType.FOCUS -> BorderStroke(2.dp, Color(0x664169E1))
        PriorityType.LOW -> BorderStroke(2.dp, Color(0x66D6C3B5))
        PriorityType.NORMAL -> null
    }
}

private fun priorityChipColors(priority: PriorityType): Pair<Color, Color> {
    return when (priority) {
        PriorityType.URGENT -> Color(0x1AD32F2F) to Color(0xFFB71C1C)
        PriorityType.FOCUS -> Color(0x1A4169E1) to Color(0xFF2F55C7)
        PriorityType.LOW -> Color(0x332E7D6E) to Color(0xFF2E7D6E)
        PriorityType.NORMAL -> Color(0x12FFFFFF) to Color(0xFF6E6E6E)
    }
}

private fun priorityContainerColor(priority: PriorityType): Color {
    return when (priority) {
        PriorityType.URGENT -> Color(0xFFFFF1EE)
        PriorityType.FOCUS -> Color(0xFFF1F5FF)
        PriorityType.LOW -> Color(0xFFF2F6F2)
        PriorityType.NORMAL -> Color(0xFFF7F4EF)
    }
}

private fun taskSupportText(priority: PriorityType, isDone: Boolean): String {
    if (isDone) return "Completed ✓"

    return when (priority) {
        PriorityType.URGENT -> "Time-sensitive — good to do first"
        PriorityType.FOCUS -> "Focused work — best with calm attention"
        PriorityType.LOW -> "Low-pressure task — nice for lower energy"
        PriorityType.NORMAL -> "A small steady step still counts"
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
    val total = items.size
    val progress = if (total == 0) 0f else completed.toFloat() / total.toFloat()

    val sortedItems by remember(items) {
        derivedStateOf {
            items.sortedWith(
                compareBy<TodoItem> { it.isDone }
                    .thenByDescending { detectPriority(it.text).rank }
                    .thenBy { it.text.lowercase(Locale.getDefault()) }
            )
        }
    }

    val activeItems = sortedItems.filter { !it.isDone }
    val completedItems = sortedItems.filter { it.isDone }

    val topFocusItem = activeItems.firstOrNull()
    val otherActiveItems = activeItems.drop(1)

    val focusFirstItems = otherActiveItems.filter {
        val p = detectPriority(it.text)
        p == PriorityType.URGENT || p == PriorityType.FOCUS
    }

    val gentleItems = otherActiveItems.filter {
        val p = detectPriority(it.text)
        p == PriorityType.LOW || p == PriorityType.NORMAL
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
                        Icon(Icons.Default.Add, contentDescription = "Add task")
                    }
                }
            )
        }
    ) { padding ->

        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProgressHeaderCard(
                    remaining = remaining,
                    completed = completed,
                    progress = progress
                )

                QuickAddCard(
                    onAddClick = { showAddDialog = true }
                )

                EmptyTodoState(
                    onAddClick = { showAddDialog = true }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
            ) {
                item {
                    ProgressHeaderCard(
                        remaining = remaining,
                        completed = completed,
                        progress = progress
                    )
                }

                item {
                    QuickAddCard(
                        onAddClick = { showAddDialog = true }
                    )
                }

                topFocusItem?.let { todo ->
                    item(key = "top-focus-header") {
                        SectionHeader(
                            title = "Top focus ✨",
                            subtitle = "Your main thing to tackle first"
                        )
                    }

                    item(key = "top-focus-card-${todo.id}") {
                        TodoCard(
                            item = todo,
                            onToggle = { checked -> onToggle(todo.id, checked) },
                            onEdit = { editing = todo },
                            onDelete = { onDelete(todo.id) },
                            isTopFocus = true
                        )
                    }
                }

                if (focusFirstItems.isNotEmpty()) {
                    item(key = "focus-first-header") {
                        SectionHeader(
                            title = "Focus first",
                            subtitle = "More important or higher-effort tasks"
                        )
                    }

                    items(focusFirstItems, key = { it.id }) { todo ->
                        TodoCard(
                            item = todo,
                            onToggle = { checked -> onToggle(todo.id, checked) },
                            onEdit = { editing = todo },
                            onDelete = { onDelete(todo.id) }
                        )
                    }
                }

                if (gentleItems.isNotEmpty()) {
                    item(key = "gentle-header") {
                        SectionHeader(
                            title = "Gentle tasks 🌿",
                            subtitle = "Smaller or lower-energy things"
                        )
                    }

                    items(gentleItems, key = { it.id }) { todo ->
                        TodoCard(
                            item = todo,
                            onToggle = { checked -> onToggle(todo.id, checked) },
                            onEdit = { editing = todo },
                            onDelete = { onDelete(todo.id) }
                        )
                    }
                }

                if (completedItems.isNotEmpty()) {
                    item(key = "completed-header") {
                        SectionHeader(
                            title = "Completed ✓",
                            subtitle = "Little wins still count"
                        )
                    }

                    items(completedItems, key = { it.id }) { todo ->
                        TodoCard(
                            item = todo,
                            onToggle = { checked -> onToggle(todo.id, checked) },
                            onEdit = { editing = todo },
                            onDelete = { onDelete(todo.id) }
                        )
                    }
                }
            }
        }

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

        editing?.let { todo ->
            TaskDialog(
                title = "Edit task",
                confirmText = "Save",
                initial = todo.text,
                onDismiss = { editing = null },
                onConfirm = { newText ->
                    onEdit(todo.id, newText)
                    editing = null
                }
            )
        }
    }
}

@Composable
private fun ProgressHeaderCard(
    remaining: Int,
    completed: Int,
    progress: Float
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Soft plan for today 📅",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = when {
                    remaining == 0 && completed == 0 -> "Add one small thing to get started"
                    remaining == 0 -> "Everything is done for now 🌷"
                    else -> "$remaining left • $completed done"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                trackColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityPill(priority = PriorityType.URGENT)
                PriorityPill(priority = PriorityType.FOCUS)
                PriorityPill(priority = PriorityType.LOW)
            }
        }
    }
}

@Composable
private fun QuickAddCard(
    onAddClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier.size(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add a task",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "What’s one small thing you can do today?",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun EmptyTodoState(
    onAddClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "No tasks yet 🌸",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Add one small task to get started. Tiny progress still counts.",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onAddClick) {
                Text("Add your first task")
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
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
            Text(
                text = priority.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TodoCard(
    item: TodoItem,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isTopFocus: Boolean = false
) {
    val priority = detectPriority(item.text)
    val border = if (isTopFocus) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
    } else {
        priorityBorder(priority)
    }

    var popped by remember { mutableStateOf(false) }

    LaunchedEffect(item.isDone) {
        if (item.isDone) {
            popped = true
            delay(120)
            popped = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = when {
            popped -> 1.08f
            item.isDone -> 0.95f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (item.isDone) 0.6f else 1f,
        animationSpec = tween(220),
        label = "alpha"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDone) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                priorityContainerColor(priority)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0x14FFFFFF),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Checkbox(
                        checked = item.isDone,
                        onCheckedChange = { checked -> onToggle(checked) }
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() }
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isTopFocus && !item.isDone) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Main task",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "${priority.emoji}  ${item.text}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.SemiBold
                )

                Text(
                    text = taskSupportText(priority, item.isDone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    fontStyle = if (item.isDone) FontStyle.Italic else FontStyle.Normal
                )

                val (bg, txt) = priorityChipColors(priority)
                Surface(
                    color = bg,
                    contentColor = txt,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = when {
                            item.isDone -> "completed"
                            isTopFocus -> "top focus"
                            else -> priority.label
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

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
                        text = "Detected: ${priority.emoji} ${priority.label}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = when (priority) {
                        PriorityType.URGENT -> "Looks time-sensitive."
                        PriorityType.FOCUS -> "Looks like focused work."
                        PriorityType.LOW -> "Looks like a lighter task."
                        PriorityType.NORMAL -> "A simple everyday task."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim().isNotEmpty()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}