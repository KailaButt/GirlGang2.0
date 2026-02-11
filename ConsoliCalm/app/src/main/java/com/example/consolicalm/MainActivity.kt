package com.example.consolicalm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import com.example.consolicalm.ui.theme.ConsoliCalmTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConsoliCalmTheme {

                var calmPoints by remember { mutableIntStateOf(240) }


                var showRewards by remember { mutableStateOf(false) }


                var showTodo by remember { mutableStateOf(false) }


                var showMeditation by remember { mutableStateOf(false) }


                val todoItems = remember { mutableStateListOf<TodoItem>() }

                Scaffold { _ ->

                    when {
                        showRewards -> {
                            RewardsScreen(
                                calmPoints = calmPoints,
                                onBack = { showRewards = false }
                            )
                        }

                        showMeditation -> {
                            MeditationScreen(
                                onBack = { showMeditation = false }
                            )
                        }

                        showTodo -> {
                            TodoScreen(
                                items = todoItems,
                                onAdd = { text ->
                                    if (text.isNotBlank()) {
                                        todoItems.add(TodoItem(text = text.trim()))
                                    }
                                },
                                onToggle = { id, checked ->
                                    val idx = todoItems.indexOfFirst { it.id == id }
                                    if (idx != -1) {
                                        todoItems[idx] = todoItems[idx].copy(isDone = checked)
                                    }
                                },
                                onEdit = { id, newText ->
                                    val idx = todoItems.indexOfFirst { it.id == id }
                                    if (idx != -1 && newText.isNotBlank()) {
                                        todoItems[idx] = todoItems[idx].copy(text = newText.trim())
                                    }
                                },
                                onDelete = { id ->
                                    todoItems.removeAll { it.id == id }
                                },
                                onBack = { showTodo = false }
                            )
                        }

                        else -> {
                            HomeScreen(
                                calmPoints = calmPoints,
                                nextRewardGoal = 500,
                                onRewardsClick = { showRewards = true },


                                onTodoClick = { showTodo = true },

                                onMeditationClick = { showMeditation = true },

                                onEarnPoints = { earned -> calmPoints += earned }
                            )
                        }
                    }
                }
            }
        }
    }
}
