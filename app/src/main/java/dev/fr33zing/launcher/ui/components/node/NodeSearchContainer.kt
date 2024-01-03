package dev.fr33zing.launcher.ui.components.node

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.MainFontFamily
import dev.fr33zing.launcher.ui.utility.conditional
import dev.fr33zing.launcher.ui.utility.mix
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows
import io.reactivex.rxjava3.subjects.PublishSubject
import java.lang.Float.max
import kotlin.math.pow
import kotlinx.coroutines.launch
import me.xdrop.fuzzywuzzy.FuzzySearch

private val searchPanelHeight = 128.dp
private val searchPanelExtraPaddingTop = 16.dp

private const val ALLOWED_OVERSCROLL_FACTOR = 1.5f
private const val OVERSCROLL_RESISTANCE_EXPONENT = 3

private const val SHOW_SCORE_INDICATOR = false

private val activateBestMatchSubject = PublishSubject.create<Unit>()

// TODO fix crash when searching disabled apps
// repro: disable system clock and search 'c'

@Composable
fun NodeSearchContainer(
    db: AppDatabase,
    containerVerticalPadding: Dp,
    panelVerticalPadding: Dp,
    shadowHeight: Dp,
    content: @Composable (ScrollState) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val preferences = Preferences(LocalContext.current)
    val density = LocalDensity.current
    val fontSize = preferences.fontSize.mappedDefault
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
    val nodePayloads = remember { mutableStateListOf<Pair<Node, Payload>>() }

    LaunchedEffect(panelVisible) {
        if (!panelVisible) {
            nodePayloads.clear()
            query.value = ""
        } else {
            db.nodeDao()
                .getAll()
                .map { node ->
                    val payload =
                        db.getPayloadByNodeId(node.kind, node.nodeId)
                            ?: throw Exception("Payload is null")
                    Pair(node, payload)
                }
                .let { nodePayloads.addAll(it) }
        }
    }

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

        if (query.value.isEmpty() || !panelVisible) content(scrollState)
        else SearchResults(db, shadowHeight, nodePayloads, nodeKindFilter, query.value)
    }
}

@Composable
private fun SearchResults(
    db: AppDatabase,
    shadowHeight: Dp,
    nodePayloads: SnapshotStateList<Pair<Node, Payload>>,
    nodeKindFilter: SnapshotStateList<NodeKind>,
    query: String
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scale = remember { mutableFloatStateOf(1f) }
    val dimensions = rememberNodeListDimensions(scale)

    val matches by
        remember(nodePayloads, nodeKindFilter, query) {
            derivedStateOf {
                FuzzySearch.extractSorted(
                    query,
                    nodePayloads.filter {
                        it.first.nodeId != ROOT_NODE_ID &&
                            (nodeKindFilter.isEmpty() || it.first.kind in nodeKindFilter)
                    }
                ) {
                    it.first.label
                }
            }
        }

    DisposableEffect(matches) {
        val subscription =
            activateBestMatchSubject.subscribe {
                matches.firstOrNull()?.referent?.second?.activate(db, context)
            }
        onDispose { subscription.dispose() }
    }

    Box(
        Modifier.absolutePadding(
                bottom =
                    androidx.compose.ui.unit.max(
                        WindowInsets.ime.asPaddingValues().calculateBottomPadding() -
                            shadowHeight * 2,
                        shadowHeight
                    )
            )
            .verticalScrollShadows(shadowHeight)
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scrollState).padding(vertical = shadowHeight)
        ) {
            Column {
                matches
                    .filter { result -> result.score > 0 }
                    .forEach { result ->
                        val (node, payload) = result.referent
                        val interactionSource = remember { MutableInteractionSource() }
                        val indication = rememberCustomIndication(color = node.kind.color)

                        Box(
                            Modifier.fillMaxWidth()
                                .conditional(SHOW_SCORE_INDICATOR) {
                                    drawBehind {
                                        val width = size.width * (result.score / 100f)
                                        drawRect(
                                            node.kind.color.copy(alpha = 0.075f),
                                            size = size.copy(width = width),
                                            topLeft = Offset(x = size.width - width, y = 0f)
                                        )
                                    }
                                }
                                .clickable(interactionSource, indication) {
                                    payload.activate(db, context)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier.padding(
                                        vertical = dimensions.spacing / 2,
                                        horizontal = RecursiveNodeListHorizontalPadding
                                    )
                            ) {
                                val ignoreState = payload is Directory
                                NodeIconAndText(
                                    fontSize = dimensions.fontSize,
                                    lineHeight = dimensions.lineHeight,
                                    label = node.label,
                                    color = node.kind.color(payload, ignoreState),
                                    icon = node.kind.icon(payload, ignoreState),
                                    lineThrough = node.kind.lineThrough(payload, ignoreState)
                                )
                            }
                        }
                    }
            }
        }
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
    val clearButtonColor by
        animateColorAsState(
            targetValue =
                if (query.value.isNotEmpty()) Catppuccin.Current.red else Color.Transparent,
            label = "search clear button color"
        )

    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color = Catppuccin.Current.red, circular = true)

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
                    imeAction = ImeAction.Go,
                    keyboardType = KeyboardType.Password // Disable auto-suggestions
                ),
            keyboardActions =
                KeyboardActions(
                    onGo = {
                        focusRequester.freeFocus()
                        focusManager.clearFocus(true)
                        activateBestMatchSubject.onNext(Unit)
                    }
                ),
            textStyle =
                TextStyle(
                    color = Foreground,
                    fontSize = fontSize,
                    fontFamily = MainFontFamily,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
            decorationBox = { textField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.value.isEmpty())
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

        Icon(
            Icons.Filled.Close,
            contentDescription = "clear query",
            tint = clearButtonColor,
            modifier =
                Modifier.clickable(
                    interactionSource,
                    indication,
                    enabled = query.value.isNotEmpty()
                ) {
                    query.value = ""
                }
        )
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
                        .drawBehind {
                            if (enabled) {
                                val radius = 2.5.dp
                                val offsetY = 6.dp
                                drawCircle(
                                    color = color.copy(alpha = 0.5f),
                                    radius = radius.toPx(),
                                    center =
                                        Offset(
                                            x = size.width / 2,
                                            y = size.height + offsetY.toPx(),
                                        )
                                )
                            }
                        }
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (enabled) color else color.copy(alpha = 0.5f),
                        modifier = Modifier.size(lineHeight)
                    )
                }
            }
        }
    }
}
