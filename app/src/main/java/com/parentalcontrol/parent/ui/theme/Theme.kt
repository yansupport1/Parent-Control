package com.parentalcontrol.parent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PrimaryBlue = Color(0xFF2563EB)
private val SecondaryTeal = Color(0xFF0D9488)
private val ErrorRed = Color(0xFFDC2626)
private val WarningAmber = Color(0xFFD97706)

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryTeal,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryTeal,
    error = ErrorRed
)

@Composable
fun ParentAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

object AppColors {
    val warning = WarningAmber
}
