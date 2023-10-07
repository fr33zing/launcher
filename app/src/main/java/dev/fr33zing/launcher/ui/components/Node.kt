package dev.fr33zing.launcher.ui.components

import android.annotation.SuppressLint
import android.view.ViewConfiguration.getLongPressTimeout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.NodeRow
import dev.fr33zing.launcher.data.nodeIndent
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.RelativeNodeOffset
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.helper.conditional
import dev.fr33zing.launcher.ui.components.dialog.AddNodeDialog
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Foreground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NodeRow(
    navController: NavController,
    row: NodeRow,
    nodeOptionsVisibleIndex: Int?,
    index: Int,
    onTapped: () -> Unit,
    onLongPressed: () -> Unit,
    onAddNodeDialogOpened: (RelativeNodePosition) -> Unit,
    onAddNodeDialogClosed: () -> Unit,
    onNewNodeKindChosen: (NodeKind) -> Unit,
) {
    val preferences = Preferences(LocalContext.current)
    val localDensity = LocalDensity.current

    val fontSize by preferences.getFontSize()
    val spacing by preferences.getSpacing()
    val indent by preferences.getIndent()
    val lineHeight = with(localDensity) { fontSize.toDp() }

    val pressing = remember { mutableStateOf(false) }

    with(row) {
        val visible by remember { derivedStateOf { !((parent?.collapsed?.value) ?: false) } }
        val showOptions = nodeOptionsVisibleIndex == index

        val tapColor = node.kind.color(collapsed.value).copy(alpha = 0.15f)
        val tapColorAnimated by
            animateColorAsState(
                if (pressing.value) tapColor else Color.Transparent,
                animationSpec = if (pressing.value) snap() else tween(1000),
                label = "node tap alpha"
            )

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
                onDialogOpened = {
                    onAddNodeDialogOpened(
                        RelativeNodePosition(row.node.nodeId, RelativeNodeOffset.Above)
                    )
                },
                onDialogClosed = onAddNodeDialogClosed,
                onKindChosen = onNewNodeKindChosen
            )

            AnimatedNodeVisibility(visible, modifier = Modifier.background(tapColorAnimated)) {
                Node(
                    navController,
                    node,
                    visible,
                    collapsed.value,
                    fontSize,
                    lineHeight,
                    spacing,
                    indent,
                    depth,
                    showOptions,
                    pressing,
                    onTapped,
                    onLongPressed,
                )
            }

            val isExpandedDir = node.kind == NodeKind.Directory && !row.collapsed.value
            AddNodeButton(
                fontSize,
                lineHeight,
                spacing,
                indent,
                depth = if (isExpandedDir) depth + 1 else depth,
                visible = showOptions,
                below = true,
                text = if (isExpandedDir) "Add item within" else "Add item below",
                onDialogOpened = {
                    onAddNodeDialogOpened(
                        RelativeNodePosition(
                            row.node.nodeId,
                            if (isExpandedDir) RelativeNodeOffset.Within
                            else RelativeNodeOffset.Below
                        )
                    )
                },
                onDialogClosed = onAddNodeDialogClosed,
                onKindChosen = onNewNodeKindChosen
            )
        }
    }
}

@Composable
fun Node(
    navController: NavController,
    node: Node,
    visible: Boolean,
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
    val icon = node.kind.icon(collapsed)
    val color = node.kind.color(collapsed)

    Box(Modifier.height(IntrinsicSize.Min)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(vertical = spacing / 2)
                    .absolutePadding(left = nodeIndent(depth, indent, lineHeight))
                    .conditional(visible) {
                        pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = true)
                                val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
                                val heldButtonJob =
                                    scope.launch {
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
                    }
                    .then(modifier)
        ) {
            NodeIconAndText(fontSize, lineHeight, node.label, color, icon)
        }

        NodeOptionButtons(navController, showOptions, fontSize, lineHeight, node)
    }
}

@Composable
fun NodeIconAndText(
    fontSize: TextUnit,
    lineHeight: Dp,
    label: String,
    color: Color,
    icon: ImageVector?,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Visible,
    @SuppressLint("ModifierParameter") textModifier: Modifier = Modifier
) {
    val iconSize = 0.9f
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
        text = label,
        modifier =
            Modifier.offset(y = lineHeight * -0.1f) // HACK: Vertically align with icon
                .absolutePadding(left = lineHeight * 0.5f)
                .then(textModifier),
        color = color,
        fontSize = fontSize,
        softWrap = softWrap,
        overflow = overflow,
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
    onDialogOpened: () -> Unit,
    onDialogClosed: () -> Unit,
    onKindChosen: (NodeKind) -> Unit,
) {

    val color = Foreground.copy(alpha = 0.5f)
    val extraSpacing = lineHeight * 0.8f
    val expandFrom = if (below) Alignment.Bottom else Alignment.Top

    val dialogVisible = remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = expandFrom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = expandFrom) + fadeOut(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.padding(vertical = spacing / 2)
                    .absolutePadding(
                        left = nodeIndent(depth, indent, lineHeight),
                        top = if (below) extraSpacing else 0.dp,
                        bottom = if (!below) extraSpacing else 0.dp
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                dialogVisible.value = true
                                onDialogOpened()
                            }
                        )
                    }
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

    AddNodeDialog(
        visible = dialogVisible,
        onDismissRequest = onDialogClosed,
        onKindChosen = {
            dialogVisible.value = false
            onKindChosen(it)
        }
    )
}

@Composable
private fun NodeOptionButtons(
    navController: NavController,
    visible: Boolean,
    fontSize: TextUnit,
    lineHeight: Dp,
    node: Node,
) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        NodeOptionButtonsLayout(
            Modifier.fillMaxHeight()
                .background(Background.copy(alpha = 0.75f))
                .clickable(onClick = { /* Prevent tapping node underneath */})
        ) {
            NodeOptionButton(fontSize, lineHeight, Icons.Outlined.Delete, "Delete") {}
            NodeOptionButton(fontSize, lineHeight, Icons.Outlined.SwapVert, "Reorder") {
                navController.navigate("reorder/${node.parentId}")
            }
            NodeOptionButton(fontSize, lineHeight, Icons.Outlined.Edit, "Edit") {
                navController.navigate("edit/${node.nodeId}")
            }

            if (node.kind == NodeKind.Application) {
                NodeOptionButton(fontSize, lineHeight, Icons.Outlined.Info, "Info") {}
            }
        }
    }
}

@Composable
private fun NodeOptionButton(
    fontSize: TextUnit,
    lineHeight: Dp,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(horizontal = lineHeight * 0.5f).clickable(onClick = onClick)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
        ) {
            Icon(icon, text, modifier = Modifier.size(lineHeight * 1.15f))
            Text(text, fontSize = fontSize * 0.65f, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NodeOptionButtonsLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.minHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x =
                        ((constraints.maxWidth / placeables.size * (index + 0.5f)) -
                                placeable.width / 2)
                            .toInt(),
                    y = 0
                )
            }
        }
    }
}

@Composable
private fun AnimatedNodeVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val transformOriginTop = TransformOrigin.Center.copy(pivotFractionY = 0f)
    val scaleYAmount = 0.4f

    val visibleAsFloat = if (visible) 1f else 0f
    val animatedHeight by
        animateFloatAsState(
            targetValue = visibleAsFloat,
            animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
            label = "node visibility: height"
        )
    val animatedAlpha by
        animateFloatAsState(
            targetValue = visibleAsFloat,
            animationSpec =
                spring(stiffness = if (visible) Spring.StiffnessVeryLow else Spring.StiffnessLow),
            label = "node visibility: alpha"
        )

    Layout(
        modifier = Modifier.then(modifier),
        content = {
            Box(
                Modifier.graphicsLayer {
                    alpha = animatedAlpha
                    scaleY = animatedAlpha * scaleYAmount + (1f - scaleYAmount)
                    transformOrigin = transformOriginTop
                }
            ) {
                content()
            }
        }
    ) { measurables, constraints ->
        val child = measurables[0].measure(constraints)
        val contentHeight = child.height
        val containerHeight = (contentHeight * animatedHeight).toInt()
        val containerWidth = constraints.maxWidth

        layout(containerWidth, containerHeight) { child.placeRelative(0, 0) }
    }
}