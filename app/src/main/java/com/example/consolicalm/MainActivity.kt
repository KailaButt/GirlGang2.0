package com.example.consolicalm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import com.example.consolicalm.ui.theme.ConsoliCalmTheme
import com.example.consolicalm.ui.home.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConsoliCalmTheme {
                Scaffold { _ ->
                    HomeScreen()
                }
            }
        }
    }
}
