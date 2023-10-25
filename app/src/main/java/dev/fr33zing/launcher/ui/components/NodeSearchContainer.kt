package dev.fr33zing.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.MainFontFamily
import dev.fr33zing.launcher.ui.util.rememberCustomIndication
import java.lang.Float.max
import kotlin.math.pow
import kotlinx.coroutines.launch

private val searchPanelHeight = 128.dp
private val searchPanelExtraPaddingTop = 16.dp

private const val ALLOWED_OVERSCROLL_FACTOR = 1.5f
private const val OVERSCROLL_RESISTANCE_EXPONENT = 3

@Composable
fun NodeSearchContainer(
    containerVerticalPadding: Dp,
    panelVerticalPadding: Dp,
    content: @Composable (ScrollState) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val density = LocalDensity.current
    val fontSize = Preferences.fontSizeDefault
    val lineHeight = with(density) { fontSize.toDp() }

    var currentPanelHeight by remember { mutableFloatStateOf(0f) }
    val maxPanelHeight = with(density) { searchPanelHeight.toPx() }
    val animatedPanelHeight = remember { Animatable(0f) }
    val panelAlpha by remember { derivedStateOf { animatedPanelHeight.value / maxPanelHeight } }
    val panelVisible by remember {
        derivedStateOf { currentPanelHeight != 0f || animatedPanelHeight.targetValue != 0f }
    }

    val query = remember { mutableStateOf("") }
    val nodeKindFilter = remember { mutableStateListOf<NodeKind>() }

    BackHandler(enabled = panelVisible) {
        currentPanelHeight = 0f
        coroutineScope.launch { animatedPanelHeight.animateTo(currentPanelHeight) }
        focusManager.clearFocus()
    }

    Column(
        modifier =
            Modifier.padding(vertical = containerVerticalPadding).pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)

                    val scrolledToTop = !scrollState.canScrollBackward
                    val panelFullyVisible = animatedPanelHeight.targetValue == maxPanelHeight
                    if (!scrolledToTop || panelFullyVisible || animatedPanelHeight.isRunning)
                        return@awaitEachGesture

                    var totalChangePx = 0f
                    val touchSlop = viewConfiguration.touchSlop

                    // Pressed
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } != 1) continue

                        val change = event.changes[0]
                        val changePx = change.position.y - change.previousPosition.y

                        if (changePx < 0) break

                        totalChangePx += changePx
                        if (totalChangePx < touchSlop) continue

                        val heightChange =
                            if ((currentPanelHeight + changePx) <= maxPanelHeight) changePx
                            else {
                                val allowedOverscroll = maxPanelHeight * ALLOWED_OVERSCROLL_FACTOR
                                val overscroll = (currentPanelHeight + changePx) - maxPanelHeight
                                val overscrollRatio = (1 - overscroll / allowedOverscroll)
                                val resistance = overscrollRatio.pow(OVERSCROLL_RESISTANCE_EXPONENT)

                                resistance * changePx
                            }

                        change.consume()
                        coroutineScope.launch {
                            currentPanelHeight = max(0f, currentPanelHeight + heightChange)
                            animatedPanelHeight.snapTo(currentPanelHeight)
                        }
                    } while (event.changes.any { it.pressed })

                    // Released
                    coroutineScope.launch {
                        currentPanelHeight =
                            if (animatedPanelHeight.value >= maxPanelHeight / 2) maxPanelHeight
                            else 0.0f

                        if (currentPanelHeight > 0f) {
                            focusRequester.requestFocus()
                        }
                        animatedPanelHeight.animateTo(currentPanelHeight)
                    }
                }
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.height(with(density) { animatedPanelHeight.value.toDp() }).clipToBounds()
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier =
                    Modifier.fillMaxWidth()
                        .requiredHeight(searchPanelHeight)
                        .alpha(panelAlpha)
                        .padding(
                            vertical = panelVerticalPadding,
                            horizontal = RecursiveNodeListHorizontalPadding,
                        )
                        .absolutePadding(top = searchPanelExtraPaddingTop),
            ) {
                SearchBox(query, focusRequester, focusManager, fontSize, lineHeight)
                SearchFilters(nodeKindFilter, lineHeight)
            }
        }

        content(scrollState)
    }
}

@Composable
private fun SearchBox(
    query: MutableState<String>,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
    fontSize: TextUnit,
    lineHeight: Dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(lineHeight / 2),
    ) {
        Icon(Icons.Filled.Search, contentDescription = "search")

        BasicTextField(
            value = query.value,
            onValueChange = { query.value = it },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Password // Disable auto-suggestions
                ),
            keyboardActions =
                KeyboardActions(
                    onDone = {
                        focusRequester.freeFocus()
                        focusManager.clearFocus(true)
                    }
                ),
            textStyle =
                TextStyle(
                    color = Foreground,
                    fontSize = fontSize,
                    fontFamily = MainFontFamily,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
            cursorBrush = SolidColor(Foreground),
            modifier = Modifier.focusRequester(focusRequester).weight(1f)
        )

        Icon(Icons.Filled.Close, contentDescription = "search")
    }
}

@Composable
fun SearchFilters(nodeKindFilter: SnapshotStateList<NodeKind>, lineHeight: Dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        NodeKind.values().forEach { nodeKind ->
            key(nodeKind) {
                val icon = remember(nodeKind) { nodeKind.icon }
                val color = remember(nodeKind) { nodeKind.color }
                val interactionSource = remember { MutableInteractionSource() }
                val indication =
                    rememberCustomIndication(
                        color = color,
                        circular = true,
                        circularSizeFactor = 1.45f,
                    )

                val enabled = nodeKindFilter.contains(nodeKind)

                Box(
                    Modifier.clickable(interactionSource, indication) {
                        if (enabled) nodeKindFilter.remove(nodeKind)
                        else nodeKindFilter.add(nodeKind)
                    }
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (enabled) color else color.copy(alpha = 0.3f),
                        modifier = Modifier.size(lineHeight)
                    )
                }
            }
        }
    }
}
