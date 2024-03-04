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
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalArguments
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.conditional
import dev.fr33zing.launcher.ui.utility.dim
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

fun Modifier.modalNodeContainerModifier(arguments: ModalArguments) =
    when (arguments.treeState.mode) {
        TreeState.Mode.Normal -> normalModifier(arguments)
        TreeState.Mode.Batch -> batchModifier(arguments)
    }

//
// Normal
//

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.normalModifier(arguments: ModalArguments) = composed {
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

private val batchSelectedColor = Foreground.dim(0.8f)

private fun Modifier.batchModifier(arguments: ModalArguments) = composed {
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
