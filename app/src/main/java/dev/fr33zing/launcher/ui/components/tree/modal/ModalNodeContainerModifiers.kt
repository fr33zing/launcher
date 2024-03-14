package dev.fr33zing.launcher.ui.components.tree.modal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.fr33zing.launcher.data.viewmodel.state.NodeRelevance
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalNodeArguments
import dev.fr33zing.launcher.ui.theme.foreground
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.conditional
import dev.fr33zing.launcher.ui.utility.dim
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

fun Modifier.modalNodeContainerModifier(arguments: ModalNodeArguments) =
    when (arguments.treeState.mode) {
        TreeState.Mode.Normal -> normalModifier(arguments)
        TreeState.Mode.Batch -> batchModifier(arguments)
        TreeState.Mode.Move -> moveModifier(arguments)
    }

//
// Normal
//

fun Modifier.normalModifier(arguments: ModalNodeArguments): Modifier {
    val (actions, _, treeNodeState) = arguments
    val (onActivatePayload, onSelectNode, onClearSelectedNode) = actions
    return normalNodeContainerModifier(treeNodeState, onActivatePayload, onSelectNode, onClearSelectedNode)
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.normalNodeContainerModifier(
    treeNodeState: TreeNodeState,
    onActivatePayload: () -> Unit = {},
    onSelectNode: () -> Unit = {},
    onClearSelectedNode: () -> Unit = {},
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(LocalNodeAppearance.current.color, longPressable = true)
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
            onClearSelectedNode()

            if (!requireDoubleTapToActivate) {
                onActivatePayload()
            } else {
                sendNotice(
                    "double-tap-to-activate-node",
                    requireDoubleTapToActivateMessage
                        ?: throw Exception("requireDoubleTapToActivateMessage is null"),
                )
            }
        },
        onDoubleClick = if (requireDoubleTapToActivate) onActivatePayload else null,
        onLongClick = onSelectNode,
    )
}

//
// Batch
//

private val batchSelectedColor = foreground.dim(0.8f)

private fun Modifier.batchModifier(arguments: ModalNodeArguments) =
    composed {
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

//
// Move
//

private fun Modifier.moveModifier(arguments: ModalNodeArguments) =
    run {
        val (_, treeState, treeNodeState, relevance) = arguments

        if (relevance == NodeRelevance.Disruptive) return@run this

        val moving = treeState.isMoving(treeNodeState.key)

        conditional(arguments.relevance == NodeRelevance.Relevant) { normalModifier(arguments) }
            .conditional(moving) { background(batchSelectedColor) }
    }
