package dev.fr33zing.launcher.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.viewmodel.TreeViewModel
import dev.fr33zing.launcher.ui.components.node.next.NodeTree
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun Tree(
    navigateBack: () -> Unit,
    viewModel: TreeViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    //    val snapshot = remember { mutableStateListOf<AnimatedTreeNodeState>() }

    //    LaunchedEffect(Unit) {
    //        viewModel.flow.filterNotNull().collectLatest { nextTreeNodeStateList ->
    //            nextTreeNodeStateList.forEach { nextTreeNodeState ->
    //                if (
    //                    snapshot.none { (treeNodeState) ->
    //                        nextTreeNodeState.nodePayload.node.nodeId ==
    //                            treeNodeState.nodePayload.node.nodeId
    //                    }
    //                ) {
    //                    val new = AnimatedTreeNodeState(nextTreeNodeState, Animatable(0f))
    //                    snapshot.add(new)
    //                    coroutineScope.launch { new.progress.animateTo(1f, tween(1000)) }
    //                }
    //            }
    //        }
    //    }

    val state by viewModel.flow.collectAsStateWithLifecycle()

    NodeTree(state ?: emptyList())
}
