package dev.fr33zing.launcher.data

import androidx.compose.runtime.MutableState
import dev.fr33zing.launcher.data.persistent.Node

class NodeRow(
    val node: Node,
    val parent: NodeRow?,
    var depth: Int,
    var collapsed: MutableState<Boolean>
)
