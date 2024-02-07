package dev.fr33zing.launcher.ui.components.node.next

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.utility.TreeState
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeInteractions(
    treeState: TreeState?,
    treeNodeState: TreeNodeState,
    features: NodeRowFeatureSet,
    onSelectNode: () -> Unit,
    onActivatePayload: () -> Unit = {},
    color: Color = LocalNodeAppearance.current.color,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color, longPressable = true)

    val hasFeature =
        remember(features) {
            object {
                val CREATE_ADJACENT = features.contains(NodeRowFeatures.CREATE_ADJACENT)
                val ACTION_BUTTONS = features.contains(NodeRowFeatures.ACTION_BUTTONS)
            }
        }

    Box(
        Modifier.fillMaxWidth()
            .height(IntrinsicSize.Min)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = onActivatePayload,
                onLongClick = onSelectNode
            )
    ) {
        content()

        AnimatedVisibility(
            visible = treeNodeState.key == treeState?.selectedKey,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            NodeActionButtonRow(treeNodeState)
        }
    }
}
