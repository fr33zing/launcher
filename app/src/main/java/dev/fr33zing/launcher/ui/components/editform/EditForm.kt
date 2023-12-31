package dev.fr33zing.launcher.ui.components.editform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Payload

val EditFormExtraPadding = 16.dp
val EditFormSpacing = 16.dp

@Composable
fun EditForm(db: AppDatabase, innerPadding: PaddingValues, node: Node, payload: Payload) {
    when (node.kind) {
        NodeKind.Application -> ApplicationEditForm(innerPadding, payload, node)
        NodeKind.Checkbox -> CheckboxEditForm(innerPadding, payload, node)
        NodeKind.Directory -> DirectoryEditForm(db, innerPadding, payload, node)
        NodeKind.Reference -> ReferenceEditForm(db, innerPadding, payload, node)
        NodeKind.WebLink -> WebLinkEditForm(innerPadding, payload, node)
        NodeKind.Location -> LocationEditForm(innerPadding, payload, node)
        NodeKind.Setting -> SettingEditForm(innerPadding, payload, node)
        NodeKind.Note -> NoteEditForm(innerPadding, payload, node)
        else -> DefaultEditForm(innerPadding, node)
    }
}

@Composable
fun EditFormColumn(
    innerPadding: PaddingValues,
    scrollable: Boolean = true,
    content: @Composable (ColumnScope.() -> Unit)
) {
    val scrollState = rememberScrollState()

    Box(Modifier.fillMaxHeight()) {
        Box(
            Modifier.fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState, enabled = scrollable)
                .height(IntrinsicSize.Max)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(EditFormSpacing),
                modifier =
                    Modifier.padding(innerPadding).padding(EditFormExtraPadding).fillMaxHeight(),
                content = content
            )
        }
    }
}
