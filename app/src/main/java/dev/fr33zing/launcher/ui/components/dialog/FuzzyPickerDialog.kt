package dev.fr33zing.launcher.ui.components.dialog

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.MainFontFamily
import dev.fr33zing.launcher.ui.theme.background
import dev.fr33zing.launcher.ui.theme.foreground
import dev.fr33zing.launcher.ui.utility.mix
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private val itemSpacing = 16.dp

// TODO remove platform font padding

@Composable
fun <T> FuzzyPickerDialog(
    visible: MutableState<Boolean>,
    items: List<T>,
    itemToString: (T) -> String,
    itemToAnnotatedString: (T, TextUnit, Color) -> AnnotatedString,
    showAnnotatedString: (T, Boolean) -> Boolean,
    onItemPicked: (T) -> Unit,
    onDismissRequest: () -> Unit = {},
) {
    BaseDialog(
        visible,
        Icons.Filled.Search,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxSize(),
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
                    color = foreground,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height - strokeWidth),
                    strokeWidth = strokeWidth,
                )
            },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(searchBoxSpacing),
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(
                            horizontal = padding - searchBoxSpacing - iconSize,
                            vertical = padding / 2,
                        ),
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "search icon",
                    tint = foreground,
                    modifier = Modifier.size(iconSize),
                )
                Box(Modifier.weight(1f)) {
                    val offsetModifier = Modifier.offset(y = lineHeight * -0.1f)

                    if (query.isEmpty()) {
                        Text(
                            "Begin typing to search...",
                            fontSize = fontSize,
                            color = foreground.mix(background, 0.666f),
                            modifier = offsetModifier,
                        )
                    }

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
                                },
                            ),
                        textStyle =
                            TextStyle(
                                color = foreground,
                                fontSize = fontSize,
                                fontFamily = MainFontFamily,
                            ),
                        cursorBrush = SolidColor(foreground),
                        modifier = Modifier.focusRequester(focusRequester).then(offsetModifier),
                    )
                }

                val clearButtonColor by
                    animateColorAsState(
                        if (query.isNotEmpty()) Catppuccin.current.red else Color.Transparent,
                        label = "fuzzy picker dialog query clear button",
                    )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "clear button",
                    tint = clearButtonColor,
                    modifier =
                        Modifier.size(iconSize).clickable(role = Role.Button) { query = "" },
                )

                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
        }

        @Composable
        fun Item(
            item: T,
            padding: Dp,
            alpha: Double,
            color: Color,
        ) {
            val interactionSource = remember(color) { MutableInteractionSource() }
            val indication = rememberCustomIndication(color = color)

            Box(
                Modifier.clickable(
                    interactionSource,
                    indication,
                    onClick = {
                        visible.value = false
                        onItemPicked(item)
                    },
                ),
            ) {
                val itemFontSize = 19.sp
                val string = itemToString(item)
                val distinct = items.map(itemToString).count { it == string } == 1
                val text =
                    if (showAnnotatedString(item, distinct)) {
                        itemToAnnotatedString(item, fontSize, color)
                    } else {
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = color, fontSize = itemFontSize)) {
                                append(itemToString(item))
                            }
                        }
                    }

                Text(
                    text,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = padding, vertical = itemSpacing / 2)
                            .alpha(alpha.toFloat()),
                )
            }
        }

        Box(Modifier.verticalScrollShadows(padding / 2)) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(vertical = padding / 2),
            ) {
                if (query.isEmpty()) {
                    items.forEach { Item(it, padding, 1.0, foreground) }
                } else {
                    styledItems(matches).forEach {
                        with(it) { Item(referent, padding, alpha, color) }
                    }
                }
            }
        }
    }
}

private data class StyledItem<T>(val referent: T, val alpha: Double, val color: Color)

private fun <T> styledItems(matches: MutableList<BoundExtractedResult<T>>): List<StyledItem<T>> {
    val minVisibleAlpha = 0.5f
    val vMin = 1.0
    val vMax = 3.0
    val scores = matches.map { it.score.toFloat() }
    val mean = scores.average()
    val sumOfSquares = scores.sumOf { (it - mean).pow(2) }
    val variance = sumOfSquares / (scores.size - 1)
    val stdDev = sqrt(variance)
    val pairs =
        matches.map {
            val varianceStdDevs = (it.score - mean) / stdDev
            val alpha = min(1.0, max(0.0, varianceStdDevs - vMin) / (vMax - vMin))
            Pair(it, alpha)
        }
    val maxPairAlpha = pairs.maxOf { it.second }
    val oneBestResult = pairs.count { it.second == 1.0 } == 1
    return pairs
        .filter { it.first.score > 0 }
        .map {
            val alpha = it.second / maxPairAlpha * (1 - minVisibleAlpha) + minVisibleAlpha
            StyledItem(
                referent = it.first.referent,
                alpha = alpha,
                color = if (oneBestResult && alpha == 1.0) Catppuccin.current.green else foreground,
            )
        }
}
