package dev.fr33zing.launcher.ui.components.tree.modal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fr33zing.launcher.ui.theme.foreground
import dev.fr33zing.launcher.ui.utility.dim
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication
import kotlin.math.roundToInt

private val fontSize = 14.sp
private val rowHeight = 44.dp
private val rowVerticalPadding = 8.dp
private const val ANIMATION_DURATION = 125

@Composable
fun ModalActionButton(
    label: String,
    icon: ImageVector,
    color: Color = foreground,
    enabled: Boolean = true,
    action: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication =
        rememberCustomIndication(color = color, circular = true, circularSizeFactor = 1.15f)
    val density = LocalDensity.current
    val fontSizeDp = remember { with(density) { fontSize.toDp() } }
    val animatedColor by
        animateColorAsState(
            targetValue = if (enabled) color else color.dim(0.6f),
            label = "modal action button color",
            animationSpec = tween(ANIMATION_DURATION),
        )

    ModalActionButtonLayout(
        Modifier.clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = indication,
            onClick = action,
        ),
    ) {
        AnimatedContent(
            targetState = icon,
            label = "modal action button icon",
            transitionSpec = {
                fadeIn(tween(ANIMATION_DURATION)).togetherWith(fadeOut(tween(ANIMATION_DURATION)))
            },
        ) { icon ->
            Icon(
                icon,
                label,
                tint = animatedColor,
                modifier = Modifier.size(fontSizeDp * 1.5f),
            )
        }

        AnimatedContent(
            targetState = label,
            label = "modal action button label",
            transitionSpec = {
                fadeIn(tween(ANIMATION_DURATION)).togetherWith(fadeOut(tween(ANIMATION_DURATION)))
            },
        ) { label ->
            Text(
                label,
                fontSize = fontSize,
                color = animatedColor,
                overflow = TextOverflow.Visible,
                softWrap = false,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ModalActionButtonRow(content: @Composable () -> Unit) {
    Box(Modifier.padding(vertical = rowVerticalPadding)) {
        ModalActionButtonRowLayout(Modifier.height(rowHeight), content)
    }
}

@Composable
private fun ModalActionButtonLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints(maxHeight = constraints.minHeight)) }
        layout(constraints.maxWidth, constraints.minHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = constraints.maxWidth / 2 - placeable.width / 2,
                    y = if (index == 0) 0 else constraints.maxHeight - placeable.height,
                )
            }
        }
    }
}

@Composable
private fun ModalActionButtonRowLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val itemHeight = rowHeight.toPx().roundToInt()
        val square = Constraints.fixed(itemHeight, itemHeight)
        val placeables = measurables.map { it.measure(square) }
        layout(constraints.maxWidth, constraints.minHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x =
                        (constraints.maxWidth / placeables.size * (index + 0.5f) - placeable.width / 2)
                            .toInt(),
                    y = constraints.maxHeight / 2 - placeable.height / 2,
                )
            }
        }
    }
}
