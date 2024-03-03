package dev.fr33zing.launcher.ui.utility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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

fun Modifier.blockPointerEvents() = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {}
    )
}

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

data class PaddingAndShadowHeight(val paddingHeight: Dp, val shadowHeight: Dp)

@Composable
fun rememberPaddingAndShadowHeight(): PaddingAndShadowHeight {
    val density = LocalDensity.current
    val statusBarsTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarsBottom =
        with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val verticalPadding =
        remember(WindowInsets.statusBars, WindowInsets.navigationBars) {
            listOf(statusBarsTop, navigationBarsBottom).max()
        }
    val hiddenRatio = 0.75f
    val shadowRatio = 1f - hiddenRatio
    val paddingHeight = verticalPadding * hiddenRatio
    val shadowHeight = verticalPadding * shadowRatio

    return remember(paddingHeight, shadowHeight) {
        PaddingAndShadowHeight(paddingHeight, shadowHeight)
    }
}

// Based on: https://stackoverflow.com/a/76244926
fun Modifier.deemphasize(): Modifier {
    val saturationMatrix = ColorMatrix().apply { setToSaturation(0f) }
    val saturationFilter = ColorFilter.colorMatrix(saturationMatrix)
    val paint =
        Paint().apply {
            colorFilter = saturationFilter
            alpha = 0.5f
        }

    return drawWithCache {
        val canvasBounds = Rect(Offset.Zero, size)
        onDrawWithContent {
            drawIntoCanvas {
                it.saveLayer(canvasBounds, paint)
                drawContent()
                it.restore()
            }
        }
    }
}
