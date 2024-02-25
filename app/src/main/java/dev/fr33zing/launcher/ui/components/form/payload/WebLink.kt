package dev.fr33zing.launcher.ui.components.form.payload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.persistent.payloads.UrlRegex
import dev.fr33zing.launcher.data.persistent.payloads.Website
import dev.fr33zing.launcher.ui.components.form.EditFormColumn
import dev.fr33zing.launcher.ui.components.form.NodePropertyTextField
import dev.fr33zing.launcher.ui.pages.EditFormArguments
import dev.fr33zing.launcher.ui.theme.Catppuccin
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun WebsiteEditForm(arguments: EditFormArguments) {
    val (padding, node, payload) = arguments
    val website = payload as Website

    val labelState = remember { mutableStateOf(node.label) }
    val urlState = remember { mutableStateOf(website.url) }
    var pendingHttpResponse by remember { mutableStateOf(false) }

    EditFormColumn(padding) {
        NodePropertyTextField(node::label, state = labelState)
        NodePropertyTextField(website::url, state = urlState)

        val isValidUrl = remember(urlState.value) { UrlRegex.matches(urlState.value) }
        val isHttpsUrl by
            remember(urlState.value) {
                derivedStateOf { isValidUrl && urlState.value.startsWith("https") }
            }

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Text(
                buildAnnotatedString {
                    append("Valid URL: ")
                    withStyle(
                        SpanStyle(
                            color =
                                if (isValidUrl) Catppuccin.Current.green else Catppuccin.Current.red
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
                            color =
                                if (isHttpsUrl) Catppuccin.Current.green else Catppuccin.Current.red
                        )
                    ) {
                        append(if (isHttpsUrl) "Yes" else "No")
                    }
                }
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(
                enabled = !pendingHttpResponse,
                onClick = {
                    pendingHttpResponse = true
                    getWebpageTitle(urlState.value) { title ->
                        pendingHttpResponse = false
                        node.label = title
                        labelState.value = title
                    }
                }
            ) {
                Text("Derive label from webpage title")
            }
        }
    }
}

private fun getWebpageTitle(url: String, setLabel: (String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        val html = URL(url).readText()
        val regex = "<title>(.*?)</title>".toRegex()
        val result = regex.find(html)
        val title = result?.groupValues?.get(1) ?: return@launch
        setLabel(title)
    }
}
