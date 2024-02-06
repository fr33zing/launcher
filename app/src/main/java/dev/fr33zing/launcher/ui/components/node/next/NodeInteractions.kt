package dev.fr33zing.launcher.ui.components.node.next

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeState
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeInteractions(
    treeNodeState: TreeNodeState,
    features: NodeRowFeatureSet,
    onLongClick: () -> Unit,
    activate: () -> Unit = {},
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

    fun onClick() {
        activate()
    }

    fun onLongClick() {}

    Column(
        Modifier.fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = ::onClick,
                onLongClick = ::onLongClick
            )
    ) {
        content()
    }
}
