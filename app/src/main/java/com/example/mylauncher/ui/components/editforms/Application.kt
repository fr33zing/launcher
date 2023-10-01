package com.example.mylauncher.ui.components.editforms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.mylauncher.data.persistent.Application
import com.example.mylauncher.data.persistent.Node
import com.example.mylauncher.data.persistent.Payload
import com.example.mylauncher.ui.components.EditFormColumn
import com.example.mylauncher.ui.components.NodePropertyTextField
import com.example.mylauncher.ui.components.dialog.FuzzyPickerDialog
import com.example.mylauncher.ui.components.enableNormalImePadding
import com.example.mylauncher.ui.theme.Background
import com.example.mylauncher.ui.theme.Foreground

@Composable
fun ApplicationEditForm(
    innerPadding: PaddingValues,
    payload: Payload?,
    node: Node,
) {
    val application = payload as Application

    EditFormColumn(innerPadding) {
        Column(
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            PickAppButton()
        }

        NodePropertyTextField(node::label, defaultValue = application.appName, userCanRevert = true)
        NodePropertyTextField(application::appName)
        NodePropertyTextField(application::packageName)
        NodePropertyTextField(application::activityClassName, userCanRevert = true)
        NodePropertyTextField(application::userHandle)
    }
}

@Composable
private fun PickAppButton() {
    val appPickerVisible = remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val (height, width) = remember { configuration.run { screenHeightDp.dp to screenWidthDp.dp } }
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
                contentColor = Background,
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

    FuzzyPickerDialog(
        visible = appPickerVisible,
        items = listOf("abcd", "cdefghi", "ghijklm"),
        itemText = { it },
        onItemPicked = { enableNormalImePadding.value = true },
        onDismissRequest = { enableNormalImePadding.value = true }
    )
}
