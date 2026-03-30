package com.example.consolicalm.com.example.consolicalm.ui.theme



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val SlateBlue   = Color(0xFF7B969F)   // top left — dark slate
private val SoftTeal    = Color(0xFFAAC4C2)   // top right — muted teal
private val Cream       = Color(0xFFF5EFE8)   // middle left — warm cream
private val MintWhite   = Color(0xFFEDF4F0)   // middle right — cool white
private val PowderBlue  = Color(0xFFBDD4DC)   // bottom — powder blue

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MintWhite,    // top — lightest
                        Cream,        // mid — warm breath
                        PowderBlue    // bottom — soft blue landing
                    )
                )
            )
    ) {
        content()
    }
}