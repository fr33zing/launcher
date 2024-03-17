package dev.fr33zing.launcher.ui.components.tree.modal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.tree.modal.bar.BatchBottomBar
import dev.fr33zing.launcher.ui.components.tree.modal.bar.BatchTopBar
import dev.fr33zing.launcher.ui.components.tree.modal.bar.MoveBottomBar
import dev.fr33zing.launcher.ui.components.tree.modal.bar.MoveTopBar
import dev.fr33zing.launcher.ui.components.tree.modal.utility.MODAL_CLEAR_STATE_DELAY
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalAnimatedContent
import dev.fr33zing.launcher.ui.components.tree.modal.utility.modalFiniteAnimationSpec
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.conditional
import kotlinx.coroutines.delay

private val verticalPadding = 8.dp

@Composable
private fun ModalTopBarContent(
    treeState: TreeState,
    actions: ModalActions,
): Unit =
    when (treeState.mode) {
        TreeState.Mode.Batch -> BatchTopBar(treeState, actions)
        TreeState.Mode.Move -> MoveTopBar(treeState, actions)
        else -> {}
    }

@Composable
private fun ModalBottomBarContent(
    treeState: TreeState,
    actions: ModalActions,
): Unit =
    when (treeState.mode) {
        TreeState.Mode.Batch -> BatchBottomBar(treeState, actions)
        TreeState.Mode.Move -> MoveBottomBar(treeState, actions)
        else -> {}
    }

@Immutable
data class ModalActions(
    val endBatchSelect: () -> Unit,
    val batchSelectAll: () -> Unit,
    val batchDeselectAll: () -> Unit,
    val beginMove: () -> Unit,
    val cancelMove: () -> Unit,
)

enum class ModalBarPosition(
    val expandFrom: Alignment.Vertical,
) {
    Top(Alignment.Bottom),
    Bottom(Alignment.Top),
    ;

    val shrinkTowards = expandFrom
}

@Composable
fun ModalBar(
    position: ModalBarPosition,
    treeState: TreeState,
    actions: ModalActions,
) {
    val preferences = Preferences(LocalContext.current)
    val spacing by preferences.nodeAppearance.spacing.state

    // Keep content during hide animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(treeState.mode) {
        if (treeState.mode != TreeState.Mode.Normal) {
            visible = true
        } else {
            visible = false
            delay(MODAL_CLEAR_STATE_DELAY)
        }
    }

    val floatAnimSpec = modalFiniteAnimationSpec<Float>(treeState.mode)
    val intSizeAnimSpec = modalFiniteAnimationSpec<IntSize>(treeState.mode)

    AnimatedVisibility(
        visible,
        enter = fadeIn(floatAnimSpec) + expandVertically(intSizeAnimSpec, position.expandFrom),
        exit = fadeOut(floatAnimSpec) + shrinkVertically(intSizeAnimSpec, position.shrinkTowards),
    ) {
        Box {
            ModalAnimatedContent(
                state = treeState,
                mode = { it.mode },
                label = "modal bar: ${position.name}",
            ) {
                Box(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = ScreenHorizontalPadding, vertical = verticalPadding)
                        .conditional(position == ModalBarPosition.Top) {
                            absolutePadding(top = spacing / 2)
                        }
                        .conditional(position == ModalBarPosition.Bottom) {
                            absolutePadding(bottom = spacing / 2)
                        },
                ) {
                    when (position) {
                        ModalBarPosition.Top ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                ModalTopBarContent(it, actions)
                            }
                        ModalBarPosition.Bottom -> ModalBottomBarContent(it, actions)
                    }
                }
            }
        }
    }
}
