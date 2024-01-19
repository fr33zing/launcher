package dev.fr33zing.launcher.ui.components.node.next

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeState
import dev.fr33zing.launcher.ui.components.node.next.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.node.next.utility.LocalNodeRowFeatures
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding

@Composable
fun NodeRow(
    state: TreeNodeState,
    features: NodeRowFeatureSet = LocalNodeRowFeatures.current,
    modifier: Modifier = Modifier
) {
    val hasFeature =
        remember(features) {
            object {
                val RENDER_STATE = features.contains(NodeRowFeatures.RENDER_STATE)
                val ACTIVATE = features.contains(NodeRowFeatures.ACTIVATE)
            }
        }

    val (node) = state.nodePayload
    val dimensions = LocalNodeDimensions.current
    val indent by remember { derivedStateOf { dimensions.indent * state.depth } }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    vertical = dimensions.spacing / 2,
                    horizontal = ScreenHorizontalPadding,
                )
                .padding(start = indent),
    ) {
        NodeDetail(label = node.label)
    }
}
