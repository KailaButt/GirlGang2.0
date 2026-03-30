package com.example.consolicalm.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ConsoliCalmLogo(modifier: Modifier = Modifier) {

    val sage  = Color(0xFFB9CBC4)
    val slate = Color(0xFF7B969F)
    val sky   = Color(0xFFCFE2E9)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {

            val center = Offset(size.width / 2, size.height / 2)

            drawCircle(
                color = sage,
                radius = size.minDimension * 0.42f,
                center = center
            )

            drawCircle(
                color = slate,
                radius = size.minDimension * 0.33f,
                center = center
            )

            drawCircle(
                color = sky,
                radius = size.minDimension * 0.24f,
                center = center
            )
        }
    }
}