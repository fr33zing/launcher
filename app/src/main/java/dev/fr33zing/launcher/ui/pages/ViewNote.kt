package dev.fr33zing.launcher.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.viewmodel.ViewNoteViewModel
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.mix

private val verticalPadding = 12.dp
private val noBodyColor = Foreground.mix(Background, 0.5f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewNote(
    viewModel: ViewNoteViewModel = hiltViewModel(),
) {
    val state by viewModel.flow.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                actions = {},
            )
        }
    ) { padding ->
        Box(
            Modifier.padding(padding)
                .padding(horizontal = ScreenHorizontalPadding, vertical = verticalPadding)
        ) {
            if (state.body.isBlank()) Text("This note has no body.", color = noBodyColor)
            else Text(state.body)
        }
    }
}
