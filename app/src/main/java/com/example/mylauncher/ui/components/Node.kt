package com.example.mylauncher.ui.components

import android.view.ViewConfiguration.getLongPressTimeout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.mylauncher.data.Node
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.data.NodeRow
import com.example.mylauncher.data.Preferences
import com.example.mylauncher.ui.util.nodeColor
import com.example.mylauncher.ui.util.nodeIcon
import com.example.mylauncher.ui.util.nodeIndent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NodeRow(
    row: NodeRow,
    nodeOptionsVisibleIndex: Int?,
    index: Int,
    onTapped: () -> Unit,
    onLongPressed: () -> Unit,
) {
    with(row) {
        val pressing = remember { mutableStateOf(false) }
        val visible = !((parent?.collapsed?.value) ?: false)
        val showOptions = nodeOptionsVisibleIndex == index

        val fontSize = Preferences.fontSizeDefault
        val spacing = Preferences.spacingDefault
        val indent = Preferences.indentDefault
        val lineHeight = with(LocalDensity.current) { fontSize.toDp() }

        val tapColor = nodeColor(row.node.kind, row.collapsed.value).copy(alpha = 0.15f)
        val tapColorAnimated by animateColorAsState(
            if (pressing.value) tapColor else Color.Transparent,
            animationSpec = if (pressing.value) snap() else tween(1000),
            label = "node tap alpha"
        )

        AnimatedVisibility(
            visible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                AddNodeButton(
                    fontSize,
                    lineHeight,
                    spacing,
                    indent,
                    depth,
                    visible = showOptions,
                    below = false,
                    text = "Add item above",
                )

                Box(Modifier.background(tapColorAnimated)) {
                    Node(
                        node = node,
                        collapsed = collapsed.value,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        spacing = spacing,
                        indent = indent,
                        depth = depth,
                        showOptions = showOptions,
                        pressing = pressing,
                        onTapped = onTapped,
                        onLongPressed = onLongPressed,
                    )
                }

                val isDir = node.kind == NodeKind.Directory
                AddNodeButton(
                    fontSize,
                    lineHeight,
                    spacing,
                    indent,
                    depth = if (isDir) depth + 1 else depth,
                    visible = showOptions,
                    below = true,
                    text = if (isDir) "Add item within" else "Add item below",
                )
            }
        }
    }
}

@Composable
fun Node(
    node: Node,
    collapsed: Boolean,
    fontSize: TextUnit,
    lineHeight: Dp,
    spacing: Dp,
    indent: Dp,
    depth: Int,
    showOptions: Boolean,
    pressing: MutableState<Boolean>,
    onTapped: () -> Unit,
    onLongPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val icon = nodeIcon(node.kind, collapsed)
    val color = nodeColor(node.kind, collapsed)

    Box(Modifier.height(IntrinsicSize.Min)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing / 2)
                .absolutePadding(left = nodeIndent(depth, indent, lineHeight))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = true)
                        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
                        val heldButtonJob = scope.launch {
                            delay(25)
                            pressing.value = true
                            delay(getLongPressTimeout().toLong() - 25)
                            // Long press
                            pressing.value = false
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPressed()
                        }
                        waitForUpOrCancellation()?.run {
                            // Tap
                            if (pressing.value) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTapped()
                            }
                        }
                        heldButtonJob.cancel()
                        pressing.value = false
                    }
                }
                .then(modifier)

        ) {
            NodeIconAndText(fontSize, lineHeight, node.label, color, icon)
        }

        NodeOptionButtons(showOptions, fontSize, lineHeight, node)
    }
}

@Composable
private fun NodeIconAndText(
    fontSize: TextUnit,
    lineHeight: Dp,
    label: String,
    color: Color,
    icon: ImageVector?,
) {
    val iconSize = 1.1f
    if (icon != null) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(lineHeight * iconSize),
        )
    } else {
        val iconPlaceholderSize = 0.5f
        val extraSpace = Modifier.width(lineHeight * (iconSize - iconPlaceholderSize) / 2)

        Spacer(extraSpace)
        Icon(
            Icons.Outlined.Circle,
            contentDescription = null,
            tint = color.copy(alpha = 0.3f),
            modifier = Modifier.size(lineHeight * 0.5f)
        )
        Spacer(extraSpace)
    }

    Text(
        label,
        Modifier
            .offset(y = lineHeight * -0.1f) // HACK: Vertically align with icon
            .padding(horizontal = lineHeight * 0.4f), color, fontSize
    )
}

@Composable
private fun AddNodeButton(
    fontSize: TextUnit,
    lineHeight: Dp,
    spacing: Dp,
    indent: Dp,
    depth: Int,
    visible: Boolean,
    below: Boolean,
    text: String,
) {
    val color = Color.White.copy(alpha = 0.5f)
    val extraSpacing = lineHeight * 0.8f
    val expandFrom = if (below) Alignment.Bottom else Alignment.Top

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = expandFrom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = expandFrom) + fadeOut(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = spacing / 2)
                .absolutePadding(
                    left = nodeIndent(depth, indent, lineHeight),
                    top = if (below) extraSpacing else 0.dp,
                    bottom = if (!below) extraSpacing else 0.dp
                )
        ) {
            NodeIconAndText(
                fontSize = fontSize,
                lineHeight = lineHeight,
                label = text,
                color = color,
                icon = Icons.Outlined.Add
            )
        }
    }
}

@Composable
private fun NodeOptionButtons(visible: Boolean, fontSize: TextUnit, lineHeight: Dp, node: Node) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        NodeOptionButtonsLayout(
            Modifier
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.75f))
        ) {
            NodeOptionButton(fontSize, lineHeight, Icons.Outlined.Delete, "Delete")
            NodeOptionButton(fontSize, lineHeight, Icons.Outlined.SwapVert, "Reorder")
            NodeOptionButton(fontSize, lineHeight, Icons.Outlined.Edit, "Edit")

            if (node.kind == NodeKind.App) {
                NodeOptionButton(fontSize, lineHeight, Icons.Outlined.Info, "Info")
            }
        }
    }
}

@Composable
private fun NodeOptionButton(fontSize: TextUnit, lineHeight: Dp, icon: ImageVector, text: String) {
    Column(
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, text, modifier = Modifier.size(lineHeight * 1.15f))
        Text(text, fontSize = fontSize * 0.65f, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NodeOptionButtonsLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.minHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = ((constraints.maxWidth / placeables.size * (index + 0.5f)) - placeable.width / 2).toInt(),
                    y = 0
                )
            }
        }
    }
}
