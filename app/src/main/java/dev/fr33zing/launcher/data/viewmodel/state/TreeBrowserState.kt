package dev.fr33zing.launcher.data.viewmodel.state

import androidx.compose.runtime.mutableStateOf
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.getRootNode
import dev.fr33zing.launcher.data.persistent.nodeLineage
import dev.fr33zing.launcher.data.persistent.payloads.Reference
import dev.fr33zing.launcher.data.utility.NullPayloadException
import dev.fr33zing.launcher.data.utility.PayloadClassMismatchException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TreeBrowserState(
    val stack: ArrayDeque<Node>,
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
    private val nodeVisiblePredicate: ((ReferenceFollowingNodePayloadState) -> Boolean)? = null,

    /**
     * Function to call when a node is tapped. Not called when tapping directories if
     * `traverseDirectories` is true.
     */
    onNodeSelected: TreeBrowserStateHolder.(ReferenceFollowingNodePayloadState) -> Unit = {},

    /**
     * Function to call when a node is tapped. Not called when tapping directories if
     * `traverseDirectories` is true.
     */
    private val onTraverse: TreeBrowserStateHolder.(Node) -> Unit = {},

    /**
     * Function used to determine the initial directory. Defaults to the global root node if no
     * value is provided.
     */
    private val initialRootNode: suspend () -> Node = db::getRootNode
) {
    private data class Update(
        val stack: ArrayDeque<Node>,
        val direction: TreeBrowserState.Direction,
    )

    private val onNodeSelectedFn = mutableStateOf(onNodeSelected)
    private val updateFlow = MutableSharedFlow<Update>()
    private var initialRootNodeId: Int? = null

    private val currentRootNode
        get() = flow.value?.stack?.last()

    val currentRootNodeId
        get() = currentRootNode?.nodeId

    val flow =
        updateFlow
            .map { (stack, direction) ->
                val rootNode = stack.last()
                val loadChildrenFromId =
                    if (rootNode.kind != NodeKind.Reference) rootNode.nodeId
                    else {
                        ((db.getPayloadByNodeId(NodeKind.Reference, rootNode.nodeId)
                                ?: NullPayloadException(rootNode))
                                as? Reference ?: throw PayloadClassMismatchException(rootNode))
                            .targetId ?: throw Exception("targetId is null")
                    }
                val childNodes =
                    db.nodeDao().getChildNodes(loadChildrenFromId).let { childNodes ->
                        nodeVisiblePredicate?.let { predicate ->
                            childNodes.filter { childNode ->
                                predicate(
                                    ReferenceFollowingNodePayloadState.fromNode(db, childNode)
                                )
                            }
                        } ?: childNodes
                    }
                val rootStateHolder = NodePayloadStateHolder(db, rootNode)
                val childrenStateHolder =
                    NodePayloadListStateHolder(db, childNodes, nodeVisiblePredicate)
                val canTraverseUpward =
                    stack.count() > 1 &&
                        (!containTraversalWithinInitialRoot || rootNode.nodeId != initialRootNodeId)

                TreeBrowserState(
                    stack = stack,
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
            val stack = db.nodeLineage(initialRootNode)

            updateFlow.emit(Update(stack, TreeBrowserState.Direction.None))
        }
    }

    fun selectNode(nodePayload: ReferenceFollowingNodePayloadState) {
        if (traverseDirectories && nodePayload.node.kind == NodeKind.Directory)
            traverseInward(nodePayload)
        else onNodeSelectedFn.value(this, nodePayload)
    }

    /** Sets the callback for when a node is selected. */
    fun onNodeSelected(
        callback: TreeBrowserStateHolder.(ReferenceFollowingNodePayloadState) -> Unit
    ) {
        onNodeSelectedFn.value = callback
    }

    /** Move out of the current directory and into its parent. */
    fun traverseUpward() {
        if (!flow.value!!.canTraverseUpward) return
        scope.launch {
            updateStack(TreeBrowserState.Direction.Upward) { stack -> stack.removeLast() }
        }
    }

    fun traverseInward(destination: ReferenceFollowingNodePayloadState) =
        updateStack(TreeBrowserState.Direction.Inward) { stack -> stack.addLast(destination.node) }

    private fun updateStack(
        direction: TreeBrowserState.Direction,
        mutateStack: (ArrayDeque<Node>) -> Unit,
    ) =
        scope.launch {
            val stack = flow.value!!.stack

            mutateStack(stack)
            updateFlow.emit(Update(stack, direction))
            onTraverse(stack.last())
        }
}
