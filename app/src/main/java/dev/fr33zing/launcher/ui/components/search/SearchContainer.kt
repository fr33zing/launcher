package dev.fr33zing.launcher.ui.components.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding

private val controlsVerticalPadding = 16.dp
private val controlsSpacing = 24.dp

@Composable
fun SearchContainer(
    controls: @Composable ColumnScope.() -> Unit,
    results: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.systemBarsPadding().imePadding()) {
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
