package dev.fr33zing.launcher.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.viewmodel.ViewNoteViewModel
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.mix
import java.text.SimpleDateFormat
import java.util.Date

private val bodyPadding = 16.dp
private val bodySpacing = 24.dp
private val dim = Foreground.mix(Background, 0.5f)

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
        Column(
            verticalArrangement = Arrangement.spacedBy(bodySpacing),
            modifier =
                Modifier.padding(padding)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = bodyPadding)
        ) {
            if (state.body.isBlank()) Text("This note has no body.", color = dim)
            else Text(state.body)

            CreatedUpdatedText(state.created, state.updated)
        }
    }
}

@Composable
private fun CreatedUpdatedText(created: Date, updated: Date) {
    val context = LocalContext.current
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]
    val preferences = dev.fr33zing.launcher.data.persistent.Preferences(context)
    val use24HourTime by preferences.home.use24HourTime.state
    val timeFormat12Hour = remember(locale) { SimpleDateFormat("hh:mm a", locale) }
    val timeFormat24Hour = remember(locale) { SimpleDateFormat("HH:mm", locale) }
    val timeFormat =
        remember(use24HourTime) { if (use24HourTime) timeFormat24Hour else timeFormat12Hour }
    val dateFormat = remember(locale, use24HourTime) { SimpleDateFormat("LLL d, yyyy", locale) }
    val dateText =
        remember(dateFormat, updated) {
            buildString {
                append("Created on ")
                append(dateFormat.format(created))
                append(" at ")
                appendLine(timeFormat.format(created).trimStart('0'))

                append("Updated on ")
                append(dateFormat.format(updated))
                append(" at ")
                append(timeFormat.format(updated).trimStart('0'))
            }
        }
    Text(dateText, color = dim)
}
