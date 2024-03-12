package dev.fr33zing.launcher.ui.components.tree.modal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalAnimatedContent
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalNodeArguments
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.foreground
import dev.fr33zing.launcher.ui.utility.dim
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

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

private val indeterminateCheckboxColor = foreground.dim(0.85f)

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
            tint = indeterminateCheckboxColor,
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

private val confirmColor = Catppuccin.current.green

@Composable
private fun Move(arguments: ModalNodeArguments) {
    val (actions, treeState, treeNodeState) = arguments

    val moving = remember(arguments) { treeState.isMoving(treeNodeState.key) }

    // Keep state during hide animation
    var relevant by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { relevant = arguments.relevance == NodeRelevance.Relevant }

    if (moving) {
        Icon(
            Icons.Filled.CheckBox,
            contentDescription = "checked checkbox",
            tint = indeterminateCheckboxColor,
        )
    } else if (!relevant) {
        Icon(
            Icons.Filled.Close,
            contentDescription = "invalid destination",
            tint = indeterminateCheckboxColor,
        )
    } else {
        val movingCount =
            remember(treeState) { treeState.moveState?.movingKeys?.count { it.value } ?: 0 }
        val dialogVisible = remember { mutableStateOf(false) }
        val yesText =
            remember(treeState) {
                buildString {
                    append("Move ")
                    append(movingCount)
                    append(" item")
                    append(if (movingCount > 1) "s" else "")
                    append(" to \"")
                    append(arguments.treeNodeState.value.node.label.ifEmpty { "<Blank>" })
                    append("\"")
                }
            }
        YesNoDialog(
            visible = dialogVisible,
            icon = Icons.Filled.Check,
            yesText = yesText,
            yesColor = confirmColor,
            yesIcon = Icons.Filled.Check,
            noText = "Choose another destination",
            noIcon = Icons.Filled.ArrowBack,
            onYes = { actions.moveBatchSelectedNodes(treeNodeState) },
        )

        val interactionSource = remember { MutableInteractionSource() }
        val indication = rememberCustomIndication(color = confirmColor, circular = true)
        Icon(
            Icons.Filled.Check,
            contentDescription = "move button",
            tint = confirmColor,
            modifier = Modifier.clickable(interactionSource, indication) { dialogVisible.value = true },
        )
    }
}
