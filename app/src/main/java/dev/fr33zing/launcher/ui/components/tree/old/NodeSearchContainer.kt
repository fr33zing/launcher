package dev.fr33zing.launcher.ui.components.tree

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.NodeMinimal
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.utility.rememberFuzzyMatcher
import dev.fr33zing.launcher.ui.components.tree.old.NodeIconAndText
import dev.fr33zing.launcher.ui.components.tree.old.NodeListDimensions
import dev.fr33zing.launcher.ui.components.tree.old.RecursiveNodeListHorizontalPadding
import dev.fr33zing.launcher.ui.components.tree.old.rememberNodeListDimensions
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.MainFontFamily
import dev.fr33zing.launcher.ui.utility.mix
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows
import io.reactivex.rxjava3.subjects.PublishSubject
import java.lang.Float.max
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.math.pow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val searchPanelHeight = 128.dp
private val searchPanelExtraPaddingTop = 16.dp

private const val ALLOWED_OVERSCROLL_FACTOR = 1.5f
private const val OVERSCROLL_RESISTANCE_EXPONENT = 3

/** Database is considered slow after this delay. */
private const val SLOW_DATABASE_DELAY_MS: Long = 1000

private val activateBestMatchSubject = PublishSubject.create<Unit>()

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
    val fontSize = preferences.nodeAppearance.fontSize.mappedDefault
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
    val nodePayloads = remember { mutableStateListOf<Pair<NodeMinimal, Payload>>() }
    var databaseSlow by remember { mutableStateOf(false) }

    val scale = remember { mutableFloatStateOf(1f) }
    val nodeDimensions = rememberNodeListDimensions(scale)

    fun closePanel() {
        currentPanelHeight = 0f
        coroutineScope.launch { animatedPanelHeight.animateTo(currentPanelHeight) }
        focusManager.clearFocus()
    }

    LaunchedEffect(panelVisible) {
        if (!panelVisible) {
            nodePayloads.clear()
            query.value = ""
            databaseSlow = false
        } else {
            val timer = Timer()
            timer.schedule(SLOW_DATABASE_DELAY_MS) { databaseSlow = true }

            db.nodeDao()
                .getAllMinimal()
                .map { node ->
                    val payload =
                        db.getPayloadByNodeId(node.kind, node.nodeId)
                            ?: throw Exception("Payload is null")
                    Pair(node, payload)
                }
                .let {
                    nodePayloads.addAll(it)
                    timer.cancel()
                    databaseSlow = false
                }
        }
    }

    BackHandler(enabled = panelVisible) { closePanel() }

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
                SearchBox(query, focusRequester, focusManager, fontSize, lineHeight, ::closePanel)
                SearchFilters(nodeKindFilter, lineHeight)
            }
        }

        if (query.value.isEmpty() || !panelVisible) {
            content(scrollState)
        } else if (nodePayloads.isNotEmpty()) {
            SearchResults(
                db,
                shadowHeight,
                nodePayloads,
                nodeKindFilter,
                nodeDimensions,
                query.value
            )
        } else if (databaseSlow) {
            Text(
                "Reading database...",
                textAlign = TextAlign.Center,
                color = Foreground.mix(Background, 0.5f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SearchResults(
    db: AppDatabase,
    shadowHeight: Dp,
    nodePayloads: SnapshotStateList<Pair<NodeMinimal, Payload>>,
    nodeKindFilter: SnapshotStateList<NodeKind>,
    dimensions: NodeListDimensions,
    query: String
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()

    val fuzzyMatcher = rememberFuzzyMatcher(nodePayloads) { it.first.label }
    val matches = remember(nodePayloads, query) { fuzzyMatcher.match(query) }

    LaunchedEffect(query) { lazyListState.scrollToItem(0, -1000) }

    DisposableEffect(matches) {
        val subscription =
            activateBestMatchSubject.subscribe {
                val payload = matches.firstOrNull()?.element?.second
                if (payload !is Directory) payload?.activate(db, context)
            }
        onDispose { subscription.dispose() }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(vertical = shadowHeight),
        modifier = Modifier.fillMaxSize().verticalScrollShadows(shadowHeight)
    ) {
        items(
            matches.filter {
                it.element.first.nodeId != ROOT_NODE_ID &&
                    (nodeKindFilter.isEmpty() || it.element.first.kind in nodeKindFilter)
            },
            { it.element.first.nodeId }
        ) { result ->
            val (node, payload) = result.element
            // HACK: Use a second query to get real-time updates.
            val payloadState by
                db.getPayloadFlowByNodeId(node.kind, node.nodeId)
                    .map { it ?: payload }
                    .collectAsStateWithLifecycle(initialValue = payload)
            val ignoreState = payload is Directory
            val color = node.kind.color(payloadState, ignoreState)
            val interactionSource = remember { MutableInteractionSource() }
            val indication = rememberCustomIndication(color = node.kind.color)
            val text = buildAnnotatedString {
                result.substrings.forEach {
                    withStyle(
                        SpanStyle(
                            color = color,
                            background =
                                if (it.matches) node.kind.color.copy(alpha = 0.25f)
                                else Color.Transparent,
                        )
                    ) {
                        append(it.text)
                    }
                }
            }

            Box(
                Modifier.fillMaxWidth().clickable(interactionSource, indication) {
                    if (payload !is Directory) payloadState.activate(db, context)
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
                    NodeIconAndText(
                        fontSize = dimensions.fontSize,
                        lineHeight = dimensions.lineHeight,
                        label = text,
                        color = node.kind.color(payloadState, ignoreState),
                        icon = node.kind.icon(payloadState, ignoreState),
                        lineThrough = node.kind.lineThrough(payloadState, ignoreState)
                    )
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
    closePanelFn: () -> Unit
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

        val actionButtonClearsQuery = query.value.isNotEmpty()
        val actionButtonColor = Catppuccin.Current.red
        val interactionSource = remember { MutableInteractionSource() }
        val indication = rememberCustomIndication(color = actionButtonColor, circular = true)
        Icon(
            if (actionButtonClearsQuery) Icons.Filled.Close else Icons.Filled.ArrowUpward,
            contentDescription = "query action button",
            tint = actionButtonColor,
            modifier =
                Modifier.clickable(
                    interactionSource,
                    indication,
                ) {
                    if (actionButtonClearsQuery) {
                        query.value = ""
                    } else {
                        closePanelFn()
                    }
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
        NodeKind.entries.forEach { nodeKind ->
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
