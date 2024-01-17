package dev.fr33zing.launcher.ui.components.editform

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import dev.fr33zing.launcher.data.persistent.payloads.Setting
import dev.fr33zing.launcher.data.persistent.payloads.mainPackageManager
import dev.fr33zing.launcher.ui.components.dialog.FuzzyPickerDialog
import dev.fr33zing.launcher.ui.components.node.NodePropertyTextField
import dev.fr33zing.launcher.ui.pages.EditFormArguments
import kotlin.reflect.full.staticProperties

private val problematicSettings =
    listOf(
        // Requires permission: android.permission.OPEN_ACCESSIBILITY_DETAILS_SETTINGS
        "android.settings.ACCESSIBILITY_DETAILS_SETTINGS",

        // Throws android.view.InflateException
        "android.settings.ACTION_APP_NOTIFICATION_REDACTION",

        // Appears to do nothing
        "android.settings.ADD_ACCOUNT_SETTINGS",
        "android.settings.BIOMETRIC_ENROLL",
        "android.settings.COMMUNAL_SETTINGS",
        "android.settings.ENTERPRISE_PRIVACY_SETTINGS",
        "android.settings.FOREGROUND_SERVICES_SETTINGS",
        "android.settings.HARD_KEYBOARD_SETTINGS",
        "android.settings.NFCSHARING_SETTINGS",
        "android.settings.VOICE_CONTROL_AIRPLANE_MODE",
        "android.settings.VOICE_CONTROL_BATTERY_SAVER_MODE",
        "android.settings.VOICE_CONTROL_DO_NOT_DISTURB_MODE",
        "android.settings.WIFI_ADD_NETWORKS",

        // Throws android.content.ActivityNotFoundException
        "android.settings.ALL_APPS_NOTIFICATION_SETTINGS_FOR_REVIEW",
        "android.settings.STORAGE_MANAGER_SETTINGS",

        // Opens toast: "The app wasn't found in the list of installed apps." on GrapheneOS
        "android.settings.APP_NOTIFICATION_BUBBLE_SETTINGS",
        "android.settings.APP_NOTIFICATION_SETTINGS",

        // Requires permission: android.permission.BLUETOOTH_SCAN
        "android.settings.BLUETOOTH_PAIRING_SETTINGS",

        // Throws java.lang.RuntimeException
        "android.settings.CHANNEL_NOTIFICATION_SETTINGS",
        "android.settings.NOTIFICATION_LISTENER_DETAIL_SETTINGS",
        "android.settings.SHOW_RESTRICTED_SETTING_DIALOG",

        // Requires permission: android.permission.MANAGED_PROFILE_SETTINGS
        "android.settings.MANAGED_PROFILE_SETTINGS",

        // Appears to be a duplicate of android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS
        "android.settings.APPLICATION_SETTINGS",
        "android.settings.MANAGE_APPLICATIONS_SETTINGS",

        // Appears to be a duplicate of android.settings.MANAGE_OVERLAY_PERMISSION
        "android.settings.action.MANAGE_OVERLAY_PERMISSION",

        // Appears to be a duplicate of android.settings.NOTIFICATION_SETTINGS
        "android.settings.NOTIFICATION_ASSISTANT_SETTINGS",

        // Appears to be a duplicate of android.settings.PRIVACY_SETTINGS
        "android.settings.REQUEST_ENABLE_CONTENT_CAPTURE",

        // Blocked by work policy on GrapheneOS
        "android.settings.SHOW_ADMIN_SUPPORT_DETAILS",

        // Requires permission: android.permission.DUMP
        "android.settings.SHOW_REMOTE_BUGREPORT_DIALOG",

        // Crashes for unknown reason
        "android.settings.SYSTEM_UPDATE_SETTINGS",
        "android.settings.TETHER_PROVISIONING_UI",

        // Requires permission: android.permission.TETHER_PRIVILEGED
        "android.settings.TETHER_UNSUPPORTED_CARRIER_UI",

        // Appears to be a duplicate of android.settings.ZEN_MODE_AUTOMATION_SETTINGS
        "android.settings.ACTION_CONDITION_PROVIDER_SETTINGS",

        // Opens toast: "Rule not found."
        "android.settings.ZEN_MODE_EVENT_RULE_SETTINGS",
        "android.settings.ZEN_MODE_SCHEDULE_RULE_SETTINGS",

        // Appears to be a duplicate of android.settings.ZEN_MODE_SETTINGS"
        "android.settings.ZEN_MODE_PRIORITY_SETTINGS",

        // Appears to be a duplicate of com.android.settings.TRUSTED_CREDENTIALS_USER
        "com.android.settings.MONITORING_CERT_INFO",
    )

private val aliases =
    mapOf(
        // Accessibility
        "android.settings.ACCESSIBILITY_SETTINGS" to "Accessibility",
        "android.settings.ACCESSIBILITY_COLOR_MOTION_SETTINGS" to "Color and motion",
        "com.android.settings.ACCESSIBILITY_COLOR_SPACE_SETTINGS" to "Color correction",
        "android.settings.COLOR_INVERSION_SETTINGS" to "Color inversion",
        "android.settings.TEXT_READING_SETTINGS" to "Display size & text",
        "android.settings.CAPTIONING_SETTINGS" to "Caption preferences",
        "android.settings.action.ONE_HANDED_SETTINGS" to "One-handed mode",

        // Cellular
        "android.settings.APN_SETTINGS" to "APNs",
        "android.settings.AIRPLANE_MODE_SETTINGS" to "Airplane mode",
        "android.settings.DATA_ROAMING_SETTINGS" to "Data roaming",
        "android.settings.DATA_SAVER_SETTINGS" to "Data saver",
        "android.settings.DATA_USAGE_SETTINGS" to "Data usage",
        "android.settings.MMS_MESSAGE_SETTING" to "MMS messaging",
        "android.settings.MOBILE_DATA_USAGE" to "Mobile data usage",
        "android.settings.NETWORK_OPERATOR_SETTINGS" to "Network operator",
        "android.settings.MANAGE_ALL_SIM_PROFILES_SETTINGS" to "SIMs",

        // App permissions
        "android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION" to "Permission: File access",
        "android.settings.MANAGE_DOMAIN_URLS" to "Permission: Opening links",
        "android.settings.MANAGE_OVERLAY_PERMISSION" to "Permission: Display over other apps",
        "android.settings.action.MANAGE_WRITE_SETTINGS" to "Permission: Modify system settings",
        "android.settings.MANAGE_UNKNOWN_APP_SOURCES" to "Permission: Install unknown apps",
        "android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS" to
            "Permission: Do Not Disturb access",
        "android.settings.PICTURE_IN_PICTURE_SETTINGS" to "Permission: Picture-in-picture",
        "android.settings.REQUEST_MANAGE_MEDIA" to "Permission: Media management",
        "android.settings.REQUEST_SCHEDULE_EXACT_ALARM" to "Permission: Alarms & reminders",
        "android.settings.USAGE_ACCESS_SETTINGS" to "Permission: Usage access",
        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS" to
            "Permission: Notification access",

        // Connection
        "android.settings.WIFI_IP_SETTINGS" to "Network preferences",
        "android.settings.WIFI_SETTINGS" to "Internet",
        "android.settings.WIRELESS_SETTINGS" to "Network & internet",
        "android.settings.BLUETOOTH_SETTINGS" to "Connected devices",
        "com.android.settings.WIFI_TETHER_SETTINGS" to "WiFi hotspot",
        "android.settings.TETHER_SETTINGS" to "Hotspot & tethering",

        // Location
        "android.settings.LOCATION_SCANNING_SETTINGS" to "Location scanning",
        "android.settings.LOCATION_SOURCE_SETTINGS" to "Location",

        // Input / locale / dictionary
        "android.settings.INPUT_METHOD_SETTINGS" to "On-screen keyboard",
        "android.settings.INPUT_METHOD_SUBTYPE_SETTINGS" to "On-screen keyboard subtype",
        "android.settings.LOCALE_SETTINGS" to "Languages",
        "android.settings.REGIONAL_PREFERENCES_SETTINGS" to "Regional preferences",
        "android.settings.USER_DICTIONARY_SETTINGS" to "Personal dictionary",
        "com.android.settings.USER_DICTIONARY_INSERT" to "Add to personal dictionary",

        // Notifications
        "android.settings.NOTIFICATION_SETTINGS" to "Notifications",
        "android.settings.NOTIFICATION_HISTORY" to "Notification history",

        // Do Not Disturb
        "android.settings.ZEN_MODE_SETTINGS" to "Do Not Disturb",
        "android.settings.ZEN_MODE_AUTOMATION_SETTINGS" to "Schedules",

        // Storage
        "android.settings.INTERNAL_STORAGE_SETTINGS" to "Storage",
        "android.settings.MEMORY_CARD_SETTINGS" to "Memory card",

        // NFC
        "android.settings.NFC_SETTINGS" to "NFC",
        "android.settings.NFC_PAYMENT_SETTINGS" to "Contactless payments",

        // Security / privacy
        "android.settings.PRIVACY_SETTINGS" to "Privacy",
        "android.settings.SECURITY_SETTINGS" to "Security",
        "com.android.settings.TRUSTED_CREDENTIALS_USER" to "Trusted credentials",
        "android.settings.ADVANCED_MEMORY_PROTECTION_SETTINGS" to "Advanced memory protection",
        "android.settings.VPN_SETTINGS" to "VPN",
        "android.settings.FINGERPRINT_ENROLL" to "Set up fingerprint unlock",

        // Development
        "android.settings.APPLICATION_DEVELOPMENT_SETTINGS" to "Developer options",
        "android.settings.BUGREPORT_HANDLER_SETTINGS" to "Bug report handler",

        // Niche
        "android.settings.VR_LISTENER_SETTINGS" to "VR helper services",
        "android.settings.WEBVIEW_SETTINGS" to "WebView implementation",
        "android.settings.MANAGE_CLONED_APPS_SETTINGS" to "Cloned apps",
        "android.settings.SHOW_REGULATORY_INFO" to "Regulatory labels",

        // Display
        "android.settings.DISPLAY_SETTINGS" to "Display",
        "android.settings.NIGHT_DISPLAY_SETTINGS" to "Night Light",
        "android.settings.REDUCE_BRIGHT_COLORS_SETTINGS" to "Extra dim",

        // Meta
        "android.settings.SETTINGS" to "All settings",
        "android.settings.APP_SEARCH_SETTINGS" to "Search all settings",
        "android.settings.DEVICE_INFO_SETTINGS" to "About phone",
        "android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS" to "All apps",

        // Broad sections
        "android.settings.DATE_SETTINGS" to "Date & time",
        "android.settings.SOUND_SETTINGS" to "Sound & vibration",

        // Personalization
        "android.settings.DARK_THEME_SETTINGS" to "Dark theme",
        "android.settings.ACTION_POWER_MENU_SETTINGS" to "Power menu",
        "android.settings.ASSIST_GESTURE_SETTINGS" to "Assist gesture",
        "android.settings.CONVERSATION_SETTINGS" to "Conversations",
        "android.settings.DREAM_SETTINGS" to "Screen saver",
        "android.settings.HOME_SETTINGS" to "Default home app",
        "android.settings.LOCK_SCREEN_SETTINGS" to "Lock screen",
        "android.settings.VOICE_INPUT_SETTINGS" to "Digital assistant app",

        // Shortcuts
        "android.settings.AUTO_ROTATE_SETTINGS" to "Auto-rotate screen",
        "android.settings.BATTERY_SAVER_SETTINGS" to "Battery saver",
        "android.settings.CAST_SETTINGS" to "Cast",

        // Accounts
        "android.settings.MANAGE_CROSS_PROFILE_ACCESS" to "Connected work & personal apps",
        "android.settings.SYNC_SETTINGS" to "Passwords & accounts",
        "android.settings.USER_SETTINGS" to "Multiple users",

        // Other
        "android.settings.ACTION_MEDIA_CONTROLS_SETTINGS" to "Media",
        "android.settings.ACTION_PRINT_SETTINGS" to "Printing",
        "android.settings.ALL_APPS_NOTIFICATION_SETTINGS" to "App notifications",
        "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS" to "Battery optimization",
        "android.settings.MANAGE_APP_LONG_RUNNING_JOBS" to "Long background tasks",
        "android.settings.MANAGE_DEFAULT_APPS_SETTINGS" to "Default apps",
    )

private fun formatSettingName(setting: String): String = aliases[setting] ?: setting

@Composable
fun SettingEditForm(arguments: EditFormArguments) {
    val (padding, node, payload) = arguments
    val setting = payload as Setting

    val allSettings = remember {
        Settings::class
            .staticProperties
            .asSequence()
            .filter { it.name.startsWith("ACTION_") }
            .mapNotNull {
                try {
                    (it.get() as? String)
                } catch (_: Exception) {
                    null
                }
            }
            .filter { it !in problematicSettings }
            .filter { mainPackageManager.resolveActivity(Intent(it), 0) != null }
            .sortedBy { formatSettingName(it) }
            .toList()
    }

    val labelState = remember { mutableStateOf(node.label) }
    val settingState = remember { mutableStateOf(setting.setting) }
    val settingPickerVisible = remember { mutableStateOf(false) }

    EditFormColumn(padding) {
        val defaultLabel = remember(settingState.value) { aliases[settingState.value] }
        NodePropertyTextField(
            node::label,
            state = labelState,
            defaultValue = defaultLabel,
            userCanRevert = true,
        )
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
        onItemPicked = {
            settingState.value = it
            setting.setting = settingState.value
            labelState.value = aliases[it] ?: it
            node.label = labelState.value
        },
    )
}
