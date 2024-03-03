package dev.fr33zing.launcher.ui.components.tree

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

private val verticalPadding = 8.dp

@Composable
fun MultiSelectStatusBar(treeState: TreeState) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(circular = true, circularSizeFactor = 1f)
    val preferences = Preferences(LocalContext.current)
    val spacing by preferences.nodeAppearance.spacing.state

    AnimatedVisibility(
        visible = treeState.multiSelectState != null,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = ScreenHorizontalPadding, vertical = verticalPadding)
                    .absolutePadding(top = spacing / 2)
        ) {
            val selectedCount =
                remember(treeState) {
                    treeState.multiSelectState?.selectedKeys?.count { it.value } ?: 0
                }
            Text("$selectedCount items selected", fontWeight = FontWeight.Bold)

            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "menu button",
                modifier = Modifier.clickable(interactionSource, indication) {}
            )
        }
    }
}
