package dev.fr33zing.launcher.ui.components.tree.old

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.dialog.baseDialogBorderColor
import dev.fr33zing.launcher.ui.components.dialog.baseDialogBorderWidth
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Dim
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.theme.typography
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

private val verticalPadding = 8.dp

@Composable
fun BatchModeStatus(treeState: TreeState) {
    val interactionSource = remember { MutableInteractionSource() }
    val openMenuIndication = rememberCustomIndication(circular = true, circularSizeFactor = 1f)
    val preferences = Preferences(LocalContext.current)
    val spacing by preferences.nodeAppearance.spacing.state
    val showMenu = remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = treeState.mode == TreeState.Mode.Batch && treeState.batchState != null,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = ScreenHorizontalPadding, vertical = verticalPadding)
                    .absolutePadding(top = spacing / 2)
        ) {
            val selectedCount =
                remember(treeState) { treeState.batchState?.selectedKeys?.count { it.value } ?: 0 }
            Text("$selectedCount items selected", fontWeight = FontWeight.Bold)

            Box {
                Icon(
                    Icons.Filled.MoreHoriz,
                    contentDescription = "menu button",
                    modifier =
                        Modifier.clickable(interactionSource, openMenuIndication) {
                            showMenu.value = true
                        }
                )
                Menu(showMenu, treeState)
            }
        }
    }
}

@Composable
private fun Menu(showMenu: MutableState<Boolean>, treeState: TreeState) {
    CompositionLocalProvider(
        LocalTextStyle provides
            remember {
                typography.labelLarge.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            }
    ) {
        val density = LocalDensity.current
        val style = LocalTextStyle.current
        val lineHeight = remember { with(density) { style.fontSize.toDp() } }

        val menuAlpha by
            animateFloatAsState(
                targetValue = if (showMenu.value) 1f else 0f,
                label = "batch mode menu alpha"
            )
        if (menuAlpha > 0f) {
            Popup(
                onDismissRequest = { showMenu.value = false },
                alignment = Alignment.TopEnd,
                offset = with(LocalDensity.current) { IntOffset(x = 0, y = 32.dp.roundToPx()) },
                properties = PopupProperties(focusable = true)
            ) {
                val shape = MaterialTheme.shapes.small
                Column(
                    modifier =
                        Modifier.alpha(menuAlpha)
                            .background(Background, shape)
                            .border(baseDialogBorderWidth, baseDialogBorderColor, shape)
                            .clip(shape)
                            .width(IntrinsicSize.Max)
                            .padding(vertical = lineHeight)
                ) {
                    val noneSelected =
                        remember(treeState) { (treeState.batchState?.selectedKeys?.size ?: 0) == 0 }
                    MenuItem("Select all", Icons.Filled.CheckBox)
                    MenuItem(
                        "Deselect all",
                        Icons.Filled.CheckBoxOutlineBlank,
                        enabled = !noneSelected
                    )
                    MenuItem("Move", Icons.Filled.DriveFileMove)
                    MenuItem("Trash", Icons.Filled.Delete)
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication()
    val density = LocalDensity.current
    val style = LocalTextStyle.current
    val lineHeight = remember { with(density) { style.fontSize.toDp() } }
    val color by
        animateColorAsState(
            targetValue = if (enabled) Foreground else Dim,
            label = "batch mode menu item color"
        )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(lineHeight * 0.75f),
        modifier =
            Modifier.clickable(interactionSource, indication) { if (enabled) onClick() }
                .padding(horizontal = lineHeight * 2, vertical = lineHeight * 0.75f)
                .fillMaxWidth()
    ) {
        Icon(icon, text, Modifier.size(lineHeight * 1.25f), color)
        Text(text, color = color, maxLines = 1)
    }
}
