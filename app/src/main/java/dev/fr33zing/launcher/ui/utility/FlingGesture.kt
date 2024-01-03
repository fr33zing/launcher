package dev.fr33zing.launcher.ui.utility

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import kotlin.math.abs
import kotlin.math.max

private suspend fun PointerInputScope.detectFling(direction: Int, onFling: () -> Unit) {
    // TODO make preferences for these
    val flingUpDragAmountThreshold = 75.dp
    val flingUpDragVelocityThreshold = 2.5.dp // per millisecond

    var cumulativeDragAmount = 0.dp
    var maxDragVelocity = 0.dp
    var flingUpGestureComplete = false

    detectVerticalDragGestures(
        onDragStart = {
            cumulativeDragAmount = 0.dp
            maxDragVelocity = 0.dp
            flingUpGestureComplete = false
        },
        onVerticalDrag = { change, dragAmountPx ->
            val elapsedMillis = change.uptimeMillis - change.previousUptimeMillis
            val dragAmountUpward = max(0f, dragAmountPx * direction)
            val dragAmount = dragAmountUpward.toDp()
            val dragVelocity = (dragAmountUpward / elapsedMillis).toDp()

            cumulativeDragAmount += dragAmount
            maxDragVelocity = max(maxDragVelocity, dragVelocity)

            if (
                !flingUpGestureComplete &&
                    cumulativeDragAmount >= flingUpDragAmountThreshold &&
                    maxDragVelocity >= flingUpDragVelocityThreshold
            ) {
                flingUpGestureComplete = true
                onFling()
            }
        }
    )
}

suspend fun PointerInputScope.detectFlingUp(onFlingUp: () -> Unit) = detectFling(-1, onFlingUp)

suspend fun PointerInputScope.detectFlingDown(onFlingUp: () -> Unit) = detectFling(1, onFlingUp)

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
