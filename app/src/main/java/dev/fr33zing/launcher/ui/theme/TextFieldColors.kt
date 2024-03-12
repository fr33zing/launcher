package dev.fr33zing.launcher.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import dev.fr33zing.launcher.ui.utility.mix

val DisabledTextFieldColor = foreground.mix(background, 0.666f)
val DisabledTextFieldLabelColor = foreground.mix(background, 0.25f)
val DisabledTextFieldTextColor = foreground.mix(background, 0.333f)

@Composable
fun outlinedTextFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        disabledTextColor = DisabledTextFieldTextColor,
        disabledBorderColor = DisabledTextFieldColor,
        disabledLeadingIconColor = DisabledTextFieldColor,
        focusedTrailingIconColor = foreground,
        unfocusedTrailingIconColor = foreground,
        disabledTrailingIconColor = DisabledTextFieldColor,
        disabledLabelColor = DisabledTextFieldLabelColor,
        disabledPlaceholderColor = DisabledTextFieldColor,
        disabledSupportingTextColor = DisabledTextFieldColor,
    )
}
