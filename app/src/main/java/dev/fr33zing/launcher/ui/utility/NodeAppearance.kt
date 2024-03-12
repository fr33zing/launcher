package dev.fr33zing.launcher.ui.utility

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.viewmodel.state.NodePayloadState
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.ui.theme.foreground

val LocalNodeAppearance =
    compositionLocalOf {
        NodeAppearance(
            color = foreground,
            icon = Icons.Filled.QuestionMark,
            lineThrough = false,
        )
    }

@Immutable
data class NodeAppearance(
    val color: Color,
    val icon: ImageVector,
    val lineThrough: Boolean,
)

@Composable
fun rememberNodeAppearance(
    kind: NodeKind,
    ignoreState: Boolean = false,
    showChildren: Boolean? = null,
): NodeAppearance =
    remember(kind, showChildren) {
        NodeAppearance(
            color = kind.color(null, ignoreState, showChildren ?: true),
            icon = kind.icon(null, ignoreState, showChildren ?: true),
            lineThrough = kind.lineThrough(null, ignoreState),
        )
    }

@Composable
fun rememberNodeAppearance(
    node: Node,
    payload: Payload? = null,
    ignoreState: Boolean = false,
    showChildren: Boolean? = null,
): NodeAppearance =
    remember(node, payload, showChildren) {
        NodeAppearance(
            color = node.kind.color(payload, ignoreState, showChildren ?: true),
            icon = node.kind.icon(payload, ignoreState, showChildren ?: true),
            lineThrough = node.kind.lineThrough(payload, ignoreState),
        )
    }

@Composable
fun rememberNodeAppearance(
    nodePayload: NodePayloadState,
    ignoreState: Boolean = false,
    showChildren: Boolean = false,
): NodeAppearance =
    rememberNodeAppearance(
        node = nodePayload.node,
        payload = nodePayload.payload,
        ignoreState = ignoreState,
        showChildren = showChildren,
    )

@Composable
fun rememberNodeAppearance(
    treeNodeState: TreeNodeState,
    ignoreState: Boolean = false,
): NodeAppearance =
    rememberNodeAppearance(
        node = treeNodeState.value.node,
        payload = treeNodeState.value.payload,
        showChildren = treeNodeState.showChildren.value,
        ignoreState = ignoreState,
    )
