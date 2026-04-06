package com.example.consolicalm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.consolicalm.ui.theme.PacificoFont   // ← same font used in HomeScreen
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            LaunchedEffect(Unit) {
                delay(2500)
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                finish()
            }

            val cream = Color(0xFFF8F1EB)
            val sage  = Color(0xFFB9CBC4)
            val slate = Color(0xFF7B969F)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cream),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Canvas(modifier = Modifier.size(160.dp)) {
                        drawCircle(color = sage,  radius = size.minDimension / 2)
                        drawCircle(color = slate, radius = size.minDimension / 2.6f)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ---- Now uses PacificoFont to match the rest of the app ----
                    Text(
                        text       = "ConsoliCalm",
                        fontFamily = PacificoFont,
                        fontSize   = 32.sp,
                        letterSpacing = 1.sp,
                        color      = slate
                    )
                }
            }
        }
    }
}