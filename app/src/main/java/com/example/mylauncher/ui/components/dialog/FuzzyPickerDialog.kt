package com.example.mylauncher.ui.components.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.example.mylauncher.ui.theme.Foreground

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> FuzzyPickerDialog(
    visible: MutableState<Boolean>,
    items: List<T>,
    itemText: (T) -> String,
    onItemPicked: (T) -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    val focusRequester = FocusRequester()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var query by remember { mutableStateOf("") }

    BaseDialog(
        visible,
        Icons.Filled.Search,
        onDismissRequest = onDismissRequest,
        padding = false,
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Box(Modifier.padding(padding / 4).fillMaxWidth()) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text,
                    ),
                keyboardActions = KeyboardActions(onSearch = {}),
                textStyle = TextStyle(color = Foreground),
                cursorBrush = SolidColor(Foreground),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )

            LaunchedEffect(visible) {
                if (visible.value) {
                    focusManager.clearFocus()
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            //
        }
    }
}
