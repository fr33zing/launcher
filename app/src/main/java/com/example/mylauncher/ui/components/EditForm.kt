package com.example.mylauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.data.persistent.Application
import com.example.mylauncher.data.persistent.Node
import com.example.mylauncher.data.persistent.Payload
import com.example.mylauncher.helper.conditional
import com.example.mylauncher.ui.components.dialog.FuzzyPickerDialog
import com.example.mylauncher.ui.theme.Background
import com.example.mylauncher.ui.theme.Foreground

private val extraPadding = 16.dp
private val spacing = 16.dp

lateinit var enableNormalImePadding: MutableState<Boolean>

@Composable
fun EditForm(innerPadding: PaddingValues, node: Node, payload: Payload?) {
    enableNormalImePadding = remember { mutableStateOf(true) }

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
    content: @Composable (ColumnScope.() -> Unit)
) {
    val scrollState = rememberScrollState()
    Box(Modifier.conditional(enableNormalImePadding.value) { imePadding() }.fillMaxHeight()) {
        Box(Modifier.fillMaxSize().verticalScroll(scrollState).height(IntrinsicSize.Max)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier.padding(innerPadding).padding(extraPadding).fillMaxHeight(),
                content = content
            )
        }
    }
}

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
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val application = payload as Application
    val appPickerVisible = remember { mutableStateOf(false) }

    FuzzyPickerDialog(
        visible = appPickerVisible,
        items = listOf("abcd", "cdefghi", "ghijklm"),
        itemText = { it },
        onItemPicked = { enableNormalImePadding.value = true },
        onDismissRequest = { enableNormalImePadding.value = true }
    )

    EditFormColumn(innerPadding) {
        Column(
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            val (height, width) =
                remember { configuration.run { screenHeightDp.dp to screenWidthDp.dp } }
            val screenMin = remember { if (width < height) width else height }
            val buttonSize = remember { screenMin * 0.4f }
            val buttonIconSize = remember { screenMin * 0.215f }
            val buttonIconTextSpacing = remember { screenMin * -0.02f }
            val buttonFontSizeDp = remember { screenMin * 0.045f }
            val buttonFontSize = remember { with(density) { buttonFontSizeDp.toSp() } }

            Button(
                onClick = {
                    appPickerVisible.value = true
                    enableNormalImePadding.value = false
                },
                shape = CircleShape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Foreground,
                        contentColor = Background
                    ),
                modifier = Modifier.requiredSize(buttonSize)
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
