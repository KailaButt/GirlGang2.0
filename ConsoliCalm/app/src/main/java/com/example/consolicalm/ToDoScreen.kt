package com.example.consolicalm

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale as drawScale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.consolicalm.ui.theme.PacificoFont
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin



data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)


private enum class PriorityType(
    val label: String,
    val emoji: String,
    val rank: Int,
    val tag: String,
    val flower: FlowerSpecies
) {
    URGENT("urgent", "🚨", 3, "[urgent]", FlowerSpecies.ROSE),
    FOCUS("focus", "🧠", 2, "[focus]", FlowerSpecies.LILY),
    LOW("nice to get to", "🌿", 1, "[gentle]", FlowerSpecies.DAFFODIL),
    NORMAL("normal", "✨", 0, "", FlowerSpecies.TULIP)
}

private fun detectPriority(rawText: String): PriorityType {
    val t = rawText.lowercase(Locale.getDefault()).trim()
    return when {
        t.startsWith("[urgent]") -> PriorityType.URGENT
        t.startsWith("[focus]") -> PriorityType.FOCUS
        t.startsWith("[gentle]") -> PriorityType.LOW
        listOf("exam", "test", "quiz", "due", "deadline", "asap", "urgent", "submit").any { it in t } ->
            PriorityType.URGENT
        listOf("study", "write", "project", "essay", "assignment", "code", "homework", "lab").any { it in t } ->
            PriorityType.FOCUS
        listOf("clean", "organize", "laundry", "dishes", "call", "email", "errand", "grocery").any { it in t } ->
            PriorityType.LOW
        else -> PriorityType.NORMAL
    }
}

private fun removePriorityTag(rawText: String): String {
    return rawText
        .replace("[urgent]", "", ignoreCase = true)
        .replace("[focus]", "", ignoreCase = true)
        .replace("[gentle]", "", ignoreCase = true)
        .trim()
}

private fun taskParts(rawText: String): List<String> =
    removePriorityTag(rawText).split("|").map { it.trim() }

private fun displayTaskText(rawText: String): String = taskParts(rawText).getOrNull(0).orEmpty()
private fun displayDueDate(rawText: String): String = taskParts(rawText).getOrNull(1).orEmpty()
private fun displayDueTime(rawText: String): String = taskParts(rawText).getOrNull(2).orEmpty()

private fun buildTaskText(
    task: String,
    dueDate: String,
    dueTime: String,
    priority: PriorityType
): String {
    val cleanTask = task.trim()
    val cleanDate = dueDate.trim()
    val cleanTime = dueTime.trim()
    val prefix = if (priority == PriorityType.NORMAL) "" else "${priority.tag} "
    val suffix = listOf(cleanDate, cleanTime).filter { it.isNotBlank() }.joinToString(" | ")
    return if (suffix.isBlank()) prefix + cleanTask else "$prefix$cleanTask | $suffix"
}



private enum class FlowerSpecies { ROSE, LILY, DAFFODIL, TULIP, HIBISCUS }


private fun bloomScale(growth: Float): Float {
    val t = (growth - 0.6f) / 0.4f
    if (t <= 0f) return 0f
    if (t >= 1f) return 1f
    return when {
        t < 0.7f -> (t / 0.7f) * 1.15f
        else -> 1.15f - ((t - 0.7f) / 0.3f) * 0.15f
    }
}


private fun DrawScope.drawFlower(
    species: FlowerSpecies,
    growth: Float,
    unit: Float,
    baseColor: Color,
    accentColor: Color,
    centerColor: Color = Color(0xFFF4C96B)
) {
    val stemColor = Color(0xFF4A7A3A)
    val leafColor = Color(0xFF6B9A4A)
    val seedColor = Color(0xFF8B6F47)

    val maxStemHeight = unit * 4.5f
    val stemWidth = unit * 0.35f
    val seedW = unit * 1.1f
    val seedH = unit * 0.7f


    val seedAlpha = (1f - (growth / 0.15f)).coerceIn(0f, 1f)
    if (seedAlpha > 0.01f) {
        drawOval(
            color = seedColor.copy(alpha = seedAlpha * 0.9f),
            topLeft = Offset(-seedW / 2f, -seedH * 1.2f),
            size = Size(seedW, seedH)
        )
    }


    val stemProgress = ((growth - 0.10f) / 0.40f).coerceIn(0f, 1f)
    val stemHeight = stemProgress * maxStemHeight
    if (stemHeight > 1f) {
        drawLine(
            color = stemColor,
            start = Offset(0f, 0f),
            end = Offset(0f, -stemHeight),
            strokeWidth = stemWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }


    val leafProgress = ((growth - 0.30f) / 0.35f).coerceIn(0f, 1f)
    if (leafProgress > 0.05f) {
        val leafSize = leafProgress * unit * 0.7f
        val leafY = -stemHeight * 0.55f
        val leafSide = when (species) {
            FlowerSpecies.ROSE, FlowerSpecies.HIBISCUS, FlowerSpecies.LILY -> -1f
            else -> 1f
        }
        rotate(degrees = leafSide * 35f, pivot = Offset(0f, leafY)) {
            drawOval(
                color = leafColor,
                topLeft = Offset(0f, leafY - leafSize / 2f),
                size = Size(leafSize * 2f, leafSize)
            )
        }
    }


    val bloomS = bloomScale(growth)
    if (bloomS > 0.01f) {
        val bloomY = -stemHeight - unit * 0.25f
        translate(left = 0f, top = bloomY) {


            val r = unit * bloomS
            when (species) {
                FlowerSpecies.ROSE -> drawRose(baseColor, accentColor, centerColor, r)
                FlowerSpecies.LILY -> drawLily(baseColor, accentColor, centerColor, r)
                FlowerSpecies.DAFFODIL -> drawDaffodil(baseColor, accentColor, centerColor, r)
                FlowerSpecies.TULIP -> drawTulip(baseColor, accentColor, centerColor, r)
                FlowerSpecies.HIBISCUS -> drawHibiscus(baseColor, accentColor, centerColor, r)
            }
        }
    }
}


private fun DrawScope.drawRose(base: Color, accent: Color, center: Color, r: Float) {
    drawCircle(color = base, radius = r, center = Offset(0f, 0f))
    drawCircle(color = accent, radius = r * 0.72f, center = Offset(-r * 0.22f, -r * 0.22f))
    drawCircle(color = base.copy(alpha = 0.7f), radius = r * 0.36f, center = Offset(0f, 0f))
    drawCircle(color = Color(0xFFFFF0E8), radius = r * 0.14f, center = Offset(r * 0.07f, -r * 0.07f))
}


private fun DrawScope.drawLily(base: Color, accent: Color, center: Color, r: Float) {
    val petalColors = listOf(base, accent, base, accent, base, accent)
    val petalLen = r * 1.6f
    val petalWid = r * 0.6f
    for (i in 0 until 6) {
        val angle = i * 60f
        rotate(degrees = angle, pivot = Offset(0f, 0f)) {
            drawOval(
                color = petalColors[i],
                topLeft = Offset(-petalWid / 2f, -petalLen),
                size = Size(petalWid, petalLen)
            )
        }
    }
    drawCircle(color = center, radius = r * 0.32f, center = Offset(0f, 0f))
}


private fun DrawScope.drawDaffodil(base: Color, accent: Color, center: Color, r: Float) {
    val petalLen = r * 1.4f
    val petalWid = r * 0.7f
    for (i in 0 until 5) {
        val angle = i * 72f
        rotate(degrees = angle, pivot = Offset(0f, 0f)) {
            drawOval(
                color = base,
                topLeft = Offset(-petalWid / 2f, -petalLen),
                size = Size(petalWid, petalLen)
            )
        }
    }
    drawCircle(color = accent, radius = r * 0.55f, center = Offset(0f, 0f))
    drawCircle(color = Color(0xFFE89210), radius = r * 0.30f, center = Offset(0f, 0f))
}


private fun DrawScope.drawTulip(base: Color, accent: Color, center: Color, r: Float) {

    val outer = Path().apply {
        moveTo(-r, r * 0.3f)
        quadraticTo(-r * 1.4f, -r * 1.0f, 0f, -r * 1.3f)
        quadraticTo(r * 1.4f, -r * 1.0f, r, r * 0.3f)
        close()
    }
    drawPath(outer, color = base)

    val mid = Path().apply {
        moveTo(-r * 0.6f, r * 0.3f)
        quadraticTo(-r * 0.9f, -r * 0.85f, 0f, -r * 1.05f)
        quadraticTo(r * 0.9f, -r * 0.85f, r * 0.6f, r * 0.3f)
        close()
    }
    drawPath(mid, color = accent)

    val inner = Path().apply {
        moveTo(-r * 0.2f, r * 0.3f)
        quadraticTo(-r * 0.24f, -r * 0.65f, 0f, -r * 0.85f)
        quadraticTo(r * 0.24f, -r * 0.65f, r * 0.2f, r * 0.3f)
        close()
    }
    drawPath(inner, color = Color(0xFFFAABCE))
}


private fun DrawScope.drawHibiscus(base: Color, accent: Color, center: Color, r: Float) {
    val petalRadius = r * 0.7f
    val orbit = r * 0.55f
    for (i in 0 until 5) {
        val angle = (i * 72f - 90f) * (Math.PI / 180f).toFloat()
        val cx = cos(angle) * orbit
        val cy = sin(angle) * orbit
        drawCircle(color = base, radius = petalRadius, center = Offset(cx, cy))
    }
    drawCircle(color = center, radius = r * 0.28f, center = Offset(0f, 0f))
    drawLine(
        color = center,
        start = Offset(0f, 0f),
        end = Offset(0f, -r * 0.7f),
        strokeWidth = r * 0.11f
    )
    drawCircle(color = accent, radius = r * 0.12f, center = Offset(0f, -r * 0.7f))
}



private val GardenSkyTop = Color(0xFFB8DCEE)
private val GardenSkyBottom = Color(0xFFDCEFF5)
private val GardenSoil = Color(0xFF8A5A3A)
private val GardenSoilDeep = Color(0xFF5E3A22)
private val DarkSage = Color(0xFF4A6B52)

private fun priorityAccent(priority: PriorityType): Color = when (priority) {
    PriorityType.URGENT -> Color(0xFFD85A30)
    PriorityType.FOCUS -> Color(0xFF7A75C4)
    PriorityType.LOW -> Color(0xFFE2A52A)
    PriorityType.NORMAL -> Color(0xFFD4719A)
}

private fun priorityTint(priority: PriorityType): Color = when (priority) {
    PriorityType.URGENT -> Color(0xFFFBE7DF)
    PriorityType.FOCUS -> Color(0xFFE7E4F7)
    PriorityType.LOW -> Color(0xFFFAEEDA)
    PriorityType.NORMAL -> Color(0xFFFDE4EF)
}


private fun flowerPalette(species: FlowerSpecies): Triple<Color, Color, Color> = when (species) {
    FlowerSpecies.ROSE -> Triple(
        Color(0xFFD85A30), Color(0xFFE8785A), Color(0xFFF4C96B)
    )
    FlowerSpecies.LILY -> Triple(
        Color(0xFFC9B8E8), Color(0xFFE0D4F0), Color(0xFFF4C96B)
    )
    FlowerSpecies.DAFFODIL -> Triple(
        Color(0xFFF5D85A), Color(0xFFF5A623), Color(0xFFE89210)
    )
    FlowerSpecies.TULIP -> Triple(
        Color(0xFFE8699A), Color(0xFFF28AB3), Color(0xFFFAABCE)
    )
    FlowerSpecies.HIBISCUS -> Triple(
        Color(0xFFE85A8E), Color(0xFFF4C96B), Color(0xFFF4C96B)
    )
}

private fun priorityChipColors(priority: PriorityType): Pair<Color, Color> =
    priorityTint(priority) to priorityAccent(priority).copy(alpha = 0.95f)

private fun taskSupportText(priority: PriorityType, isDone: Boolean): String {
    if (isDone) {
        return when (priority) {
            PriorityType.URGENT -> "Bloomed — a rose for you 🌹"
            PriorityType.FOCUS -> "Bloomed — a lily 🪻"
            PriorityType.LOW -> "Bloomed — a daffodil 🌼"
            PriorityType.NORMAL -> "Bloomed — a tulip 🌷"
        }
    }
    return when (priority) {
        PriorityType.URGENT -> "Time-sensitive — good to do first"
        PriorityType.FOCUS -> "Focused work — best with calm attention"
        PriorityType.LOW -> "Low-pressure — nice for lower energy"
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
    var selectedPriority by remember { mutableStateOf(PriorityType.NORMAL) }
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
                    .thenBy { displayTaskText(it.text).lowercase(Locale.getDefault()) }
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
                title = {
                    Column {
                        Text(
                            text = "Today's Garden",
                            fontFamily = PacificoFont,
                            color = DarkSage,
                            fontSize = 25.sp
                        )
                        Text(
                            text = when {
                                total == 0 -> ""
                                remaining == 0 -> "Everything's bloomed 🌷"
                                else -> "$remaining to tend • $completed bloomed"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkSage.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedPriority = PriorityType.NORMAL
                    showAddDialog = true
                },
                containerColor = Color(0xFF7B969F),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add a task"
                )
            }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GardenStripCard(
                    items = emptyList(),
                    remaining = remaining,
                    completed = completed,
                    progress = progress
                )

                SoftPlanCard(
                    onPriorityClick = { priority ->
                        selectedPriority = priority
                        showAddDialog = true
                    }
                )

                EmptyTodoState(
                    onAddClick = {
                        selectedPriority = PriorityType.NORMAL
                        showAddDialog = true
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
            ) {
                item(key = "garden-strip") {
                    GardenStripCard(
                        items = sortedItems,
                        remaining = remaining,
                        completed = completed,
                        progress = progress
                    )
                }

                item(key = "soft-plan") {
                    SoftPlanCard(
                        onPriorityClick = { priority ->
                            selectedPriority = priority
                            showAddDialog = true
                        }
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
                            title = "Bloomed today 🌷",
                            subtitle = "Little wins — they all count"
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
                title = "Plant a task",
                confirmText = "Plant",
                initialTask = "",
                initialDate = "",
                initialTime = "",
                initialPriority = selectedPriority,
                onDismiss = { showAddDialog = false },
                onConfirm = { task, date, time, priority ->
                    onAdd(buildTaskText(task, date, time, priority))
                    showAddDialog = false
                }
            )
        }

        editing?.let { todo ->
            TaskDialog(
                title = "Edit task",
                confirmText = "Save",
                initialTask = displayTaskText(todo.text),
                initialDate = displayDueDate(todo.text),
                initialTime = displayDueTime(todo.text),
                initialPriority = detectPriority(todo.text),
                onDismiss = { editing = null },
                onConfirm = { task, date, time, priority ->
                    onEdit(todo.id, buildTaskText(task, date, time, priority))
                    editing = null
                }
            )
        }
    }
}



@Composable
private fun GardenStripCard(
    items: List<TodoItem>,
    remaining: Int,
    completed: Int,
    progress: Float
) {


    val streakAchieved = completed >= 3
    val hibiscusIndex = if (streakAchieved && items.isNotEmpty()) {
        val displayed = items.take(10)
        val completedIndices = displayed.mapIndexedNotNull { i, t -> if (t.isDone) i else null }
        completedIndices.getOrNull(2) ?: -1
    } else -1


    val transition = rememberInfiniteTransition(label = "garden-breath")
    val breath by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F2E8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Your garden 🌿",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when {
                            items.isEmpty() -> "Plant your first seed below"
                            streakAchieved -> "Streak bloom unlocked! 🌺"
                            completed == 0 -> "Seeds waiting to bloom"
                            remaining == 0 -> "Full bloom, beautiful 🌷"
                            else -> "$completed of ${items.size} bloomed"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }

                if (items.isNotEmpty()) {
                    val pct = (progress * 100).toInt()
                    Surface(
                        color = Color(0xFFE4EFE6),
                        contentColor = Color(0xFF3B6D11),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "🌱 $pct%",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to GardenSkyTop,
                                0.50f to GardenSkyBottom,
                                0.55f to GardenSkyBottom,
                                0.56f to GardenSoil,
                                1.0f to GardenSoilDeep
                            )
                        )
                    )
            ) {



                SkyLayer(
                    modifier = Modifier.fillMaxSize()
                )



                GrassLayer(
                    modifier = Modifier.fillMaxSize()
                )

                if (items.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(3) {
                            FlowerSlot(
                                species = FlowerSpecies.TULIP,
                                isDone = false,
                                breath = 1f,
                                modifier = Modifier
                                    .alpha(0.3f)
                                    .size(width = 42.dp, height = 95.dp)
                            )
                        }
                    }
                } else {
                    val displayItems = items.take(10)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        displayItems.forEachIndexed { i, task ->
                            val priority = detectPriority(task.text)
                            val species = if (i == hibiscusIndex && task.isDone) {
                                FlowerSpecies.HIBISCUS
                            } else priority.flower

                            FlowerSlot(
                                species = species,
                                isDone = task.isDone,
                                breath = breath,
                                modifier = Modifier.size(width = 34.dp, height = 115.dp)
                            )
                        }
                    }
                }




                CritterLayer(
                    modifier = Modifier.fillMaxSize()
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                trackColor = Color(0xFFE8DDC8),
                color = Color(0xFF7FA99B)
            )
        }
    }
}


@Composable
private fun FlowerSlot(
    species: FlowerSpecies,
    isDone: Boolean,
    breath: Float,
    modifier: Modifier = Modifier
) {
    val growth = remember { Animatable(if (isDone) 1f else 0f) }

    LaunchedEffect(isDone) {
        if (isDone) {
            growth.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
            )
        } else {
            growth.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }
    }

    val (base, accent, center) = flowerPalette(species)
    val effectiveBreath = if (isDone) breath else 1f

    Canvas(modifier = modifier) {




        val slotHeight = size.height
        val unit = slotHeight * 0.14f

        val baseX = size.width / 2f
        val baseY = size.height - slotHeight * 0.05f
        translate(left = baseX, top = baseY) {
            drawScale(scaleX = effectiveBreath, scaleY = effectiveBreath, pivot = Offset(0f, 0f)) {
                drawFlower(
                    species = species,
                    growth = growth.value,
                    unit = unit,
                    baseColor = base,
                    accentColor = accent,
                    centerColor = center
                )
            }
        }
    }
}



@Composable
private fun SkyLayer(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height



        val sunCenter = Offset(w * 0.88f, h * 0.16f)
        val sunRadius = h * 0.075f


        drawCircle(
            color = Color(0xFFFFE6A8).copy(alpha = 0.35f),
            radius = sunRadius * 1.9f,
            center = sunCenter
        )

        drawCircle(
            color = Color(0xFFFFD877).copy(alpha = 0.6f),
            radius = sunRadius * 1.35f,
            center = sunCenter
        )

        drawCircle(
            color = Color(0xFFF7B956),
            radius = sunRadius,
            center = sunCenter
        )

        drawCircle(
            color = Color(0xFFFFD68A),
            radius = sunRadius * 0.55f,
            center = Offset(sunCenter.x - sunRadius * 0.18f, sunCenter.y - sunRadius * 0.18f)
        )




        drawCloud(
            center = Offset(w * 0.18f, h * 0.13f),
            sizePx = h * 0.06f
        )
        drawCloud(
            center = Offset(w * 0.45f, h * 0.20f),
            sizePx = h * 0.045f
        )
        drawCloud(
            center = Offset(w * 0.66f, h * 0.10f),
            sizePx = h * 0.05f
        )
    }
}


private fun DrawScope.drawCloud(center: Offset, sizePx: Float) {
    val cloudColor = Color.White.copy(alpha = 0.85f)
    val cloudShadow = Color(0xFFE8F0F4).copy(alpha = 0.55f)


    drawOval(
        color = cloudShadow,
        topLeft = Offset(center.x - sizePx * 1.6f, center.y + sizePx * 0.1f),
        size = Size(sizePx * 3.2f, sizePx * 0.5f)
    )


    drawCircle(
        color = cloudColor,
        radius = sizePx,
        center = Offset(center.x - sizePx * 0.7f, center.y)
    )
    drawCircle(
        color = cloudColor,
        radius = sizePx * 1.2f,
        center = Offset(center.x, center.y - sizePx * 0.15f)
    )
    drawCircle(
        color = cloudColor,
        radius = sizePx * 0.9f,
        center = Offset(center.x + sizePx * 0.8f, center.y)
    )
}



@Composable
private fun GrassLayer(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.55f

        val grassDark = Color(0xFF6B9A4A)
        val grassLight = Color(0xFF7AAA5A)




        val bladeCount = 50
        for (i in 0 until bladeCount) {
            val xFrac = (i + 0.5f) / bladeCount.toFloat()
            val baseX = xFrac * w



            val flowerSlots = 5
            val tooClose = (0 until flowerSlots).any { f ->
                val flowerX = (f + 0.5f) / flowerSlots.toFloat()
                kotlin.math.abs(xFrac - flowerX) < 0.022f
            }
            if (tooClose) continue


            val offset = when (i % 5) {
                0 -> -2.5f
                1 -> 1.8f
                2 -> -1.4f
                3 -> 2.4f
                else -> -2f
            }

            val len = when (i % 4) {
                0 -> 13f
                1 -> 9f
                2 -> 15f
                else -> 11f
            }
            val color = if (i % 2 == 0) grassLight else grassDark

            val path = Path().apply {
                moveTo(baseX, groundY)
                quadraticTo(
                    baseX + offset, groundY - len * 0.7f,
                    baseX + offset * 1.2f, groundY - len
                )
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 2.2f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
    }
}



@Composable
private fun CritterLayer(modifier: Modifier = Modifier) {


    var timeMs by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { now ->
                timeMs = (now - start).toFloat()
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val skyBottom = h * 0.5f
        val groundLine = h * 0.92f

        val tSec = timeMs / 1000f


        run {
            val period = 14f
            val phase = (tSec % period) / period
            val bx = -30f + phase * (w + 60f)
            val by = h * 0.10f + sin(tSec * 0.4f) * (h * 0.04f)
            val flap = sin(tSec * 4f) * 0.6f + 0.5f
            drawBird(Offset(bx, by), scale = 1.0f, flap = flap, alpha = 0.7f)
        }


        run {
            val period = 18f
            val phase = ((tSec + 7f) % period) / period
            val bx = -30f + phase * (w + 60f)
            val by = h * 0.06f + sin(tSec * 0.5f + 1f) * (h * 0.03f)
            val flap = sin(tSec * 4.6f + 0.7f) * 0.6f + 0.5f
            drawBird(Offset(bx, by), scale = 0.85f, flap = flap, alpha = 0.65f)
        }


        run {
            val period = 11f
            val phase = (tSec % period) / period
            val bx = -20f + phase * (w + 40f)
            val by = skyBottom * 0.55f + sin(tSec * 1.4f) * (h * 0.08f)

            val flap = (sin(tSec * 9f) * 0.5f + 0.5f) * 0.5f + 0.5f
            drawButterfly(Offset(bx, by), scale = 1f, wingScaleX = flap)
        }


        run {
            val cx = w / 2f
            val cy = h * 0.32f
            val bx = cx + sin(tSec * 1.1f) * (w * 0.42f)
            val by = cy + sin(tSec * 2.2f) * (h * 0.10f)

            val buzz = sin(tSec * 40f) * 0.5f + 0.5f
            drawBee(Offset(bx, by), scale = 1f, buzz = buzz)
        }


        run {
            val period = 22f
            val phase = (tSec % period) / period
            val cx = -20f + phase * (w + 40f)

            val pulse = sin(tSec * 4f) * 0.5f + 0.5f
            drawCaterpillar(Offset(cx, groundLine), scale = 1f, pulse = pulse)
        }


        run {
            val period = 28f

            val phase = (tSec % period) / period
            val lx = w + 20f - phase * (w + 40f)

            val ly = h * 0.96f + sin(tSec * 1.6f) * 1.5f
            drawLadybug(Offset(lx, ly), scale = 1f, facingLeft = true)
        }


        run {
            val period = 36f
            val phase = ((tSec + 12f) % period) / period
            val lx = -20f + phase * (w + 40f)
            val ly = h * 0.88f + sin(tSec * 1.9f + 1.4f) * 1.5f
            drawLadybug(Offset(lx, ly), scale = 0.85f, facingLeft = false)
        }
    }
}




private fun DrawScope.drawBird(
    pos: Offset,
    scale: Float,
    flap: Float,
    alpha: Float
) {
    val s = 22f * scale
    val color = Color(0xFF5A6B7A).copy(alpha = alpha)
    val flapY = (1f - flap) * s * 0.4f

    val path = Path().apply {
        moveTo(pos.x - s, pos.y + flapY * 0.5f)
        quadraticTo(
            pos.x - s * 0.4f, pos.y - s * 0.4f - flapY,
            pos.x, pos.y
        )
        quadraticTo(
            pos.x + s * 0.4f, pos.y - s * 0.4f - flapY,
            pos.x + s, pos.y + flapY * 0.5f
        )
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 4.5f * scale,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    )
}


private fun DrawScope.drawButterfly(
    pos: Offset,
    scale: Float,
    wingScaleX: Float
) {
    val s = 18f * scale
    val wingW = s * wingScaleX
    val pink = Color(0xFFE8699A)
    val pinkLight = Color(0xFFF28AB3)


    drawOval(
        color = pink.copy(alpha = 0.85f),
        topLeft = Offset(pos.x - wingW * 1.1f, pos.y - s * 1.1f),
        size = Size(wingW, s * 1.3f)
    )
    drawOval(
        color = pink.copy(alpha = 0.85f),
        topLeft = Offset(pos.x + 0.1f * wingW, pos.y - s * 1.1f),
        size = Size(wingW, s * 1.3f)
    )

    drawOval(
        color = pinkLight.copy(alpha = 0.85f),
        topLeft = Offset(pos.x - wingW * 0.9f, pos.y),
        size = Size(wingW * 0.8f, s)
    )
    drawOval(
        color = pinkLight.copy(alpha = 0.85f),
        topLeft = Offset(pos.x + 0.1f * wingW, pos.y),
        size = Size(wingW * 0.8f, s)
    )

    drawLine(
        color = Color(0xFF5A3D2A),
        start = Offset(pos.x, pos.y - s),
        end = Offset(pos.x, pos.y + s),
        strokeWidth = 2.8f * scale,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
}


private fun DrawScope.drawBee(
    pos: Offset,
    scale: Float,
    buzz: Float
) {
    val s = 13f * scale
    val yellow = Color(0xFFF4C96B)
    val black = Color(0xFF3A2A1F)
    val wing = Color(0xFFE8E8F0).copy(alpha = 0.7f)


    val wingScale = 0.85f + buzz * 0.3f
    drawOval(
        color = wing,
        topLeft = Offset(pos.x - s * 1.1f, pos.y - s * 1.1f * wingScale),
        size = Size(s * 1.4f, s * 0.9f * wingScale)
    )
    drawOval(
        color = wing,
        topLeft = Offset(pos.x - s * 0.3f, pos.y - s * 1.1f * wingScale),
        size = Size(s * 1.4f, s * 0.9f * wingScale)
    )


    drawOval(
        color = yellow,
        topLeft = Offset(pos.x - s, pos.y - s * 0.65f),
        size = Size(s * 2f, s * 1.3f)
    )

    drawRect(
        color = black,
        topLeft = Offset(pos.x - s * 0.7f, pos.y - s * 0.65f),
        size = Size(s * 0.4f, s * 1.3f)
    )
    drawRect(
        color = black,
        topLeft = Offset(pos.x + s * 0.15f, pos.y - s * 0.65f),
        size = Size(s * 0.4f, s * 1.3f)
    )
}


private fun DrawScope.drawCaterpillar(
    pos: Offset,
    scale: Float,
    pulse: Float
) {
    val segR = 10f * scale
    val segCount = 5
    val gap = segR * 1.5f
    val bodyColor = Color(0xFF7AAA5A)
    val bodyDark = Color(0xFF5C8A3F)
    val headColor = Color(0xFF3F6B2A)


    for (i in 0 until segCount) {
        val sx = pos.x + i * gap
        val bob = sin((pulse * 2f + i * 0.6f).toDouble()).toFloat() * scale * 2.5f
        val sy = pos.y - bob
        val color = if (i % 2 == 0) bodyColor else bodyDark
        drawCircle(color = color, radius = segR, center = Offset(sx, sy))
    }

    val headX = pos.x + segCount * gap
    val headY = pos.y - sin((pulse * 2f + segCount * 0.6f).toDouble()).toFloat() * scale * 2.5f
    drawCircle(color = headColor, radius = segR * 1.15f, center = Offset(headX, headY))
    drawCircle(
        color = Color.White,
        radius = segR * 0.25f,
        center = Offset(headX + segR * 0.4f, headY - segR * 0.3f)
    )
    drawCircle(
        color = Color.Black,
        radius = segR * 0.13f,
        center = Offset(headX + segR * 0.45f, headY - segR * 0.3f)
    )
}


private fun DrawScope.drawLadybug(
    pos: Offset,
    scale: Float,
    facingLeft: Boolean
) {
    val s = 13f * scale
    val red = Color(0xFFD43A30)
    val black = Color(0xFF2A1F1A)


    val dir = if (facingLeft) -1f else 1f


    drawOval(
        color = Color(0xFF000000).copy(alpha = 0.10f),
        topLeft = Offset(pos.x - s * 1.2f, pos.y + s * 0.3f),
        size = Size(s * 2.4f, s * 0.5f)
    )


    drawOval(
        color = red,
        topLeft = Offset(pos.x - s, pos.y - s * 0.7f),
        size = Size(s * 2f, s * 1.4f)
    )


    drawLine(
        color = black,
        start = Offset(pos.x, pos.y - s * 0.7f),
        end = Offset(pos.x, pos.y + s * 0.7f),
        strokeWidth = scale * 1.5f
    )


    drawCircle(color = black, radius = s * 0.18f, center = Offset(pos.x - s * 0.45f, pos.y - s * 0.25f))
    drawCircle(color = black, radius = s * 0.18f, center = Offset(pos.x + s * 0.45f, pos.y - s * 0.25f))
    drawCircle(color = black, radius = s * 0.18f, center = Offset(pos.x - s * 0.4f, pos.y + s * 0.3f))
    drawCircle(color = black, radius = s * 0.18f, center = Offset(pos.x + s * 0.4f, pos.y + s * 0.3f))


    drawOval(
        color = black,
        topLeft = Offset(pos.x + dir * s * 0.7f - s * 0.35f, pos.y - s * 0.45f),
        size = Size(s * 0.7f, s * 0.6f)
    )
}



@Composable
private fun SoftPlanCard(
    onPriorityClick: (PriorityType) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2ECE2)),
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
                text = "Pick a kind of seed to plant — or tap + to add your own.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                PriorityPill(
                    priority = PriorityType.URGENT,
                    onClick = { onPriorityClick(PriorityType.URGENT) }
                )
                PriorityPill(
                    priority = PriorityType.FOCUS,
                    onClick = { onPriorityClick(PriorityType.FOCUS) }
                )
                PriorityPill(
                    priority = PriorityType.LOW,
                    onClick = { onPriorityClick(PriorityType.LOW) }
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2ECE2)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "No seeds yet 🌱",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap a priority above, or hit the + to plant your first task.",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onAddClick) {
                Text("Plant your first task")
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(top = 4.dp, start = 4.dp),
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
private fun PriorityPill(
    priority: PriorityType,
    onClick: (() -> Unit)? = null
) {
    val (bg, txt) = priorityChipColors(priority)
    Surface(
        color = bg,
        contentColor = txt,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 0.dp,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
    val accent = priorityAccent(priority)
    val tint = priorityTint(priority)

    val taskText = displayTaskText(item.text)
    val dueDate = displayDueDate(item.text)
    val dueTime = displayDueTime(item.text)

    var popped by remember { mutableStateOf(false) }

    LaunchedEffect(item.isDone) {
        if (item.isDone) {
            popped = true
            delay(120)
            popped = false
        }
    }

    val cardScale by animateFloatAsState(
        targetValue = when {
            popped -> 1.06f
            item.isDone -> 0.97f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (item.isDone) 0.65f else 1f,
        animationSpec = tween(220),
        label = "alpha"
    )

    val containerColor = if (item.isDone) Color(0xFFF4F0E8) else Color(0xFFFFFBF4)

    val cardBorder: BorderStroke? = if (isTopFocus && !item.isDone) {
        BorderStroke(1.5.dp, accent.copy(alpha = 0.55f))
    } else null



    val miniGrowth = remember { Animatable(if (item.isDone) 1f else 0f) }
    LaunchedEffect(item.isDone) {
        if (item.isDone) {
            miniGrowth.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        } else {
            miniGrowth.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .alpha(alpha)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(92.dp)
                    .background(accent)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {


                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(tint),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isDone || miniGrowth.value > 0.05f) {
                        val (b, a, c) = flowerPalette(priority.flower)
                        Canvas(modifier = Modifier.size(44.dp)) {

                            val unit = size.height * 0.18f
                            translate(left = size.width / 2f, top = size.height * 0.85f) {
                                drawFlower(
                                    species = priority.flower,
                                    growth = miniGrowth.value,
                                    unit = unit,
                                    baseColor = b,
                                    accentColor = a,
                                    centerColor = c
                                )
                            }
                        }
                    } else {
                        Text(text = priority.emoji, fontSize = 18.sp)
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEdit() },
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (isTopFocus && !item.isDone) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = accent
                            )
                            Text(
                                text = "Main task",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = taskText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.SemiBold,
                        textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                    )

                    if (dueDate.isNotBlank() || dueTime.isNotBlank()) {
                        Text(
                            text = listOf(dueDate, dueTime).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = accent,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = taskSupportText(priority, item.isDone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        fontStyle = if (item.isDone) FontStyle.Italic else FontStyle.Normal
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Checkbox(
                        checked = item.isDone,
                        onCheckedChange = { checked -> onToggle(checked) }
                    )
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskDialog(
    title: String,
    confirmText: String,
    initialTask: String,
    initialDate: String,
    initialTime: String,
    initialPriority: PriorityType,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, PriorityType) -> Unit
) {
    var task by remember { mutableStateOf(initialTask) }
    var dueDate by remember { mutableStateOf(initialDate) }
    var dueTime by remember { mutableStateOf(initialTime) }
    var selectedPriority by remember { mutableStateOf(initialPriority) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = task,
                    onValueChange = { task = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: study for exam") },
                    singleLine = true,
                    label = { Text("Task") }
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: Apr 10") },
                    singleLine = true,
                    label = { Text("Due date") }
                )

                OutlinedTextField(
                    value = dueTime,
                    onValueChange = { dueTime = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: 3:00 PM") },
                    singleLine = true,
                    label = { Text("Due time") }
                )

                Text(
                    text = "Choose section",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    SelectablePriorityPill(
                        priority = PriorityType.URGENT,
                        selected = selectedPriority == PriorityType.URGENT,
                        onClick = { selectedPriority = PriorityType.URGENT }
                    )
                    SelectablePriorityPill(
                        priority = PriorityType.FOCUS,
                        selected = selectedPriority == PriorityType.FOCUS,
                        onClick = { selectedPriority = PriorityType.FOCUS }
                    )
                    SelectablePriorityPill(
                        priority = PriorityType.LOW,
                        selected = selectedPriority == PriorityType.LOW,
                        onClick = { selectedPriority = PriorityType.LOW }
                    )
                    SelectablePriorityPill(
                        priority = PriorityType.NORMAL,
                        selected = selectedPriority == PriorityType.NORMAL,
                        onClick = { selectedPriority = PriorityType.NORMAL }
                    )
                }

                Text(
                    text = when (selectedPriority) {
                        PriorityType.URGENT -> "This will bloom as a rose 🌹 when done."
                        PriorityType.FOCUS -> "This will bloom as a lily 🪻 when done."
                        PriorityType.LOW -> "This will bloom as a daffodil 🌼 when done."
                        PriorityType.NORMAL -> "This will bloom as a tulip 🌷 when done."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        task.trim(),
                        dueDate.trim(),
                        dueTime.trim(),
                        selectedPriority
                    )
                },
                enabled = task.trim().isNotEmpty()
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

@Composable
private fun SelectablePriorityPill(
    priority: PriorityType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (bg, txt) = priorityChipColors(priority)

    Surface(
        color = if (selected) txt.copy(alpha = 0.16f) else bg,
        contentColor = txt,
        shape = RoundedCornerShape(999.dp),
        border = if (selected) BorderStroke(1.5.dp, txt) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(priority.emoji)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = priority.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}