package dev.fr33zing.launcher.ui.utility

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import dev.fr33zing.launcher.ui.components.Notice
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.theme.Background

// https://stackoverflow.com/a/72554087
fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier =
    if (condition) then(modifier(Modifier)) else this

fun Modifier.longPressable(tapNotice: (() -> Notice)? = null, onLongPressed: () -> Unit) =
    composed {
        val haptics = LocalHapticFeedback.current
        this.pointerInput(onLongPressed) {
            detectTapGestures(
                onTap = {
                    tapNotice?.let {
                        sendNotice(tapNotice())
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPressed()
                }
            )
        }
    }

// TODO use this for NodeList?
fun Modifier.verticalScrollShadows(height: Dp) = drawWithContent {
    val h = height.toPx()
    drawContent()
    drawRect(
        brush =
            Brush.verticalGradient(
                0.0f to Background,
                1.0f to Color.Transparent,
                endY = h,
            ),
        blendMode = BlendMode.SrcAtop,
        size = Size(size.width, h),
    )
    drawRect(
        brush =
            Brush.verticalGradient(
                0.0f to Color.Transparent,
                1.0f to Background,
                startY = size.height - h,
                endY = size.height,
            ),
        blendMode = BlendMode.SrcAtop,
        size = Size(size.width, h),
        topLeft = Offset(0f, size.height - h),
    )
}

fun Modifier.wholeScreenVerticalScrollShadows() = composed {
    val density = LocalDensity.current
    val statusBarsTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarsBottom =
        with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val verticalPadding =
        remember(WindowInsets.statusBars, WindowInsets.navigationBars) {
            listOf(statusBarsTop, navigationBarsBottom).max()
        }
    val hiddenRatio = 0.666f
    val shadowRatio = 1f - hiddenRatio
    val shadowHeight = verticalPadding * shadowRatio

    verticalScrollShadows(shadowHeight)
}
