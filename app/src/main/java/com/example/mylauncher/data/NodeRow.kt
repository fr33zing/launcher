package com.example.mylauncher.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.mylauncher.data.persistent.Node

class NodeRow(
    val node: Node,
    val parent: NodeRow?,
    var depth: Int,
    var collapsed: MutableState<Boolean> = mutableStateOf(false),
)
