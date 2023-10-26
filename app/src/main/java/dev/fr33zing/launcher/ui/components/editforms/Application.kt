package dev.fr33zing.launcher.ui.components.editforms

import android.content.pm.LauncherActivityInfo
import android.os.Process
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Application
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.helper.getActivityInfos
import dev.fr33zing.launcher.ui.components.EditFormColumn
import dev.fr33zing.launcher.ui.components.NodePropertyTextField
import dev.fr33zing.launcher.ui.components.dialog.FuzzyPickerDialog
import dev.fr33zing.launcher.ui.components.refreshNodePropertyTextFields
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.util.mix

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
            PickAppButton {
                with(application) {
                    appName = it.label.toString()
                    packageName = it.applicationInfo.packageName
                    activityClassName = it.componentName.className
                    userHandle = it.user.toString()
                }
                node.label = application.appName
                refreshNodePropertyTextFields()
            }
        }

        NodePropertyTextField(node::label, defaultValue = application.appName, userCanRevert = true)
        NodePropertyTextField(application::appName)
        NodePropertyTextField(application::packageName)
        NodePropertyTextField(application::activityClassName, userCanRevert = true)
        NodePropertyTextField(
            application::userHandle,
            defaultValue = Process.myUserHandle().toString()
        )
    }
}

// TODO make this reusable and use it for Setting payload edit form
@Composable
private fun PickAppButton(onAppPicked: (LauncherActivityInfo) -> Unit) {
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
    LaunchedEffect(Unit) { getActivityInfos(context).forEach { activityInfos.add(it) } }

    FuzzyPickerDialog(
        visible = appPickerVisible,
        items = activityInfos,
        itemToString = { app -> app.label.toString() },
        itemToAnnotatedString = { app, fontSize, color ->
            buildAnnotatedString {
                val labelStyle = SpanStyle(color = color, fontSize = fontSize)
                val packageNameStyle =
                    SpanStyle(
                        color = Foreground.mix(Background, 0.4f),
                        fontSize = fontSize * 0.65f,
                    )

                withStyle(labelStyle) { append("${app.label} ") }
                withStyle(packageNameStyle) { append("(${app.applicationInfo.packageName})") }
            }
        },
        showAnnotatedString = { _, distinct -> !distinct },
        onItemPicked = onAppPicked,
    )
}
