package dev.fr33zing.launcher.ui.components.tree.modal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalAnimatedContent
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalClearStateDelay
import dev.fr33zing.launcher.ui.components.tree.modal.utility.modalFiniteAnimationSpec
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.conditional
import kotlinx.coroutines.delay

private val verticalPadding = 8.dp

private fun modalTopBarContent(
    treeState: TreeState,
    actions: ModalBarActions
): (@Composable RowScope.() -> Unit)? =
    when (treeState.mode) {
        TreeState.Mode.Batch -> {
            { BatchTopBar(treeState) }
        }
        else -> null
    }

private fun modalBottomBarContent(
    treeState: TreeState,
    actions: ModalBarActions
): (@Composable RowScope.() -> Unit)? =
    when (treeState.mode) {
        TreeState.Mode.Batch -> {
            { BatchBottomBar(treeState, actions) }
        }
        else -> null
    }

@Immutable
data class ModalBarActions(
    val batchSelectAll: () -> Unit,
    val batchDeselectAll: () -> Unit,
)

enum class ModalBarPosition(
    val content: (TreeState, ModalBarActions) -> (@Composable RowScope.() -> Unit)?,
    val expandFrom: Alignment.Vertical,
) {
    Top(::modalTopBarContent, Alignment.Bottom),
    Bottom(::modalBottomBarContent, Alignment.Top);

    val shrinkTowards = expandFrom
}

@Composable
fun ModalBar(position: ModalBarPosition, treeState: TreeState, actions: ModalBarActions) {
    val preferences = Preferences(LocalContext.current)
    val spacing by preferences.nodeAppearance.spacing.state

    // Keep content during hide animation
    var contentState by remember { mutableStateOf<@Composable (RowScope.() -> Unit)?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(treeState) {
        if (treeState.mode != TreeState.Mode.Normal) {
            contentState = position.content(treeState, actions)
            visible = true
        } else {
            visible = false
            delay(ModalClearStateDelay)
            contentState = null
        }
    }

    val floatAnimSpec = modalFiniteAnimationSpec<Float>(treeState.mode)
    val intSizeAnimSpec = modalFiniteAnimationSpec<IntSize>(treeState.mode)

    AnimatedVisibility(
        visible,
        enter = fadeIn(floatAnimSpec) + expandVertically(intSizeAnimSpec, position.expandFrom),
        exit = fadeOut(floatAnimSpec) + shrinkVertically(intSizeAnimSpec, position.shrinkTowards),
    ) {
        ModalAnimatedContent(
            state = treeState,
            mode = { it.mode },
            label = "modal bar: ${position.name}",
        ) {
            if (contentState != null)
                Row(
                    content = contentState!!,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(
                                horizontal = ScreenHorizontalPadding,
                                vertical = verticalPadding
                            )
                            .conditional(position == ModalBarPosition.Top) {
                                absolutePadding(top = spacing / 2)
                            }
                            .conditional(position == ModalBarPosition.Bottom) {
                                absolutePadding(bottom = spacing / 2)
                            }
                )
        }
    }
}
