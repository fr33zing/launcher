package dev.fr33zing.launcher.ui.components.tree.modal.utility

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import dev.fr33zing.launcher.data.viewmodel.state.TreeState

private const val inDuration = 220
private const val outDuration = 90
private const val inDelay = 90
private const val outDelayShort = 220
private const val outDelayLong = 1000

const val ModalClearStateDelay = outDelayLong.toLong()

fun <S> modalFiniteAnimationSpec(
    mode: TreeState.Mode,
    modeChangeDelay: Boolean = false
): FiniteAnimationSpec<S> =
    tween(durationMillis = inDuration, delayMillis = if (modeChangeDelay) outDelay(mode) else 0)

private fun <S> modalAnimatedContentTransitionSpec(
    mode: (S) -> TreeState.Mode,
    modalChangeDelay: Boolean = true
): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    val outDelay = if (modalChangeDelay) outDelay(mode(targetState)) else outDelayShort
    fadeIn(animationSpec = tween(inDuration, delayMillis = inDelay))
        .togetherWith(fadeOut(animationSpec = tween(outDuration, delayMillis = outDelay)))
}

private fun outDelay(mode: TreeState.Mode) =
    if (mode == TreeState.Mode.Normal) outDelayLong else outDelayShort

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
        transitionSpec = modalAnimatedContentTransitionSpec(mode),
        content = content
    )
