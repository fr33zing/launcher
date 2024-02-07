package dev.fr33zing.launcher.ui.components.tree.utility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fr33zing.launcher.data.persistent.Preferences

val LocalNodeDimensions = compositionLocalOf { NodeDimensions() }

@Immutable
data class NodeDimensions(
    private val fontSizeState: State<TextUnit> = derivedStateOf { 22.sp },
    private val indentState: State<Dp> = derivedStateOf { 0.dp },
    private val spacingState: State<Dp> = derivedStateOf { 0.dp },
    private val lineHeightState: State<Dp> = derivedStateOf { 0.dp },
) {
    val fontSize by fontSizeState
    val indent by indentState
    val spacing by spacingState
    val lineHeight by lineHeightState
}

@Composable
fun createLocalNodeDimensions(): NodeDimensions {
    val preferences = Preferences(LocalContext.current)
    val density = LocalDensity.current

    val fontSize = preferences.nodeAppearance.fontSize.state
    val indent = preferences.nodeAppearance.indent.state
    val spacing = preferences.nodeAppearance.spacing.state
    val lineHeight = remember { derivedStateOf { with(density) { fontSize.value.toDp() } } }

    return NodeDimensions(fontSize, indent, spacing, lineHeight)
}
