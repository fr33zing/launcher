package dev.fr33zing.launcher.ui.components.tree.utility

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeKey
import dev.fr33zing.launcher.ui.components.tree.HIGHLIGHT_ANIMATION_DURATION_MS
import dev.fr33zing.launcher.ui.components.tree.HIGHLIGHT_ANIMATION_MAX_ALPHA
import dev.fr33zing.launcher.ui.components.tree.UNHIGHLIGHT_ANIMATION_DURATION_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Immutable
class NodeTreeHighlightAnimation(
    onDisableFlowStagger: () -> Unit,
    onClearHighlightedNode: () -> Unit,
    coroutineScope: CoroutineScope,
    highlightKeyFlow: StateFlow<TreeNodeKey?>,
) {
    val alpha = Animatable(0f)
    val flow =
        highlightKeyFlow
            .onEach {
                if (it == null) return@onEach
                onDisableFlowStagger()
                coroutineScope.launch {
                    alpha.snapTo(0f)
                    alpha.animateTo(
                        HIGHLIGHT_ANIMATION_MAX_ALPHA,
                        tween(HIGHLIGHT_ANIMATION_DURATION_MS),
                    )
                    alpha.animateTo(0f, tween(UNHIGHLIGHT_ANIMATION_DURATION_MS))
                    onClearHighlightedNode()
                }
            }
}
