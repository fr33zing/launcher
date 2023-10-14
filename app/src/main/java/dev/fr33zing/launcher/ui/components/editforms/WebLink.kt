package dev.fr33zing.launcher.ui.components.editforms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.persistent.payloads.UrlRegex
import dev.fr33zing.launcher.data.persistent.payloads.WebLink
import dev.fr33zing.launcher.ui.components.EditFormColumn
import dev.fr33zing.launcher.ui.components.NodePropertyTextField
import dev.fr33zing.launcher.ui.theme.Catppuccin

@Composable
fun WebLinkEditForm(
    innerPadding: PaddingValues,
    payload: Payload?,
    node: Node,
) {
    val webLink = payload as WebLink
    val urlState = remember { mutableStateOf(webLink.url) }

    EditFormColumn(innerPadding) {
        NodePropertyTextField(node::label)
        NodePropertyTextField(webLink::url, state = urlState)

        val isValidUrl = remember(urlState.value) { UrlRegex.matches(urlState.value) }
        val isHttpsUrl by
            remember(urlState.value) {
                derivedStateOf { isValidUrl && urlState.value.startsWith("https") }
            }

        Text(
            buildAnnotatedString {
                append("Valid URL: ")
                withStyle(
                    SpanStyle(
                        color = if (isValidUrl) Catppuccin.Current.green else Catppuccin.Current.red
                    )
                ) {
                    append(if (isValidUrl) "Yes" else "No")
                }
            }
        )

        Text(
            buildAnnotatedString {
                append("Uses HTTPS: ")
                withStyle(
                    SpanStyle(
                        color = if (isHttpsUrl) Catppuccin.Current.green else Catppuccin.Current.red
                    )
                ) {
                    append(if (isHttpsUrl) "Yes" else "No")
                }
            }
        )
    }
}
