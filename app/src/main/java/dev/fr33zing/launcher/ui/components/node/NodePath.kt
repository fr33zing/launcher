package dev.fr33zing.launcher.ui.components.node

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import kotlin.text.Typography.nbsp

@Composable
fun NodePath(
    nodeLineage: List<Node>,
    modifier: Modifier = Modifier,
) {
    val inlineContents = remember {
        mapOf(
            "delimiter" to
                InlineTextContent(
                    Placeholder(
                        width = 1.75.em,
                        height = 0.85.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    Icon(
                        Icons.Filled.ArrowForwardIos,
                        contentDescription = "right chevron",
                        modifier = Modifier.fillMaxSize()
                    )
                }
        )
    }
    val annotatedString =
        // HACK: does not update without toList
        remember(nodeLineage.toList()) {
            buildAnnotatedString {
                nodeLineage.forEachIndexed { index, node ->
                    // Don't show root node
                    if (node.nodeId == ROOT_NODE_ID) return@forEachIndexed

                    withStyle(SpanStyle(color = node.kind.color)) {
                        append(node.label.replace(' ', nbsp))
                    }
                    if (index < nodeLineage.size - 1) appendInlineContent("delimiter", ">")
                }
            }
        }

    Text(text = annotatedString, inlineContent = inlineContents, modifier = modifier)
}
