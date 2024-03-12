package dev.fr33zing.launcher.ui.theme

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
import dev.fr33zing.launcher.ui.utility.dim
import dev.fr33zing.launcher.ui.utility.mix

private val catppuccinDark = Catppuccin.Frappe
private val catppuccinLight = Catppuccin.Latte

var background: Color = Color.Black
var foreground: Color = Color.White
var dim: Color = foreground.dim()

fun colorScheme(darkTheme: Boolean): ColorScheme {
    Catppuccin.current = if (darkTheme) catppuccinDark else catppuccinLight
    val baseColorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

    if (!darkTheme) {
        val temp = background
        background = foreground
        foreground = temp
    }

    with(Catppuccin.current) {
        return baseColorScheme.copy(
            background = background,
            error = red,
            errorContainer = base,
            onPrimary = background,
            onPrimaryContainer = crust,
            primary = pink,
            primaryContainer = mauve,
            secondaryContainer = yellow,
            surface = background,
            onSurface = foreground,
            onSurfaceVariant = foreground.mix(background, 0.25f),
            tertiaryContainer = teal,
            outline = foreground,
        )
    }
}

@Composable
fun LauncherTheme(
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
