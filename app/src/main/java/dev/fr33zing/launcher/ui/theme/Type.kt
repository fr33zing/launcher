package dev.fr33zing.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import dev.fr33zing.launcher.R

@OptIn(ExperimentalTextApi::class)
val MainFontFamily =
    FontFamily(
        Font(
            R.font.roboto_flex,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(300),
                    FontVariation.width(110.0f),
                ),
        )
    )

// Set of Material typography styles to start with
val defaultTypography = Typography()
val typography =
    Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = MainFontFamily),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = MainFontFamily),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = MainFontFamily),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = MainFontFamily),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = MainFontFamily),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = MainFontFamily),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = MainFontFamily),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = MainFontFamily),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = MainFontFamily),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = MainFontFamily),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = MainFontFamily),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = MainFontFamily),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = MainFontFamily),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = MainFontFamily),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = MainFontFamily)
    )
