package dev.fr33zing.launcher.ui.components.tree.modal.utility

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import dev.fr33zing.launcher.data.viewmodel.state.TreeState

private fun <S> transitionSpec(
    mode: (S) -> TreeState.Mode
): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    val inDuration = 220
    val outDuration = 90
    val inDelay = 90
    val outDelay = if (mode(targetState) == TreeState.Mode.Normal) 1000 else outDuration

    fadeIn(animationSpec = tween(inDuration, delayMillis = inDelay))
        .togetherWith(fadeOut(animationSpec = tween(outDuration, delayMillis = outDelay)))
}

@Composable
fun <S> ModalAnimatedContent(
    state: S,
    mode: (S) -> TreeState.Mode,
    label: String,
    content: @Composable() (AnimatedContentScope.(targetState: S) -> Unit)
) =
    AnimatedContent(
        targetState = state,
        contentKey = mode,
        label = label,
        contentAlignment = Alignment.Center,
        transitionSpec = transitionSpec(mode),
        content = content
    )
