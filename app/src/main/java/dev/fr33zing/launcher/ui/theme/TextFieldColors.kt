package dev.fr33zing.launcher.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import dev.fr33zing.launcher.ui.util.mix

val DisabledTextFieldColor = Foreground.mix(Background, 0.666f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun outlinedTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        disabledTextColor = Foreground.mix(Background, 0.333f),
        disabledBorderColor = DisabledTextFieldColor,
        disabledLeadingIconColor = DisabledTextFieldColor,
        focusedTrailingIconColor = Foreground,
        unfocusedTrailingIconColor = Foreground,
        disabledTrailingIconColor = DisabledTextFieldColor,
        disabledLabelColor = Foreground.mix(Background, 0.25f),
        disabledPlaceholderColor = DisabledTextFieldColor,
        disabledSupportingTextColor = DisabledTextFieldColor,
    )
