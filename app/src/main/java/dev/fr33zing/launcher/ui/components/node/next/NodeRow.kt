package dev.fr33zing.launcher.ui.components.node.next

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeState
import dev.fr33zing.launcher.ui.components.node.next.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.rememberNodeAppearance

@Composable
fun NodeRow(state: TreeNodeState, features: NodeRowFeatureSet, modifier: Modifier = Modifier) {
    val hasFeature =
        remember(features) {
            object {
                val RENDER_STATE = features.contains(NodeRowFeatures.RENDER_STATE)
            }
        }

    val (node) = state.nodePayload
    val dimensions = LocalNodeDimensions.current
    val appearance = rememberNodeAppearance(state.nodePayload, hasFeature.RENDER_STATE)
    val indent by remember { derivedStateOf { dimensions.indent * state.depth } }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    vertical = dimensions.spacing / 2,
                    horizontal = ScreenHorizontalPadding,
                )
                .padding(start = indent)
                .then(modifier),
    ) {
        NodeDetail(
            fontSize = dimensions.fontSize,
            lineHeight = dimensions.lineHeight,
            label = node.label,
            color = appearance.color,
            icon = appearance.icon,
            lineThrough = appearance.lineThrough,
        )
    }
}

@Composable
private fun NodeDetail(
    fontSize: TextUnit,
    lineHeight: Dp,
    label: String,
    color: Color,
    icon: ImageVector,
    lineThrough: Boolean,
    isValidReference: Boolean = false,
    @SuppressLint("ModifierParameter") textModifier: Modifier = Modifier
) {
    val inlineContentMap =
        mapOf(
            "reference" to
                inlineLabelContent(
                    icon = NodeKind.Reference.icon,
                    color = NodeKind.Reference.color,
                    contentDescription = "reference indicator",
                    fontSize = fontSize
                )
        )
    val text = buildAnnotatedString {
        append(label)
        if (isValidReference) {
            withStyle(SpanStyle(textDecoration = TextDecoration.None)) { append("  ") }
            appendInlineContent("reference", "→")
        }
    }
    val iconSize = remember { 1f }

    Icon(
        icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(lineHeight * iconSize),
    )
    Text(
        text = text,
        modifier =
            Modifier.offset(y = lineHeight * -0.1f) // HACK: Vertically align with icon
                .absolutePadding(left = lineHeight * 0.5f)
                .then(textModifier),
        color = color,
        fontSize = fontSize,
        softWrap = true,
        overflow = TextOverflow.Visible,
        textDecoration = if (lineThrough) TextDecoration.LineThrough else null,
        inlineContent = inlineContentMap
    )
}

@Composable
private fun inlineLabelContent(
    icon: ImageVector,
    color: Color,
    contentDescription: String,
    fontSize: TextUnit,
): InlineTextContent {
    val density = LocalDensity.current
    val size = remember { fontSize }
    val sizeDp = remember { with(density) { size.toDp() } }

    return InlineTextContent(
        Placeholder(
            width = size,
            height = size,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(sizeDp)
        )
    }
}
