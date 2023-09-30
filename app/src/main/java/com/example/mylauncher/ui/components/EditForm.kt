package com.example.mylauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.data.persistent.Application
import com.example.mylauncher.data.persistent.Node
import com.example.mylauncher.data.persistent.Payload
import com.example.mylauncher.ui.theme.Background
import com.example.mylauncher.ui.theme.Foreground

private val extraPadding = 16.dp
private val spacing = 16.dp

@Composable
fun EditForm(innerPadding: PaddingValues, node: Node, payload: Payload?) {
    if (payload != null)
        when (node.kind) {
            NodeKind.Application -> ApplicationEditForm(innerPadding, payload, node)
            else -> DefaultEditForm(innerPadding, node)
        }
    else DefaultEditForm(innerPadding, node)
}

@Composable
private fun EditFormColumn(
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit)
) =
    Column(
        modifier =
            Modifier.padding(innerPadding).padding(extraPadding).fillMaxWidth().then(modifier),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )

@Composable
private fun DefaultEditForm(innerPadding: PaddingValues, node: Node) {
    EditFormColumn(innerPadding) { NodePropertyTextField(node::label) }
}

@Composable
private fun ApplicationEditForm(
    innerPadding: PaddingValues,
    payload: Payload?,
    node: Node,
) {
    val application = payload as Application

    val (height, width) = LocalConfiguration.current.run { screenHeightDp.dp to screenWidthDp.dp }
    val screenMin = if (width < height) width else height
    val buttonOuterPadding = screenMin * 0.15f
    val buttonIconSize = screenMin * 0.215f
    val buttonIconTextSpacing = screenMin * -0.02f
    val buttonFontSizeDp = screenMin * 0.045f
    val buttonFontSize = with(LocalDensity.current) { buttonFontSizeDp.toSp() }

    EditFormColumn(innerPadding, modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier.fillMaxWidth()
                    .weight(1f)
                    .padding(buttonOuterPadding)
                    .absolutePadding(bottom = extraPadding)
        ) {
            Button(
                onClick = { /*TODO*/},
                shape = CircleShape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Foreground,
                        contentColor = Background
                    ),
                modifier = Modifier.aspectRatio(1f)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(buttonIconTextSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "search",
                        modifier = Modifier.size(buttonIconSize)
                    )
                    Text("Pick app", fontSize = buttonFontSize)
                }
            }
        }

        NodePropertyTextField(node::label, defaultValue = application.appName, userCanRevert = true)
        NodePropertyTextField(application::appName)
        NodePropertyTextField(application::packageName)
        NodePropertyTextField(application::activityClassName, userCanRevert = true)
        NodePropertyTextField(application::userHandle)
    }
}
