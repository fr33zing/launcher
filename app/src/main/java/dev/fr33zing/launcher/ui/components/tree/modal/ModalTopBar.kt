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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalAnimatedContent
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

private val verticalPadding = 8.dp

private fun modalTopBarContent(treeState: TreeState): (@Composable RowScope.() -> Unit)? =
    when (treeState.mode) {
        TreeState.Mode.Batch -> {
            { BatchTopBar(treeState) }
        }
        else -> null
    }

@Composable
fun ModalTopBar(
    treeState: TreeState,
    modalContent: (TreeState) -> (@Composable RowScope.() -> Unit)?,
) {
    val preferences = Preferences(LocalContext.current)
    val spacing by preferences.nodeAppearance.spacing.state

    val content = modalContent(treeState)

    AnimatedVisibility(
        visible = content != null,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
    ) {
        ModalAnimatedContent(
            state = treeState,
            mode = { it.mode },
            label = "modal top bar",
        ) {
            if (content != null)
                Row(
                    content = content,
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
