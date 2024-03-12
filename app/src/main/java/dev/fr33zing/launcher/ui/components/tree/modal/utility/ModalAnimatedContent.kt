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

private const val IN_DURATION = 220
private const val OUT_DURATION = 100
private const val IN_DELAY = 0
private const val OUT_DELAY_SHORT = 0
private const val OUT_DELAY_LONG = 1000

const val MODAL_CLEAR_STATE_DELAY = OUT_DELAY_LONG.toLong()

fun <S> modalFiniteAnimationSpec(
    mode: TreeState.Mode,
    modeChangeDelay: Boolean = false,
): FiniteAnimationSpec<S> = tween(durationMillis = IN_DURATION, delayMillis = if (modeChangeDelay) outDelay(mode) else 0)

private fun <S> modalAnimatedContentTransitionSpec(
    mode: (S) -> TreeState.Mode,
    modalChangeDelay: Boolean = true,
): AnimatedContentTransitionScope<S>.() -> ContentTransform =
    {
        val outDelay = if (modalChangeDelay) outDelay(mode(targetState)) else OUT_DELAY_SHORT
        fadeIn(animationSpec = tween(IN_DURATION, delayMillis = IN_DELAY)).togetherWith(
            fadeOut(
                animationSpec = tween(OUT_DURATION, delayMillis = outDelay),
            ),
        )
    }

private fun outDelay(mode: TreeState.Mode) = if (mode == TreeState.Mode.Normal) OUT_DELAY_LONG else OUT_DELAY_SHORT

@Composable
fun <S> ModalAnimatedContent(
    state: S,
    mode: (S) -> TreeState.Mode,
    label: String,
    content: @Composable (AnimatedContentScope.(targetState: S) -> Unit),
) = AnimatedContent(
    targetState = state,
    contentKey = { mode(it) },
    label = label,
    contentAlignment = Alignment.Center,
    transitionSpec = modalAnimatedContentTransitionSpec(mode),
    content = content,
)
