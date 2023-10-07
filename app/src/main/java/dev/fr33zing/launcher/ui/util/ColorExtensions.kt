package dev.fr33zing.launcher.ui.util

import androidx.compose.ui.graphics.Color

fun Color.mix(with: Color, withRatio: Float): Color {
    if (withRatio < 0f || withRatio > 1f) throw Exception("Value of ratio must be within 0 and 1")
    val thisRatio = 1f - withRatio
    return Color(
        red = this.red * thisRatio + with.red * withRatio,
        green = this.green * thisRatio + with.green * withRatio,
        blue = this.blue * thisRatio + with.blue * withRatio,
        alpha = this.alpha * thisRatio + with.alpha * withRatio
    )
}
