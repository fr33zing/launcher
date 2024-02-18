package dev.fr33zing.launcher.ui.components.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardAlt
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

private val controlsVerticalPadding = 16.dp
private val controlsSpacing = 24.dp

private val requestFocusButtonSize = 64.dp
private val requestFocusButtonMargin = 16.dp
private const val requestFocusButtonIconRatio = 0.375f
private const val requestFocusButtonBackgroundAlpha = 0.625f
private const val requestFocusButtonFadeInDurationMs = 500
private const val requestFocusButtonFadeInDelayMs = 500

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchContainer(
    requestFocus: () -> Unit,
    showRequestFocusButton: Boolean,
    controls: @Composable() (ColumnScope.() -> Unit),
    results: @Composable() (ColumnScope.() -> Unit)
) {
    Scaffold(
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = { RequestFocusButton(showRequestFocusButton, requestFocus) }
    ) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).imePadding()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(controlsSpacing),
                modifier =
                    Modifier.padding(
                        horizontal = ScreenHorizontalPadding,
                        vertical = controlsVerticalPadding
                    )
            ) {
                controls()
            }

            results()
        }
    }
}

@Composable
private fun RequestFocusButton(visible: Boolean, requestFocus: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication()
    val alpha by
        animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            label = "focus search bar button alpha",
            animationSpec =
                if (!visible) snap()
                else tween(requestFocusButtonFadeInDurationMs, requestFocusButtonFadeInDelayMs),
        )

    Box(Modifier.padding(bottom = requestFocusButtonMargin).alpha(alpha)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.size(requestFocusButtonSize)
                    .background(
                        Background.copy(alpha = requestFocusButtonBackgroundAlpha),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable(interactionSource, indication, onClick = requestFocus)
        ) {
            Icon(
                Icons.Outlined.KeyboardAlt,
                contentDescription = "show keyboard",
                modifier = Modifier.size(requestFocusButtonSize * requestFocusButtonIconRatio)
            )
        }
    }
}
