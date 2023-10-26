package dev.fr33zing.launcher.ui.components.editforms

import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.persistent.payloads.Setting
import dev.fr33zing.launcher.ui.components.EditFormColumn
import dev.fr33zing.launcher.ui.components.NodePropertyTextField
import dev.fr33zing.launcher.ui.components.dialog.FuzzyPickerDialog
import java.util.Locale
import kotlin.reflect.full.staticProperties

private const val SETTINGS_PREFIX = "android.settings."
private const val SETTINGS_SUFFIX = "_SETTINGS"

private val substitutions =
    mapOf(
            "apn" to "APN",
            "ui" to "UI",
            "nfc" to "NFC",
            "uri" to "URI",
            "vpn" to "VPN",
            "vr" to "VR",
            "wifi" to "WiFi",
            "ip" to "IP",
            "sim" to "SIM"
        )
        .map { (from, to) -> Pair(from.toRegex(RegexOption.IGNORE_CASE), to) }
        .toMap()

// TODO improve performance OR make a static map
private fun formatSettingName(setting: String): String =
    setting
        .split('.')
        .last()
        .replace(SETTINGS_SUFFIX, "")
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        .let {
            var settingName = it
            substitutions.forEach { (from, to) -> settingName = settingName.replace(from, to) }
            settingName
        }

@Composable
fun SettingEditForm(
    innerPadding: PaddingValues,
    payload: Payload?,
    node: Node,
) {
    val allSettings = remember {
        Settings::class
            .staticProperties
            .toList()
            .mapNotNull {
                try {
                    (it.get() as? String)
                } catch (_: Exception) {
                    null
                }
            }
            .filter { it.startsWith(SETTINGS_PREFIX) }
            .sortedBy { formatSettingName(it) }
    }

    val setting = payload as Setting
    val settingState = remember { mutableStateOf("") }
    val settingPickerVisible = remember { mutableStateOf(false) }

    EditFormColumn(innerPadding) {
        NodePropertyTextField(node::label)
        NodePropertyTextField(setting::setting, state = settingState)

        Button(onClick = { settingPickerVisible.value = true }) { Text("Pick setting") }
    }

    FuzzyPickerDialog(
        visible = settingPickerVisible,
        items = allSettings,
        itemToString = ::formatSettingName,
        itemToAnnotatedString = { settingName, fontSize, color ->
            buildAnnotatedString {
                withStyle(SpanStyle(color = color, fontSize = fontSize)) {
                    append(formatSettingName(settingName))
                }
            }
        },
        showAnnotatedString = { _, distinct -> !distinct },
        onItemPicked = { settingState.value = it },
    )
}
