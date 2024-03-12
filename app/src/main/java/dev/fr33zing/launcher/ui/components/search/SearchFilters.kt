package dev.fr33zing.launcher.ui.components.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

@Composable
fun SearchFilters(
    nodeKindFilter: Map<NodeKind, Boolean>,
    updateFilter: (nodeKind: NodeKind, enabled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    lineHeight: Dp = LocalNodeDimensions.current.lineHeight,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        NodeKind.entries.forEach { nodeKind ->
            key(nodeKind) {
                val icon = remember(nodeKind) { nodeKind.icon }
                val color = remember(nodeKind) { nodeKind.color }
                val interactionSource = remember { MutableInteractionSource() }
                val indication =
                    rememberCustomIndication(
                        color = color,
                        circular = true,
                        circularSizeFactor = 1.45f,
                    )
                val enabled = nodeKindFilter.getOrDefault(nodeKind, false)

                Box(
                    Modifier.clickable(interactionSource, indication) {
                        updateFilter(nodeKind, !enabled)
                    }
                        .drawBehind {
                            if (enabled) {
                                val radius = 2.5.dp
                                val offsetY = 6.dp
                                drawCircle(
                                    color = color.copy(alpha = 0.5f),
                                    radius = radius.toPx(),
                                    center =
                                        Offset(
                                            x = size.width / 2,
                                            y = size.height + offsetY.toPx(),
                                        ),
                                )
                            }
                        },
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (enabled) color else color.copy(alpha = 0.5f),
                        modifier = Modifier.size(lineHeight),
                    )
                }
            }
        }
    }
}
