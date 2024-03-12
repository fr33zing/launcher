package dev.fr33zing.launcher.ui.components.dialog

import android.content.pm.LauncherActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import dev.fr33zing.launcher.data.utility.getActivityInfos
import dev.fr33zing.launcher.ui.theme.background
import dev.fr33zing.launcher.ui.theme.foreground
import dev.fr33zing.launcher.ui.utility.mix

@Composable
fun ApplicationPickerDialog(
    visible: MutableState<Boolean>,
    onAppPicked: (LauncherActivityInfo) -> Unit,
    overrideActivityInfos: List<LauncherActivityInfo>? = null,
) {
    val context = LocalContext.current
    var activityInfos by
        remember(overrideActivityInfos) { mutableStateOf<List<LauncherActivityInfo>>(emptyList()) }

    LaunchedEffect(overrideActivityInfos) {
        activityInfos = overrideActivityInfos ?: getActivityInfos(context)
    }

    FuzzyPickerDialog(
        visible = visible,
        items = activityInfos,
        itemToString = { app -> app.label.toString() },
        itemToAnnotatedString = { app, fontSize, color ->
            buildAnnotatedString {
                val labelStyle = SpanStyle(color = color, fontSize = fontSize)
                val packageNameStyle =
                    SpanStyle(
                        color = foreground.mix(background, 0.4f),
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
