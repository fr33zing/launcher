package dev.fr33zing.launcher.ui.components.tree.modal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalAnimatedContent
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalClearStateDelay
import dev.fr33zing.launcher.ui.components.tree.modal.utility.modalFiniteAnimationSpec
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication
import kotlinx.coroutines.delay

private val verticalPadding = 8.dp

private fun modalTopBarContent(treeState: TreeState): (@Composable RowScope.() -> Unit)? =
    when (treeState.mode) {
        TreeState.Mode.Batch -> {
            { BatchTopBar(treeState) }
        }
        else -> null
    }

private fun modalBottomBarContent(treeState: TreeState): (@Composable RowScope.() -> Unit)? =
    when (treeState.mode) {
        TreeState.Mode.Batch -> {
            { BatchTopBar(treeState) }
        }
        else -> null
    }

enum class ModalBarPosition(
    val content: (TreeState) -> (@Composable RowScope.() -> Unit)?,
    val expandFrom: Alignment.Vertical,
) {
    Top(::modalTopBarContent, Alignment.Bottom),
    Bottom(::modalBottomBarContent, Alignment.Top);

    val shrinkTowards = expandFrom
}

@Composable
fun ModalBar(position: ModalBarPosition, treeState: TreeState) {
    val preferences = Preferences(LocalContext.current)
    val spacing by preferences.nodeAppearance.spacing.state

    // Keep content during hide animation
    var contentState by remember { mutableStateOf<@Composable (RowScope.() -> Unit)?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(treeState) {
        if (treeState.mode != TreeState.Mode.Normal) {
            contentState = position.content(treeState)
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
                            .absolutePadding(top = spacing / 2)
                )
        }
    }
}

//
// Batch
//

@Composable
private fun BatchTopBar(treeState: TreeState) {
    val interactionSource = remember { MutableInteractionSource() }
    val openMenuIndication = rememberCustomIndication(circular = true, circularSizeFactor = 1f)
    val showMenu = remember { mutableStateOf(false) }

    val selectedCount =
        remember(treeState) { treeState.batchState?.selectedKeys?.count { it.value } ?: 0 }
    Text("$selectedCount items selected", fontWeight = FontWeight.Bold)

    Box {
        Icon(
            Icons.Filled.MoreHoriz,
            contentDescription = "menu button",
            modifier =
                Modifier.clickable(interactionSource, openMenuIndication) { showMenu.value = true }
        )
    }
}
