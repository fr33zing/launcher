package dev.fr33zing.launcher.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.viewmodel.SetupViewModel
import dev.fr33zing.launcher.ui.theme.background
import dev.fr33zing.launcher.ui.theme.foreground
import dev.fr33zing.launcher.ui.utility.mix

@Composable
fun Setup(
    navigateToHome: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.flow.collectAsStateWithLifecycle(null)

    LaunchedEffect(Unit) {
        viewModel.addNewUserInstructionNodes()
        viewModel.autoCategorizeApplications(context)
        navigateToHome()
    }

    state?.let { (remainingAppsToCategorize, progress) ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                buildString {
                    append("Automatically categorizing applications.")
                    append("\n\n")
                    append(
                        "This is done slowly to reduce the load on services used to determine application categories.",
                    )
                    append("\n\n")
                    append("Remaining: $remainingAppsToCategorize")
                },
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(0.5f),
                progress = progress,
                color = foreground,
                trackColor = background.mix(foreground, 0.1f),
            )
        }
    }
}
