package dev.fr33zing.launcher.ui.components.form.payload

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.payloads.Checkbox
import dev.fr33zing.launcher.ui.components.form.EditFormColumn
import dev.fr33zing.launcher.ui.components.form.NodePropertyTextField
import dev.fr33zing.launcher.ui.components.form.OutlinedValue
import dev.fr33zing.launcher.ui.pages.EditFormArguments
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun CheckboxEditForm(arguments: EditFormArguments) {
    val (padding, node, payload, creatingNewNode) = arguments
    val checkbox = payload as Checkbox

    EditFormColumn(padding) {
        NodePropertyTextField(
            node::label,
            autoFocus = creatingNewNode,
        )
        LabeledDateText("Last checked", checkbox.checkedOn)
        LabeledDateText("Last unchecked", checkbox.uncheckedOn)
    }
}

@Composable
private fun LabeledDateText(label: String, date: Date?) =
    OutlinedValue(label) { padding ->
        Box(Modifier.padding(padding)) { date?.let { RelativeDateText(it) } ?: Text("Never") }
    }

@Composable
private fun RelativeDateText(date: Date) {
    Column { DateText(date) }
}

@Composable
private fun DateText(date: Date) {
    val context = LocalContext.current
    val preferences = Preferences(context)
    val use24HourTime by preferences.home.use24HourTime.state
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]
    val timeFormat12Hour = remember(locale) { SimpleDateFormat("hh:mm:ss z", locale) }
    val timeFormat24Hour = remember(locale) { SimpleDateFormat("HH:mm:ss z", locale) }
    val dateFormat = remember(locale) { SimpleDateFormat("EEEE, LLL d, yyyy", locale) }
    val text = remember {
        buildString {
            append(dateFormat.format(date))
            append(" @ ")
            append(
                (if (use24HourTime) timeFormat24Hour else timeFormat12Hour)
                    .format(date)
                    .trimStart('0')
            )
        }
    }

    Text(text)
}
