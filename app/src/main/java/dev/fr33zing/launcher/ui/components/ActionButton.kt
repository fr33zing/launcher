package dev.fr33zing.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.mix
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

val ActionButtonVerticalPadding = 24.dp
val ActionButtonSpacing = 36.dp

private val BUTTON_SIZE = 64.dp
private const val ICON_SIZE_RATIO = 0.375f
private const val BACKGROUND_ALPHA = 0.625f
private val BACKGROUND_COLOR = Background.copy(alpha = BACKGROUND_ALPHA)
private val ICON_COLOR = Foreground.mix(Background, 0.3f)

@Composable
fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication()

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier.size(BUTTON_SIZE)
                .background(BACKGROUND_COLOR, shape = CircleShape)
                .clip(CircleShape)
                .clickable(interactionSource, indication, onClick = onClick)
    ) {
        Icon(icon, contentDescription, Modifier.size(BUTTON_SIZE * ICON_SIZE_RATIO), ICON_COLOR)
    }
}
