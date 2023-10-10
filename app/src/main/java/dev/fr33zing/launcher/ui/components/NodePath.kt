package dev.fr33zing.launcher.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import kotlin.text.Typography.nbsp

@Composable
fun NodePath(db: AppDatabase, node: Node, lastNodeLabelState: MutableState<String>? = null) {
    val hierarchy = remember { mutableListOf<Node>() }
    val payloads = remember { mutableListOf<Payload>() }
    var annotatedString by remember { mutableStateOf<AnnotatedString?>(null) }
    val inlineContents = remember { mutableMapOf<String, InlineTextContent>() }

    LaunchedEffect(node, lastNodeLabelState?.value) {
        if (hierarchy.isEmpty()) {
            getHierarchy(db, node, ArrayList()).also {
                hierarchy += it
                hierarchy.forEachIndexed { index, node ->
                    payloads.add(
                        index,
                        db.getPayloadByNodeId(node.kind, node.nodeId)
                            ?: throw Exception("Payload is null")
                    )
                }
            }
        }

        annotatedString = buildAnnotatedString {
            hierarchy.forEachIndexed { index, node ->
                val payload = payloads.getOrNull(index)
                val label =
                    if (lastNodeLabelState != null && index == hierarchy.size - 1)
                        lastNodeLabelState.value
                    else node.label

                //                nodeIcon(node, payload, inlineContents)
                withStyle(SpanStyle(color = node.kind.color(payload, ignoreState = true))) {
                    append(label.replace(' ', nbsp))
                }
                if (index < hierarchy.size - 1) delimiter(node, inlineContents)
            }
        }
    }

    if (annotatedString != null) Text(text = annotatedString!!, inlineContent = inlineContents)
}

private fun AnnotatedString.Builder.nodeIcon(
    node: Node,
    payload: Payload?,
    inlineContents: MutableMap<String, InlineTextContent>,
) {
    val id = "${node.nodeId}-nodeIcon"
    inlineContents[id] =
        InlineTextContent(
            Placeholder(
                width = 1.25.em,
                height = 1.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Icon(
                    node.kind.icon(payload, ignoreState = true),
                    contentDescription = "node icon",
                    tint = node.kind.color(payload, ignoreState = true),
                    modifier = Modifier.fillMaxHeight()
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    appendInlineContent(id, "[icon]")
}

private fun AnnotatedString.Builder.delimiter(
    node: Node,
    inlineContents: MutableMap<String, InlineTextContent>,
) {
    val id = "${node.nodeId}-delimiter"
    inlineContents[id] =
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
    appendInlineContent(id, ">")
}

private suspend fun getHierarchy(
    db: AppDatabase,
    node: Node,
    list: ArrayList<Node>
): ArrayList<Node> {
    list.add(0, node)
    if (node.parentId == null) return list
    val parent = db.nodeDao().getNodeById(node.parentId!!) ?: throw Exception("Parent is null")
    return getHierarchy(db, parent, list)
}
