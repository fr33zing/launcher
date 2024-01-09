package dev.fr33zing.launcher.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Note
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.typography
import dev.fr33zing.launcher.ui.utility.mix
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows

private val spacing = 12.dp
private val padding = 20.dp

@Composable
fun NoteBodyDialog(visible: MutableState<Boolean>, node: Node, payload: Note) {
    BaseDialog(visible = visible, icon = NodeKind.Note.icon(), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.verticalScrollShadows(padding)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(padding)
            ) {
                Text(
                    node.label,
                    style =
                        typography.titleLarge.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    payload.body.ifEmpty { "Note has no body." },
                    color =
                        if (payload.body.isEmpty()) Foreground.mix(Background, 0.25f)
                        else Color.Unspecified,
                    fontStyle = if (payload.body.isEmpty()) FontStyle.Italic else FontStyle.Normal,
                    style =
                        typography.bodyLarge.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
