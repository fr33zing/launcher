package com.example.mylauncher.ui.components.editforms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.example.mylauncher.data.persistent.Node
import com.example.mylauncher.ui.components.EditFormColumn
import com.example.mylauncher.ui.components.NodePropertyTextField

@Composable
fun DefaultEditForm(innerPadding: PaddingValues, node: Node) {
    EditFormColumn(innerPadding) { NodePropertyTextField(node::label) }
}