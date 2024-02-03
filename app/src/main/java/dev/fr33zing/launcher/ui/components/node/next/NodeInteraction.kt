package dev.fr33zing.launcher.ui.components.node.next

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.fr33zing.launcher.TAG
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeState
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeInteractions(
    state: TreeNodeState,
    color: Color = LocalNodeAppearance.current.color,
    nodeRow: @Composable () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color, longPressable = true)

    fun onClick() {
        Log.d(TAG, state.toString())
        state.nodePayload.activate(context)
    }

    fun onLongClick() {}

    Column(
        Modifier.fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = ::onClick,
                onLongClick = ::onLongClick
            )
    ) {
        nodeRow()
    }
}
