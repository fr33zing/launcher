package dev.fr33zing.launcher.data.viewmodel.utility

import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.getRootNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// data class TreeBrowserPage(
//    val root: NodePayloadStateHolder,
//    val children: NodePayloadListStateHolder
// )

data class TreeBrowserState(
    //    val previous: TreeBrowserPage?,
    //    val current: TreeBrowserPage,

    val root: NodePayloadStateHolder,
    val children: NodePayloadListStateHolder,
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

    //    /** Node to use as the initial browsing destination. Defaults to global root node if null.
    // */
    //    initialRootNode: Node? = null,

    /** If true, tapping a directory will set the root node instead of calling onNodeClicked. */
    private val traverseDirectories: Boolean = true,

    /** If the predicate returns false, the node will be hidden. */
    private val nodeVisiblePredicate: (NodePayloadState) -> Boolean = { true },

    /** Function to call when a node is tapped, after ... */
    private val onNodeSelected: (NodePayloadState) -> Unit = {},

    /**
     * Function used to determine the initial browsing destination. Defaults to global root node if
     * no value is provided.
     */
    private val initialRootNode: suspend () -> Node = db::getRootNode
) {
    private val rootNode = MutableSharedFlow<Pair<Node, TreeBrowserState.Direction>>()

    //    private var previousPage: TreeBrowserPage? = null

    val flow =
        rootNode.map { (rootNode) ->
            val childNodes =
                db.nodeDao().getChildNodes(rootNode.nodeId).filter {
                    val state = NodePayloadState.fromNode(db, it)
                    nodeVisiblePredicate(state)
                }

            val rootStateHolder = NodePayloadStateHolder(db, rootNode)
            val childrenStateHolder =
                NodePayloadListStateHolder(db, childNodes, nodeVisiblePredicate)

            TreeBrowserState(rootStateHolder, childrenStateHolder)
        }

    //    val flow =
    //        currentPage.map { TreeBrowserState(previousPage, it) }.onEach { previousPage =
    // it.current }

    init {
        scope.launch { rootNode.emit(Pair(initialRootNode(), TreeBrowserState.Direction.None)) }
    }

    private fun setRootNode(nodePayload: NodePayloadState, direction: TreeBrowserState.Direction) =
        scope.launch { rootNode.emit(Pair(nodePayload.node, direction)) }

    fun traverseUpward() =
        scope.launch {
            rootNode.last().first.parentId?.let { parentId ->
                val parent = NodePayloadState.fromNodeId(db, parentId)
                setRootNode(parent, TreeBrowserState.Direction.Upward)
            }
        }

    fun traverseInward(nodePayload: NodePayloadState) =
        setRootNode(nodePayload, TreeBrowserState.Direction.Inward)

    fun selectNode(nodePayload: NodePayloadState) {
        if (traverseDirectories && nodePayload.node.kind == NodeKind.Directory)
            traverseInward(nodePayload)
        else onNodeSelected(nodePayload)
    }
}
