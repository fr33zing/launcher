package dev.fr33zing.launcher.ui.components.editform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.fr33zing.launcher.data.persistent.payloads.Reference
import dev.fr33zing.launcher.data.viewmodel.editform.EditReferenceViewModel
import dev.fr33zing.launcher.ui.components.OutlinedValue
import dev.fr33zing.launcher.ui.components.TreeBrowser
import dev.fr33zing.launcher.ui.components.node.NodePath
import dev.fr33zing.launcher.ui.components.node.NodePropertyTextField
import dev.fr33zing.launcher.ui.pages.EditFormArguments
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.mix
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows

@Composable
fun ReferenceEditForm(
    arguments: EditFormArguments,
    viewModel: EditReferenceViewModel = hiltViewModel()
) {
    val (padding, node, payload) = arguments
    val reference = payload as Reference

    LaunchedEffect(viewModel.selectedNode) { reference.targetId = viewModel.selectedNode?.nodeId }

    Column(
        verticalArrangement = Arrangement.spacedBy(EditFormSpacing),
        modifier = Modifier.padding(padding).padding(EditFormExtraPadding).fillMaxHeight(),
    ) {
        NodePropertyTextField(node::label)
        OutlinedValue(label = "Target", modifier = Modifier.fillMaxWidth()) { padding ->
            NodePath(viewModel.selectedNodePath, modifier = Modifier.padding(padding))
        }
        OutlinedValue(label = "Browser", modifier = Modifier.fillMaxWidth().weight(1f)) { padding ->
            // HACK: Not sure why this 22dp bottom padding is necessary
            Box(Modifier.padding(bottom = 22.dp)) {
                val shadowHeight = remember { 12.dp }
                Box(Modifier.verticalScrollShadows(shadowHeight)) {
                    Box(Modifier.verticalScroll(rememberScrollState())) {
                        TreeBrowser(
                            viewModel.treeBrowser,
                            modifier = Modifier.padding(vertical = shadowHeight),
                            additionalRowContent = { (node) ->
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Selection indicator",
                                    tint =
                                        if (node.nodeId == viewModel.selectedNode?.nodeId)
                                            Catppuccin.Current.green
                                        else Background.mix(Foreground, 0.2f)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
