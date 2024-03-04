package dev.fr33zing.launcher.ui.components.tree

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.IndeterminateCheckBox
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.viewmodel.state.NodeRelevance
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.conditional
import dev.fr33zing.launcher.ui.utility.dim
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

@Immutable
data class TreeModeSpecificActions(
    val activatePayload: () -> Unit,
    val selectNode: () -> Unit,
    val clearSelectedNode: () -> Unit,
    val toggleBatchSelected: () -> Unit
)

@Immutable
data class TreeModeSpecificArguments(
    val actions: TreeModeSpecificActions,
    val treeState: TreeState,
    val treeNodeState: TreeNodeState,
    val relevance: NodeRelevance
)

fun Modifier.treeModeSpecificModifier(arguments: TreeModeSpecificArguments) =
    when (arguments.treeState.mode) {
        TreeState.Mode.Normal -> normalModifier(arguments)
        TreeState.Mode.Batch -> batchModifier(arguments)
    }

@Composable
fun TreeModeSpecificInteractions(arguments: TreeModeSpecificArguments) {
    Box(Modifier.padding(horizontal = 16.dp)) {
        AnimatedContent(
            targetState = arguments,
            contentKey = { it.treeState.mode },
            label = "tree mode specific interactions",
            contentAlignment = Alignment.Center,
            transitionSpec = {
                val inDuration = 220
                val outDuration = 90
                val inDelay = 90
                val outDelay =
                    if (targetState.treeState.mode == TreeState.Mode.Normal) 1000 else outDuration

                fadeIn(animationSpec = tween(inDuration, delayMillis = inDelay))
                    .togetherWith(
                        fadeOut(animationSpec = tween(outDuration, delayMillis = outDelay))
                    )
            }
        ) { state ->
            when (state.treeState.mode) {
                TreeState.Mode.Normal -> {}
                TreeState.Mode.Batch -> BatchCheckbox(state)
            }
        }
    }
}

//
// Normal
//

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.normalModifier(arguments: TreeModeSpecificArguments) = composed {
    val (actions, _, treeNodeState, _) = arguments
    val interactionSource = remember { MutableInteractionSource() }
    val indication =
        rememberCustomIndication(LocalNodeAppearance.current.color, longPressable = true)
    val requireDoubleTapToActivateMessage =
        remember(treeNodeState.value.node.kind) {
            treeNodeState.value.node.kind.requiresDoubleTapToActivate()
        }
    val requireDoubleTapToActivate =
        remember(requireDoubleTapToActivateMessage) { requireDoubleTapToActivateMessage != null }

    this.combinedClickable(
        interactionSource = interactionSource,
        indication = indication,
        onClick = {
            actions.clearSelectedNode()

            if (!requireDoubleTapToActivate) actions.activatePayload()
            else
                sendNotice(
                    "double-tap-to-activate-node",
                    requireDoubleTapToActivateMessage
                        ?: throw Exception("requireDoubleTapToActivateMessage is null")
                )
        },
        onDoubleClick = if (requireDoubleTapToActivate) actions.activatePayload else null,
        onLongClick = actions.selectNode
    )
}

//
// Batch
//

private val indeterminateCheckboxColor = Foreground.dim(0.85f)
private val batchSelectedColor = Foreground.dim(0.8f)

private fun Modifier.batchModifier(arguments: TreeModeSpecificArguments) = composed {
    val (actions, treeState, treeNodeState, relevance) = arguments

    if (relevance != NodeRelevance.Relevant) return@composed this

    val haptics = LocalHapticFeedback.current
    val selected = treeState.isBatchSelected(treeNodeState.key)

    clickable(remember { MutableInteractionSource() }, indication = null) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            actions.toggleBatchSelected()
        }
        .conditional(selected) { background(batchSelectedColor) }
}

@Composable
private fun BatchCheckbox(arguments: TreeModeSpecificArguments) {
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
