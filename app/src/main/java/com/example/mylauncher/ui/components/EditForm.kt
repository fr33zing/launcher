package com.example.mylauncher.ui.components

import androidx.compose.runtime.Composable
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.data.persistent.Application
import com.example.mylauncher.data.persistent.Node
import com.example.mylauncher.data.persistent.Payload

@Composable
fun EditForm(node: Node, payload: Payload?) {
    if (payload != null)
        when (node.kind) {
            NodeKind.Application -> ApplicationEditForm(payload, node)
            else -> DefaultEditForm(node)
        }
    else DefaultEditForm(node)
}

@Composable
private fun DefaultEditForm(node: Node) {
    NodePropertyTextField(node::label)
}

@Composable
private fun ApplicationEditForm(
    payload: Payload?,
    node: Node,
) {
    val application = payload as Application
    NodePropertyTextField(node::label, defaultValue = application.appName, userCanRevert = true)
    NodePropertyTextField(application::appName)
    NodePropertyTextField(application::packageName)
    NodePropertyTextField(application::activityClassName, userCanRevert = true)
    NodePropertyTextField(application::userHandle)
}
