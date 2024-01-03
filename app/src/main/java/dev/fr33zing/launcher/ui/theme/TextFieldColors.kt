package dev.fr33zing.launcher.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import dev.fr33zing.launcher.ui.utility.mix

val DisabledTextFieldColor = Foreground.mix(Background, 0.666f)
val DisabledTextFieldLabelColor = Foreground.mix(Background, 0.25f)
val DisabledTextFieldTextColor = Foreground.mix(Background, 0.333f)

@Composable
fun outlinedTextFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        disabledTextColor = DisabledTextFieldTextColor,
        disabledBorderColor = DisabledTextFieldColor,
        disabledLeadingIconColor = DisabledTextFieldColor,
        focusedTrailingIconColor = Foreground,
        unfocusedTrailingIconColor = Foreground,
        disabledTrailingIconColor = DisabledTextFieldColor,
        disabledLabelColor = DisabledTextFieldLabelColor,
        disabledPlaceholderColor = DisabledTextFieldColor,
        disabledSupportingTextColor = DisabledTextFieldColor,
    )
}
