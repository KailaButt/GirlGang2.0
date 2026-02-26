package com.example.consolicalm.com.example.consolicalm.ui.theme


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConsoliCalmLogo(modifier: Modifier = Modifier) {

    // ---- Color Palette ----
    val cream = Color(0xFFF8F1EB)
    val slate = Color(0xFF7B969F)
    val sage  = Color(0xFFB9CBC4)
    val sky   = Color(0xFFCFE2E9)
    val mint  = Color(0xFFEEF7F1)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        // ---- Background + Circles ----
        Canvas(modifier = Modifier.matchParentSize()) {

            // Background
            drawRect(color = cream)

            val center = Offset(size.width / 2, size.height / 2)

            // Outer soft circle
            drawCircle(
                color = sage,
                radius = size.minDimension * 0.42f,
                center = center
            )

            // Main circle
            drawCircle(
                color = slate,
                radius = size.minDimension * 0.33f,
                center = center
            )

            // Inner glow
            drawCircle(
                color = sky,
                radius = size.minDimension * 0.24f,
                center = center
            )
        }

        // ---- Text Layer ----
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "CONSOLI",
                color = cream,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "C  A  L  M",
                color = mint,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}