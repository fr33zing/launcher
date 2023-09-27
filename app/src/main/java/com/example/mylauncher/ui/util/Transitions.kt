package com.example.mylauncher.ui.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color

@Composable
fun Slide(content: @Composable AnimatedVisibilityScope.() -> Unit) {
    val visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = visible, enter = slideInHorizontally(), exit = slideOutHorizontally()
    ) {
        content()
    }
}

@Composable
fun Fade(content: @Composable AnimatedVisibilityScope.() -> Unit) {
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = visible, enter = fadeIn(), exit = fadeOut()
    ) {
        content()
    }
}

@Composable
fun FadeOut(visible: Boolean, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = visible, enter = EnterTransition.None, exit = fadeOut()
    ) {
        content()
    }
}

@Composable
fun FadeInFromBlack(content: @Composable () -> Unit) {
    val alpha = remember { Animatable(1.0f) }

    Box(
        Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                drawRect(color = Color.Black, size = size, alpha = alpha.value)
            }) {

        LaunchedEffect(alpha) {
            alpha.animateTo(0.0f, tween(1000))
        }
        content()
    }
}

