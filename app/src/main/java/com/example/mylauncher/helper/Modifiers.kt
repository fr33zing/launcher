package com.example.mylauncher.helper

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback

// https://stackoverflow.com/a/72554087
fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

fun Modifier.longPressable(onLongPressed: () -> Unit) =
    composed() {
        val haptics = LocalHapticFeedback.current
        pointerInput(onLongPressed) {
            detectTapGestures(
                onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPressed()
                }
            )
        }
    }
