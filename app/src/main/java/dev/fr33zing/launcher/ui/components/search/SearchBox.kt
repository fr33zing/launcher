package dev.fr33zing.launcher.ui.components.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.MainFontFamily
import dev.fr33zing.launcher.ui.utility.mix
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

@Composable
fun SearchBox(
    query: String,
    updateQuery: (query: String) -> Unit,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = LocalNodeDimensions.current.fontSize,
    lineHeight: Dp = LocalNodeDimensions.current.lineHeight,
    onGo: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(lineHeight / 2),
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.Search, contentDescription = "search")

        BasicTextField(
            value = query,
            onValueChange = updateQuery,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Go,
                    keyboardType = KeyboardType.Password // Disable auto-suggestions
                ),
            keyboardActions = KeyboardActions(onGo = { onGo() }),
            textStyle =
                TextStyle(
                    color = Foreground,
                    fontSize = fontSize,
                    fontFamily = MainFontFamily,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
            decorationBox = { textField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty())
                        Text(
                            "Begin typing to search...",
                            style =
                                TextStyle(
                                    color = Foreground.mix(Background, 0.5f),
                                    fontSize = fontSize * 0.85f,
                                    fontFamily = MainFontFamily,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                ),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    textField()
                }
            },
            cursorBrush = SolidColor(Foreground),
            modifier = Modifier.focusRequester(focusRequester).weight(1f)
        )

        val clearQueryButtonAlpha by
            animateFloatAsState(if (query.isEmpty()) 0f else 1f, label = "clear query button alpha")
        val clearQueryButtonColor = Catppuccin.Current.red.copy(alpha = clearQueryButtonAlpha)
        val interactionSource = remember { MutableInteractionSource() }
        val indication = rememberCustomIndication(color = clearQueryButtonColor, circular = true)

        Icon(
            Icons.Filled.Close,
            contentDescription = "clear query button",
            tint = clearQueryButtonColor,
            modifier =
                Modifier.clickable(
                    interactionSource,
                    indication,
                ) {
                    updateQuery("")
                }
        )
    }
}
