package dev.fr33zing.launcher.data.viewmodel.utility

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import dev.fr33zing.launcher.TAG
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.getRootNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// data class TreeBrowserPage(
//    val root: NodePayloadStateHolder,
//    val children: NodePayloadListStateHolder
// )

data class TreeBrowserState(
    val root: NodePayloadStateHolder,
    val children: NodePayloadListStateHolder,
    val canTraverseUpward: Boolean,
    val direction: Direction = Direction.None,
) {
    enum class Direction(val depthChange: Int) {
        /** Not traversing, i.e. setting initial root directory */
        None(0),

        /** Traversing out of a directory */
        Upward(-1),

        /** Traversing into a directory */
        Inward(1)
    }
}

class TreeBrowserStateHolder(
    private val db: AppDatabase,
    private val scope: CoroutineScope,

    /** If true, tapping a directory will set the root node instead of calling onNodeClicked. */
    private val traverseDirectories: Boolean = true,

    /** If true, the user will be unable to traverse upward out of the initial directory. */
    private val containTraversalWithinInitialRoot: Boolean = true,

    /** If the predicate returns false, the node will be hidden. */
    private val nodeVisiblePredicate: (NodePayloadState) -> Boolean = { true },

    /** Function to call when a node is tapped, after ... */
    private val onNodeSelected: MutableState<(NodePayloadState) -> Unit> = mutableStateOf({}),

    /**
     * Function used to determine the initial directory. Defaults to global root node if no value is
     * provided.
     */
    private val initialRootNode: suspend () -> Node = db::getRootNode
) {
    private val rootNodeFlow = MutableSharedFlow<Pair<Node, TreeBrowserState.Direction>>()

    private var initialRootNodeId: Int? = null
    private var canTraverseUpward: Boolean = !containTraversalWithinInitialRoot

    val flow =
        rootNodeFlow
            .map { (rootNode, direction) ->
                canTraverseUpward =
                    !containTraversalWithinInitialRoot || rootNode.nodeId != initialRootNodeId

                val childNodes =
                    db.nodeDao().getChildNodes(rootNode.nodeId).filter {
                        val state = NodePayloadState.fromNode(db, it)
                        nodeVisiblePredicate(state)
                    }
                val rootStateHolder = NodePayloadStateHolder(db, rootNode)
                val childrenStateHolder =
                    NodePayloadListStateHolder(db, childNodes, nodeVisiblePredicate)

                TreeBrowserState(
                    root = rootStateHolder,
                    children = childrenStateHolder,
                    canTraverseUpward = canTraverseUpward,
                    direction = direction,
                )
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

    init {
        scope.launch {
            val initialRootNode = initialRootNode()
            initialRootNodeId = initialRootNode.nodeId
            rootNodeFlow.emit(Pair(initialRootNode, TreeBrowserState.Direction.None))
        }
    }

    private fun setRootNode(nodePayload: NodePayloadState, direction: TreeBrowserState.Direction) =
        scope.launch { rootNodeFlow.emit(Pair(nodePayload.node, direction)) }

    /** Move out of the current directory and into its parent. */
    fun traverseUpward(currentState: TreeBrowserState) {
        if (!currentState.canTraverseUpward) return
        currentState.root.node.parentId?.let { parentNodeId ->
            scope.launch {
                val parent = NodePayloadState.fromNodeId(db, parentNodeId)
                setRootNode(parent, TreeBrowserState.Direction.Upward)
            }
        }
    }

    private fun traverseInward(destination: NodePayloadState) =
        setRootNode(destination, TreeBrowserState.Direction.Inward)

    fun selectNode(nodePayload: NodePayloadState) {
        if (traverseDirectories && nodePayload.node.kind == NodeKind.Directory)
            traverseInward(nodePayload)
        else {
            Log.d(TAG, "ASDASD")
            onNodeSelected.value(nodePayload)
        }
    }

    fun onNodeSelected(callback: (NodePayloadState) -> Unit) {
        onNodeSelected.value = callback
    }
}
