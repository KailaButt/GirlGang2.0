package com.example.consolicalm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.consolicalm.ui.theme.AppTheme

@Composable
fun ProfileScreen(
    selectedTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Profile", style = MaterialTheme.typography.titleLarge)
        }

        // Profile picture placeholder
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )
        }

        // Themes
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Themes", style = MaterialTheme.typography.titleMedium)

                Text(
                    "Pick a look for the app.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeButton(
                        label = "Default",
                        selected = selectedTheme == AppTheme.DEFAULT,
                        onClick = { onSelectTheme(AppTheme.DEFAULT) }
                    )
                    ThemeButton(
                        label = "Sage",
                        selected = selectedTheme == AppTheme.SAGE,
                        onClick = { onSelectTheme(AppTheme.SAGE) }
                    )
                    ThemeButton(
                        label = "Mocha",
                        selected = selectedTheme == AppTheme.MOCHA,
                        onClick = { onSelectTheme(AppTheme.MOCHA) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}