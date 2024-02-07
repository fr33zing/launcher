package dev.fr33zing.launcher.ui.components.dialog

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.fr33zing.launcher.TAG
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.ui.components.tree.old.NodeIconAndText
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

enum class YesNoDialogBackAction {
    Dismiss,
    Yes,
    No
}

@Composable
fun YesNoDialog(
    visible: MutableState<Boolean>,
    icon: ImageVector,
    yesText: String,
    yesColor: Color,
    yesIcon: ImageVector,
    noText: String,
    noColor: Color = Color(0xFF888888),
    noIcon: ImageVector,
    backAction: YesNoDialogBackAction = YesNoDialogBackAction.Dismiss,
    onDismissRequest: () -> Unit = {},
    onYes: () -> Unit = {},
    onNo: () -> Unit = {},
) {
    val preferences = Preferences(LocalContext.current)
    val localDensity = LocalDensity.current
    val fontSize = preferences.nodeAppearance.fontSize.mappedDefault
    val lineHeight = with(localDensity) { fontSize.toDp() }

    BaseDialog(visible, icon, onDismissRequest = onDismissRequest) { padding ->
        BackHandler(enabled = backAction != YesNoDialogBackAction.Dismiss) {
            visible.value = false
            if (backAction == YesNoDialogBackAction.Yes) onYes() else onNo()
        }

        val verticalPadding = remember {
            padding - preferences.nodeAppearance.spacing.mappedDefault / 2
        }
        Column(modifier = Modifier.width(IntrinsicSize.Max).padding(vertical = verticalPadding)) {
            Option(visible, padding, fontSize, lineHeight, noText, noColor, noIcon, onNo)
            Option(visible, padding, fontSize, lineHeight, yesText, yesColor, yesIcon, onYes)
        }
    }
}

@Composable
private fun Option(
    visible: MutableState<Boolean>,
    padding: Dp,
    fontSize: TextUnit,
    lineHeight: Dp,
    text: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val preferences = Preferences(LocalContext.current)
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color = color)

    Box(
        Modifier.clickable(
            interactionSource,
            indication,
            onClick = {
                Log.d(TAG, "User selected YES: $text")
                visible.value = false
                onClick()
            }
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        horizontal = padding,
                        vertical = preferences.nodeAppearance.spacing.mappedDefault / 2
                    )
        ) {
            NodeIconAndText(
                fontSize = fontSize,
                lineHeight = lineHeight,
                label = text,
                color = color,
                icon = icon,
                softWrap = false,
            )
        }
    }
}
