package com.example.mylauncher.ui.components.editforms

import android.content.pm.LauncherActivityInfo
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.mylauncher.data.persistent.Application
import com.example.mylauncher.data.persistent.Node
import com.example.mylauncher.data.persistent.Payload
import com.example.mylauncher.helper.getActivityInfos
import com.example.mylauncher.ui.components.EditFormColumn
import com.example.mylauncher.ui.components.NodePropertyTextField
import com.example.mylauncher.ui.components.dialog.FuzzyPickerDialog
import com.example.mylauncher.ui.theme.Background
import com.example.mylauncher.ui.theme.Foreground
import com.example.mylauncher.ui.util.mix

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
    val buttonColor by
        animateColorAsState(
            if (!appPickerVisible.value) Foreground else Foreground.mix(Background, 0.75f),
            label = "app picker button container color"
        )

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.70f else 1f, label = "app picker button scale")

    Button(
        onClick = { appPickerVisible.value = true },
        shape = CircleShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = Background,
            ),
        modifier =
            Modifier.requiredSize(buttonSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(pressed) {
                    awaitPointerEventScope {
                        pressed =
                            if (pressed) {
                                waitForUpOrCancellation()
                                false
                            } else {
                                awaitFirstDown(false)
                                true
                            }
                    }
                }
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

    val context = LocalContext.current
    val activityInfos = remember { mutableListOf<LauncherActivityInfo>() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { getActivityInfos(context).forEach { activityInfos.add(it) } }

    FuzzyPickerDialog(
        visible = appPickerVisible,
        items = activityInfos,
        itemToString = { it.label.toString() },
        itemToAnnotatedString = {
            val split = it.componentName.packageName.split(".")
            val path = split.take(split.size - 1).joinToString(".")
            val name = split.last()

            buildAnnotatedString {
                withStyle(SpanStyle(color = Foreground.mix(Background, 0.4f))) { append("$path.") }
                withStyle(SpanStyle(color = Foreground)) { append(name) }
            }
        },
        showAnnotatedString = { _, distinct -> !distinct },
        onItemPicked = { focusManager.clearFocus(true) },
    )
}
