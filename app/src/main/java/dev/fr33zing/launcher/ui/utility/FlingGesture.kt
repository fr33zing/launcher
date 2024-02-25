package dev.fr33zing.launcher.ui.utility

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import kotlin.math.abs

suspend fun PointerInputScope.detectFling(
    flingUpEnabled: () -> Boolean = { true },
    flingDownEnabled: () -> Boolean = { true },
    onFirstDown: (() -> Unit)? = null,
    onFlingUp: (() -> Unit)? = null,
    onFlingDown: (() -> Unit)? = null,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        if (onFirstDown != null) onFirstDown()

        val touchSlop = viewConfiguration.touchSlop

        val flingDragAmountThreshold = 75.dp
        val flingDragVelocityThreshold = 2.5.dp // per millisecond

        var cumulativeDragAmount = 0.dp
        var maxDragVelocity = 0.dp
        var flingGestureComplete = false

        do {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } != 1) continue

            val change = event.changes[0]
            val dragAmountPx = change.position.y - change.previousPosition.y

            if (dragAmountPx == 0f) continue
            else if (dragAmountPx < 0 && !flingUpEnabled()) continue
            else if (dragAmountPx > 0 && !flingDownEnabled()) continue

            val elapsedMillis = change.uptimeMillis - change.previousUptimeMillis
            val dragAmount = dragAmountPx.toDp()
            val dragVelocity = abs(dragAmountPx / elapsedMillis).toDp()

            cumulativeDragAmount += dragAmount
            maxDragVelocity = max(maxDragVelocity, dragVelocity)

            if (abs(dragAmountPx) < touchSlop) continue
            if (flingGestureComplete) continue

            change.consume()

            if (maxDragVelocity >= flingDragVelocityThreshold) {
                if (onFlingDown != null && cumulativeDragAmount >= flingDragAmountThreshold) {
                    flingGestureComplete = true
                    onFlingDown()
                } else if (onFlingUp != null && -cumulativeDragAmount >= flingDragAmountThreshold) {
                    flingGestureComplete = true
                    onFlingUp()
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

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
