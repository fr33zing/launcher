package com.example.mylauncher.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.mylauncher.ui.util.mix

private val catppuccinDark = Catppuccin.Frappe
private val catppuccinLight = Catppuccin.Latte

var Background: Color = Color.Black
var Foreground: Color = Color.White

fun colorScheme(darkTheme: Boolean): ColorScheme {
    Catppuccin.Current = if (darkTheme) catppuccinDark else catppuccinLight
    val baseColorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

    if (!darkTheme) {
        val temp = Background
        Background = Foreground
        Foreground = temp
    }

    with(Catppuccin.Current) {
        return baseColorScheme.copy(
            background = Background,
            error = red,
            errorContainer = base,
            onPrimary = yellow,
            onPrimaryContainer = crust,
            primary = pink,
            primaryContainer = mauve,
            secondaryContainer = yellow,
            surface = Background,
            onSurface = Foreground,
            onSurfaceVariant = Foreground.mix(Background, 0.25f),
            tertiaryContainer = teal,
            outline = Foreground,
        )
    }
}

@Composable
fun MyLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme(darkTheme),
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
