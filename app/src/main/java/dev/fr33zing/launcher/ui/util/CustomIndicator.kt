package dev.fr33zing.launcher.ui.util

import android.view.ViewConfiguration.getLongPressTimeout
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.fr33zing.launcher.ui.theme.Foreground
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.timerTask
import kotlin.math.hypot
import kotlinx.coroutines.launch

@Composable
fun rememberCustomIndication(
    color: Color = Foreground,
    overrideAlpha: Float = 0.333f,
    useHaptics: Boolean = true,
    circular: Boolean = false,
) = remember(color, overrideAlpha) { CustomIndication(color, overrideAlpha, useHaptics, circular) }

class CustomIndication(
    private val color: Color,
    private val overrideAlpha: Float,
    private val useHaptics: Boolean,
    private val circular: Boolean
) : Indication {
    private val longPressTimeout = getLongPressTimeout().toLong()
    private val timer = Timer()
    private var timerTask: TimerTask? = null

    private class CustomIndicationInstance(
        private val circular: Boolean,
        private val colorState: State<Color>,
    ) : IndicationInstance {

        override fun ContentDrawScope.drawIndication() {
            if (circular)
                drawCircle(color = colorState.value, radius = hypot(size.width, size.height) / 2)
            else drawRect(color = colorState.value, size = size)
            drawContent()
        }
    }

    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val haptics = LocalHapticFeedback.current
        val pressedColor = remember(color, overrideAlpha) { color.copy(overrideAlpha) }
        val animatedColor = remember { Animatable(Color.Transparent) }
        val colorState = remember(animatedColor) { animatedColor.asState() }
        val coroutineScope = rememberCoroutineScope()
        var pressed by remember { mutableStateOf(false) }

        fun onReleaseOrCancel() {
            if (!pressed) return
            pressed = false
            coroutineScope.launch {
                if (useHaptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                animatedColor.animateTo(Color.Transparent, animationSpec = tween(1000))
            }
        }

        suspend fun onPress() {
            if (pressed) return
            pressed = true
            animatedColor.snapTo(pressedColor)
            timerTask?.run { cancel() }
            timerTask = timerTask { onReleaseOrCancel() }
            timer.schedule(timerTask, longPressTimeout)
        }

        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> onPress()
                    is PressInteraction.Release -> onReleaseOrCancel()
                    is PressInteraction.Cancel -> onReleaseOrCancel()
                }
            }
        }

        return remember(interactionSource) { CustomIndicationInstance(circular, colorState) }
    }
}
