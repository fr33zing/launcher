package dev.fr33zing.launcher.ui.components.form.payload

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
import dev.fr33zing.launcher.ui.pages.EditFormArguments

val EditFormExtraPadding = 16.dp
val EditFormSpacing = 16.dp

@Composable
fun EditForm(arguments: EditFormArguments) {
    when (arguments.node.kind) {
        NodeKind.Application -> ApplicationEditForm(arguments)
        NodeKind.Checkbox -> CheckboxEditForm(arguments)
        NodeKind.Directory -> DirectoryEditForm(arguments)
        NodeKind.Reference -> ReferenceEditForm(arguments)
        NodeKind.WebLink -> WebLinkEditForm(arguments)
        NodeKind.Location -> LocationEditForm(arguments)
        NodeKind.Setting -> SettingEditForm(arguments)
        NodeKind.Note -> NoteEditForm(arguments)
        else -> DefaultEditForm(arguments)
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
