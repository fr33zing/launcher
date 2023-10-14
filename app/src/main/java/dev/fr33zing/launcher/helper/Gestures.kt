package dev.fr33zing.launcher.helper

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlin.math.abs

// Adapted from: https://stackoverflow.com/a/72668732
suspend fun PointerInputScope.detectZoom(
    changeScale: Float = 1f,
    onGesture: (zoom: Float) -> Unit,
) {
    awaitEachGesture {
        var zoom = 1f
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } < 2) continue
            val zoomChange = event.calculateZoom()

            if (!pastTouchSlop) {
                zoom *= zoomChange

                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                val zoomMotion = abs(1 - zoom) * centroidSize

                if (zoomMotion > touchSlop) {
                    pastTouchSlop = true
                }
            }

            if (pastTouchSlop) {
                if (zoomChange != 1f) {
                    val scaledChange =
                        if (changeScale == 1f) zoomChange
                        else {
                            (if (zoomChange > 1) {
                                1 + ((zoomChange - 1) * changeScale)
                            } else {
                                1 - ((1 - zoomChange) * changeScale)
                            })
                        }
                    onGesture(scaledChange)
                    event.changes.forEach { it.consume() }
                }
            }
        } while (event.changes.any { it.pressed })
    }
}
