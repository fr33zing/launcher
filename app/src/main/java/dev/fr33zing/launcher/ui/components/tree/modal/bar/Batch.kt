package dev.fr33zing.launcher.ui.components.tree.modal.bar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

@Composable
fun BatchTopBar(treeState: TreeState) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(circular = true, circularSizeFactor = 1f)

    val showMenu = remember { mutableStateOf(false) }
    val selectedCount =
        remember(treeState) { treeState.batchState?.selectedKeys?.count { it.value } ?: 0 }

    Text("$selectedCount items selected", fontWeight = FontWeight.Bold)
    Box {
        Icon(
            Icons.Filled.MoreHoriz,
            contentDescription = "menu button",
            modifier = Modifier.clickable(interactionSource, indication) { showMenu.value = true }
        )
    }
}

@Composable fun BatchBottomBar(treeState: TreeState) {}
