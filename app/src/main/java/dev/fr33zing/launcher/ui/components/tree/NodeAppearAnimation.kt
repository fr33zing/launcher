package dev.fr33zing.launcher.ui.components.tree

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.utility.conditional

@Composable
fun NodeAppearAnimation(
    progress: Animatable<Float, AnimationVector1D>,
    content: @Composable () -> Unit,
) {
    val dimensions = LocalNodeDimensions.current

    Box(
        Modifier.fillMaxWidth().conditional(progress.value < 1f) {
            graphicsLayer {
                translationY = (1 - progress.value) * (dimensions.lineHeight.toPx() * 0.75f)
                alpha = progress.value
            }
        },
        content = { content() },
    )
}
