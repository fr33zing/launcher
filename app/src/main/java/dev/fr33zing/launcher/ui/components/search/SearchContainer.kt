package dev.fr33zing.launcher.ui.components.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.ui.components.ActionButton
import dev.fr33zing.launcher.ui.components.ActionButtonVerticalPadding
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding

private val controlsVerticalPadding = 16.dp
private val controlsSpacing = 24.dp

private val requestFocusButtonMargin = 16.dp
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
    val alpha by
        animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            label = "focus search bar button alpha",
            animationSpec =
                if (!visible) snap()
                else tween(requestFocusButtonFadeInDurationMs, requestFocusButtonFadeInDelayMs),
        )

    Box(Modifier.padding(bottom = ActionButtonVerticalPadding).alpha(alpha)) {
        ActionButton(
            icon = Icons.Outlined.Keyboard,
            contentDescription = "show keyboard",
            onClick = requestFocus,
        )
    }
}
