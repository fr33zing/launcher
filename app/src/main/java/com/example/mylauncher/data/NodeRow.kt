package com.example.mylauncher.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.mylauncher.data.persistent.AppDatabase
import com.example.mylauncher.data.persistent.Node

class NodeRow(
    val node: Node,
    val parent: NodeRow?,
    var depth: Int,
    var collapsed: MutableState<Boolean> = mutableStateOf(false),
)

suspend fun flattenNodes(db: AppDatabase): List<NodeRow> {
    val result = ArrayList<NodeRow>()

    suspend fun add(node: Node, parent: NodeRow?, depth: Int) {
        val row = NodeRow(node, parent, depth)
        result.add(row)

        db.nodeDao().getChildNodes(node.nodeId).forEach { add(it, row, depth + 1) }
    }

    db.nodeDao().getTopLevelNodes().forEach { add(it, null, 0) }

    return result
}
