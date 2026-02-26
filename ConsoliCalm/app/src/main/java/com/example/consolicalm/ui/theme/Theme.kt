package com.example.consolicalm.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppTheme {
    DEFAULT,
    SAGE,
    MOCHA
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val SageLightScheme = lightColorScheme(
    primary = Color(0xFF2F6F63),
    secondary = Color(0xFF7A6F66),
    tertiary = Color(0xFF4A6FA5),
    background = Color(0xFFF6F7F4),
    surface = Color(0xFFF6F7F4),
    surfaceVariant = Color(0xFFE6EAE6),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF151614),
    onSurface = Color(0xFF151614),
    onSurfaceVariant = Color(0xFF37322D)
)

private val MochaLightScheme = lightColorScheme(
    primary = Color(0xFF7B5D4C),
    secondary = Color(0xFF6A625B),
    tertiary = Color(0xFF4A6FA5),
    background = Color(0xFFF8F5F2),
    surface = Color(0xFFF8F5F2),
    surfaceVariant = Color(0xFFEFE6DE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF171412),
    onSurface = Color(0xFF171412),
    onSurfaceVariant = Color(0xFF3A2F29)
)

@Composable
fun ConsoliCalmTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {

        appTheme == AppTheme.SAGE -> SageLightScheme
        appTheme == AppTheme.MOCHA -> MochaLightScheme


        dynamicColor && appTheme == AppTheme.DEFAULT &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }


        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}