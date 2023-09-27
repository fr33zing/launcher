package com.example.mylauncher.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val catppuccinLight = Catppuccin.Latte

val DarkColorScheme = with(Catppuccin.Mocha) {
    darkColorScheme(
        background = Black,
        error = red,
        errorContainer = base,
        //inverseOnSurface =
        //inversePrimary =
        //inverseSurface =
        //onBackground =
        //onError =
        //onErrorContainer =
        onPrimary = yellow,
        onPrimaryContainer = crust,
        //onSecondary =
        //onSecondaryContainer =
        //onSurface =
        //onSurfaceVariant =
        //onTertiary =
        //onTertiaryContainer =
        //outline =
        //outlineVariant =
        primary = blue,
        primaryContainer = mauve,
        //scrim =
        //secondary =
        secondaryContainer = yellow,
        surface = Color(0xFF111111),
        //surfaceBright =
        //surfaceContainerHigh =
        //surfaceContainerHighest =
        //surfaceContainerLow =
        //surfaceContainerLowest =
        //surfaceDim =
        //surfaceTint =
        //surfaceVariant =
        //tertiary =
        tertiaryContainer = teal,
    )
}

val LightColorScheme = lightColorScheme(
    background = catppuccinLight.base

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun MyLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }

        darkTheme -> {
            Catppuccin.Current = Catppuccin.Mocha
            DarkColorScheme
        }

        else -> {
            Catppuccin.Current = Catppuccin.Frappe
            LightColorScheme
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme, typography = Typography, content = content
    )
}