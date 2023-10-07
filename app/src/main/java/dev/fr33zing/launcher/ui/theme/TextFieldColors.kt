package dev.fr33zing.launcher.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import dev.fr33zing.launcher.ui.util.mix

val DisabledTextFieldColor = Foreground.mix(Background, 0.666f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun outlinedTextFieldColors() =
    TextFieldDefaults.outlinedTextFieldColors(
        disabledTextColor = Foreground.mix(Background, 0.333f),
        disabledLabelColor = Foreground.mix(Background, 0.25f),
        disabledBorderColor = DisabledTextFieldColor,
        disabledPlaceholderColor = DisabledTextFieldColor,
        disabledSupportingTextColor = DisabledTextFieldColor,
        disabledLeadingIconColor = DisabledTextFieldColor,
        disabledTrailingIconColor = DisabledTextFieldColor,
        unfocusedTrailingIconColor = Foreground,
        focusedTrailingIconColor = Foreground
    )
