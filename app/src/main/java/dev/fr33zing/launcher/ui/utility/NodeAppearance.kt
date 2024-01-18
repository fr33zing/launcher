package dev.fr33zing.launcher.ui.utility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.viewmodel.utility.NodePayloadState

data class NodeAppearance(
    val color: Color,
    val icon: ImageVector,
    val lineThrough: Boolean,
)

@Composable
fun rememberNodeAppearance(
    node: Node,
    payload: Payload? = null,
    ignoreState: Boolean = false
): NodeAppearance =
    remember(node, payload) {
        NodeAppearance(
            color = node.kind.color(payload, ignoreState),
            icon = node.kind.icon(payload, ignoreState),
            lineThrough = node.kind.lineThrough(payload, ignoreState)
        )
    }

@Composable
fun rememberNodeAppearance(
    nodePayload: NodePayloadState,
    ignoreState: Boolean = false
): NodeAppearance = rememberNodeAppearance(nodePayload.node, nodePayload.payload, ignoreState)