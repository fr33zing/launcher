package com.example.mylauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.example.mylauncher.R

@OptIn(ExperimentalTextApi::class)
private val fontFamily = FontFamily(
    Font(
        R.font.roboto_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(300),
            FontVariation.width(110.0f),
        ),
    )
)

// Set of Material typography styles to start with
val defaultTypography = Typography()
val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily),

    titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily),

    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily),

    labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily)
)