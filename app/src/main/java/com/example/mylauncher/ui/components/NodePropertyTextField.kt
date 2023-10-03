package com.example.mylauncher.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.example.mylauncher.helper.conditional
import com.example.mylauncher.helper.longPressable
import com.example.mylauncher.ui.theme.DisabledTextFieldColor
import com.example.mylauncher.ui.theme.Foreground
import com.example.mylauncher.ui.theme.outlinedTextFieldColors
import com.example.mylauncher.ui.util.getUserEditableAnnotation
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlin.reflect.KMutableProperty0
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val refresh = PublishSubject.create<Unit>()

fun refreshNodePropertyTextFields() {
    refresh.onNext(Unit)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun NodePropertyTextField(
    property: KMutableProperty0<String>,
    defaultValue: String? = null,
    userCanRevert: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val annotation = remember { property.getUserEditableAnnotation() }
    val input = remember { mutableStateOf(property.get()) }
    var initialValue by remember { mutableStateOf(defaultValue ?: input.value) }
    val locked = remember { mutableStateOf(annotation.locked) }
    var enabled by remember { mutableStateOf(true) }

    fun setValue(value: String) {
        input.value = value
        property.set(value)
    }

    fun clearFocus() {
        focusManager.clearFocus()
        keyboardController?.hide()

        // HACK: Quickly disable and enable to clear selection
        enabled = false
        CoroutineScope(Dispatchers.Main).launch {
            delay(25)
            enabled = true
        }
    }

    DisposableEffect(Unit) {
        val subscription =
            refresh.subscribe {
                input.value = property.get()
                initialValue = input.value
                locked.value = annotation.locked
            }

        onDispose { subscription.dispose() }
    }

    OutlinedTextField(
        value = input.value,
        colors = outlinedTextFieldColors(),
        readOnly = locked.value,
        enabled = !locked.value && enabled,
        onValueChange = ::setValue,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                imeAction = imeAction,
                keyboardType = KeyboardType.Text,
            ),
        keyboardActions = KeyboardActions(onDone = { clearFocus() }),
        label = { Text(annotation.label) },
        supportingText =
            if (annotation.supportingText.isNotEmpty()) {
                { Text(annotation.supportingText) }
            } else null,
        trailingIcon = {
            AnimatedContent(
                targetState = userCanRevert && input.value != initialValue,
                label = "node property text field: lock/revert button"
            ) { showRevertButton ->
                if (showRevertButton) {
                    RevertButton {
                        locked.value = annotation.locked
                        setValue(initialValue)
                        clearFocus()
                    }
                } else if (annotation.locked) {
                    ToggleLockedButton(locked, annotation.userCanUnlock)
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ToggleLockedButton(locked: MutableState<Boolean>, userCanUnlock: Boolean) {
    Icon(
        if (locked.value) Icons.Filled.Lock else Icons.Filled.LockOpen,
        contentDescription = if (locked.value) "closed lock" else "open lock",
        modifier =
            Modifier.conditional(userCanUnlock) {
                longPressable() { locked.value = !locked.value }
            },
        tint = if (userCanUnlock) Foreground else DisabledTextFieldColor
    )
}

@Composable
private fun RevertButton(onLongPressed: () -> Unit) {
    Icon(
        Icons.Filled.Undo,
        contentDescription = "revert changes",
        modifier = Modifier.longPressable(onLongPressed)
    )
}
