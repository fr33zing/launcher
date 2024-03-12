package dev.fr33zing.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.ui.theme.background
import dev.fr33zing.launcher.ui.theme.foreground
import dev.fr33zing.launcher.ui.utility.mix

@Composable
fun ColumnScope.GiantPickerButtonContainer(content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
fun <T> GiantPickerButton(
    text: String,
    onPicked: (T) -> Unit,
    dialog: @Composable (dialogVisible: MutableState<Boolean>, onPicked: (T) -> Unit) -> Unit,
) {
    val dialogVisible = remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val (height, width) = configuration.run { screenHeightDp.dp to screenWidthDp.dp }
    val screenMin = remember(height, width) { if (width < height) width else height }
    val buttonSize = remember(screenMin) { screenMin * 0.4f }
    val buttonIconSize = remember(screenMin) { screenMin * 0.215f }
    val buttonIconTextSpacing = remember(screenMin) { screenMin * -0.02f }
    val buttonFontSizeDp = remember(screenMin) { screenMin * 0.045f }
    val buttonFontSize =
        remember(density, buttonFontSizeDp) { with(density) { buttonFontSizeDp.toSp() } }
    val buttonColor by
        animateColorAsState(
            if (!dialogVisible.value) foreground else foreground.mix(background, 0.75f),
            label = "GiantPickerButton container color",
        )

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.70f else 1f, label = "GiantPickerButton scale")

    Button(
        onClick = { dialogVisible.value = true },
        shape = CircleShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = background,
            ),
        modifier =
            Modifier.requiredSize(buttonSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(pressed) {
                    awaitPointerEventScope {
                        pressed =
                            if (pressed) {
                                waitForUpOrCancellation()
                                false
                            } else {
                                awaitFirstDown(false)
                                true
                            }
                    }
                },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(buttonIconTextSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "search",
                modifier = Modifier.size(buttonIconSize),
            )
            Text(text, fontSize = buttonFontSize)
        }
    }

    dialog(dialogVisible, onPicked)
    //    ApplicationPickerDialog(dialogVisible, onPicked)
}
