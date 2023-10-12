package dev.fr33zing.launcher.ui.components

import android.annotation.SuppressLint
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.NodeRow
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.nodeIndent
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.RelativeNodeOffset
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.ui.components.dialog.AddNodeDialog
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.util.rememberCustomIndication

@Composable
fun NodeRow(
    db: AppDatabase,
    navController: NavController,
    row: NodeRow,
    nodeOptionsVisibleIndex: Int?,
    index: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAddNodeDialogOpened: (RelativeNodePosition) -> Unit,
    onAddNodeDialogClosed: () -> Unit,
    onNewNodeKindChosen: (NodeKind) -> Unit,
) {
    with(row) {
        val preferences = Preferences(LocalContext.current)
        val localDensity = LocalDensity.current

        val fontSize by preferences.getFontSize()
        val spacing by preferences.getSpacing()
        val indent by preferences.getIndent()
        val lineHeight = with(localDensity) { fontSize.toDp() }

        val pressing = remember { mutableStateOf(false) }
        val userCanCreateWithin = remember {
            hasPermission(PermissionKind.Create, PermissionScope.Recursive)
        }
        val userCanCreateWithinParent = remember {
            parent?.hasPermission(PermissionKind.Create, PermissionScope.Recursive) ?: true
        }
        val isExpandedDir by
            remember(row) { derivedStateOf { node.kind == NodeKind.Directory && !collapsed } }
        val showCreateAboveButton by remember { derivedStateOf { userCanCreateWithinParent } }
        val showCreateBelowButton by remember {
            derivedStateOf { if (isExpandedDir) userCanCreateWithin else userCanCreateWithinParent }
        }

        val visible by remember { derivedStateOf { !((parent?.collapsed) ?: false) } }
        val showOptions = nodeOptionsVisibleIndex == index

        // TODO replace this with a custom Indication?
        val tapColor = node.kind.color(payload).copy(alpha = 0.15f)
        val tapColorAnimated by
            animateColorAsState(
                if (pressing.value) tapColor else Color.Transparent,
                animationSpec = if (pressing.value) snap() else tween(1000),
                label = "node tap alpha"
            )

        Column {
            if (showCreateAboveButton) {
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
                            RelativeNodePosition(node.nodeId, RelativeNodeOffset.Above)
                        )
                    },
                    onDialogClosed = onAddNodeDialogClosed,
                    onKindChosen = onNewNodeKindChosen
                )
            }

            AnimatedNodeVisibility(visible, modifier = Modifier.background(tapColorAnimated)) {
                Node(
                    db,
                    navController,
                    row,
                    visible,
                    fontSize,
                    lineHeight,
                    spacing,
                    indent,
                    depth,
                    showOptions,
                    onClick,
                    onLongClick,
                )
            }

            if (showCreateBelowButton) {
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
                                node.nodeId,
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Node(
    db: AppDatabase,
    navController: NavController,
    row: NodeRow,
    visible: Boolean,
    fontSize: TextUnit,
    lineHeight: Dp,
    spacing: Dp,
    indent: Dp,
    depth: Int,
    showOptions: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    with(row) {
        val icon = node.kind.icon(payload)
        val color = node.kind.color(payload)
        val lineThrough = node.kind.lineThrough(payload)

        val interactionSource = remember { MutableInteractionSource() }
        val indication = rememberCustomIndication(color)

        Box(
            Modifier.height(IntrinsicSize.Min)
                .combinedClickable(
                    enabled = visible,
                    interactionSource = interactionSource,
                    indication = indication,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(vertical = spacing / 2)
                        .absolutePadding(left = nodeIndent(depth, indent, lineHeight))
                        .then(modifier)
            ) {
                NodeIconAndText(fontSize, lineHeight, node.label, color, icon, lineThrough)
            }

            NodeOptionButtons(db, navController, showOptions, fontSize, lineHeight, row)
        }
    }
}

@Composable
fun NodeIconAndText(
    fontSize: TextUnit,
    lineHeight: Dp,
    label: String,
    color: Color,
    icon: ImageVector,
    lineThrough: Boolean = false,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Visible,
    @SuppressLint("ModifierParameter") textModifier: Modifier = Modifier
) {
    val iconSize = 1f
    Icon(
        icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(lineHeight * iconSize),
    )
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
        textDecoration = if (lineThrough) TextDecoration.LineThrough else null
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
    val expandFrom = if (below) Alignment.Bottom else Alignment.Top
    val dialogVisible = remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color)

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = expandFrom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = expandFrom) + fadeOut(),
        modifier =
            Modifier.fillMaxWidth().clickable(interactionSource, indication, enabled = visible) {
                dialogVisible.value = true
                onDialogOpened()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.padding(vertical = spacing / 2)
                    .absolutePadding(left = nodeIndent(depth, indent, lineHeight))
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
