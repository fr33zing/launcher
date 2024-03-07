package dev.fr33zing.launcher.ui.components.tree.modal.bar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.tree.modal.ModalActions
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

private val closeButtonColor = Catppuccin.Current.red

@Composable
fun MoveTopBar(treeState: TreeState, actions: ModalActions) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication =
        rememberCustomIndication(circular = true, circularSizeFactor = 1f, color = closeButtonColor)
    val movingCount =
        remember(treeState) { treeState.moveState?.movingKeys?.count { it.value } ?: 0 }

    Text("Moving $movingCount items", fontWeight = FontWeight.Bold)
    Box {
        Icon(
            Icons.Filled.Close,
            contentDescription = "close button",
            tint = closeButtonColor,
            modifier =
                Modifier.clickable(interactionSource, indication, onClick = actions.endBatchMove)
        )
    }
}
