package dev.fr33zing.launcher.ui.components.tree.modal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IndeterminateCheckBox
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.viewmodel.state.NodeRelevance
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalAnimatedContent
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalNodeArguments
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.dim

@Composable
fun ModalNodeComponents(arguments: ModalNodeArguments) {
    Box(Modifier.padding(horizontal = 16.dp)) {
        ModalAnimatedContent(
            state = arguments,
            mode = { it.treeState.mode },
            label = "modal node components",
        ) { state ->
            when (state.treeState.mode) {
                TreeState.Mode.Batch -> Batch(state)
                TreeState.Mode.Move -> Move(state)
                else -> {}
            }
        }
    }
}

//
// Batch
//

private val indeterminateCheckboxColor = Foreground.dim(0.85f)

@Composable
private fun Batch(arguments: ModalNodeArguments) {
    val (_, treeState, treeNodeState, _) = arguments

    val selected = remember(arguments) { treeState.isBatchSelected(treeNodeState.key) }

    // Keep state during hide animation
    var canSelect by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { canSelect = arguments.relevance == NodeRelevance.Relevant }

    if (!canSelect) {
        Icon(
            Icons.Filled.IndeterminateCheckBox,
            contentDescription = "indeterminate checkbox",
            tint = indeterminateCheckboxColor
        )
    } else if (!selected) {
        Icon(Icons.Filled.CheckBoxOutlineBlank, contentDescription = "unchecked checkbox")
    } else {
        Icon(Icons.Filled.CheckBox, contentDescription = "checked checkbox")
    }
}

//
// Move
//

@Composable
private fun Move(arguments: ModalNodeArguments) {
    val (_, treeState, treeNodeState, _) = arguments

    val moving = remember(arguments) { treeState.isMoving(treeNodeState.key) }

    // Keep state during hide animation
    var relevant by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { relevant = arguments.relevance == NodeRelevance.Relevant }

    if (moving) {
        Icon(
            Icons.Filled.CheckBox,
            contentDescription = "checked checkbox",
            tint = indeterminateCheckboxColor
        )
    } else if (!relevant) {
        Icon(
            Icons.Filled.Close,
            contentDescription = "invalid destination",
            tint = indeterminateCheckboxColor
        )
    } else {
        Icon(
            Icons.Filled.Check,
            contentDescription = "move button",
            tint = Catppuccin.Current.green
        )
    }
}
