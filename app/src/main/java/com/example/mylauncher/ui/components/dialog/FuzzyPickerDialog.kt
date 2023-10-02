package com.example.mylauncher.ui.components.dialog

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mylauncher.helper.verticalScrollShadows
import com.example.mylauncher.ui.theme.Catppuccin
import com.example.mylauncher.ui.theme.Foreground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.xdrop.fuzzywuzzy.FuzzySearch

@Composable
fun <T> FuzzyPickerDialog(
    visible: MutableState<Boolean>,
    items: List<T>,
    itemToString: (T) -> String,
    itemToAnnotatedString: (T) -> AnnotatedString,
    showAnnotatedString: (T, Boolean) -> Boolean,
    onItemPicked: (T) -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    BaseDialog(
        visible,
        Icons.Filled.Search,
        onDismissRequest = onDismissRequest,
        padding = false,
        spacing = false,
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }
        val scrollState = rememberScrollState()
        var query by remember { mutableStateOf("") }
        val matches by remember {
            derivedStateOf { FuzzySearch.extractSorted(query, items, itemToString) }
        }

        val searchBoxSpacing = 8.dp
        val iconSize = 16.dp
        val lineHeight = 16.dp
        val fontSize = with(LocalDensity.current) { lineHeight.toSp() }
        assert(iconSize >= lineHeight)

        Box(
            Modifier.fillMaxWidth().drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = Foreground,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height - strokeWidth),
                    strokeWidth = strokeWidth
                )
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(searchBoxSpacing),
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(
                            horizontal = padding - searchBoxSpacing - iconSize,
                            vertical = padding / 2
                        )
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "search icon",
                    tint = Foreground,
                    modifier = Modifier.size(iconSize)
                )

                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        CoroutineScope(Dispatchers.Main).launch { scrollState.scrollTo(0) }
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Text,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                focusRequester.freeFocus()
                                focusManager.clearFocus(true)
                            }
                        ),
                    textStyle = TextStyle(color = Foreground, fontSize = fontSize),
                    cursorBrush = SolidColor(Foreground),
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                )

                val clearButtonColor by
                    animateColorAsState(
                        if (query.isNotEmpty()) Catppuccin.Current.red else Color.Transparent,
                        label = "fuzzy picker dialog query clear button"
                    )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "clear button",
                    tint = clearButtonColor,
                    modifier = Modifier.size(iconSize).clickable(role = Role.Button) { query = "" }
                )

                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
        }

        @Composable
        fun Item(item: T, score: Int) {
            val string = itemToString(item)
            val distinct = items.map(itemToString).count { it == string } == 1
            val text =
                if (showAnnotatedString(item, distinct)) itemToAnnotatedString(item)
                else buildAnnotatedString { append(itemToString(item)) }
            val alphaVariance = 0.75f
            val alpha = score / 100f * alphaVariance + 1 - alphaVariance

            Text(
                text,
                fontSize = fontSize,
                modifier =
                    Modifier.alpha(alpha).clickable {
                        visible.value = false
                        onItemPicked(item)
                    }
            )
        }

        Box(Modifier.verticalScrollShadows(padding / 2)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                    Modifier.fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = padding, vertical = padding / 2)
            ) {
                if (query.isEmpty()) {
                    items.forEach { Item(it, 100) }
                } else {
                    matches.filter { it.score > 0 }.forEach { Item(it.referent, it.score) }
                }
            }
        }
    }
}
