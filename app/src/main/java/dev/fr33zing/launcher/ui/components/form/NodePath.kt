package dev.fr33zing.launcher.ui.components.form

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
import dev.fr33zing.launcher.data.RootDirectoryColor
import dev.fr33zing.launcher.data.UnlabeledNodeColor
import dev.fr33zing.launcher.data.UnlabeledNodeText
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
    val annotatedString = buildAnnotatedString {
        nodeLineage.forEachIndexed { index, node ->
            // Don't show root node unless it's the only node in the lineage
            if (node.nodeId == ROOT_NODE_ID && nodeLineage.size > 1) return@forEachIndexed

            val color =
                if (node.label.isBlank()) UnlabeledNodeColor
                else if (node.nodeId == ROOT_NODE_ID) RootDirectoryColor else node.kind.color
            val label = node.label.ifBlank { UnlabeledNodeText }

            withStyle(SpanStyle(color = color)) { append(label.replace(' ', nbsp)) }
            if (index < nodeLineage.size - 1) appendInlineContent("delimiter", ">")
        }
    }

    Text(text = annotatedString, inlineContent = inlineContents, modifier = modifier)
}
