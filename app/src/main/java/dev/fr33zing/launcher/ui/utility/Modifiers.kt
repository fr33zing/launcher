package dev.fr33zing.launcher.ui.utility

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import dev.fr33zing.launcher.ui.components.Notice
import dev.fr33zing.launcher.ui.components.sendNotice

// https://stackoverflow.com/a/72554087
fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

fun Modifier.longPressable(tapNotice: (() -> Notice)? = null, onLongPressed: () -> Unit) =
    composed {
        val haptics = LocalHapticFeedback.current
        pointerInput(onLongPressed) {
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
                0.0f to Color.Black,
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
                1.0f to Color.Black,
                startY = size.height - h,
                endY = size.height,
            ),
        blendMode = BlendMode.SrcAtop,
        size = Size(size.width, h),
        topLeft = Offset(0f, size.height - h),
    )
}
