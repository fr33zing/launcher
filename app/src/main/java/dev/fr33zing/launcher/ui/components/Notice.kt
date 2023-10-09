package dev.fr33zing.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.util.mix
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.Timer
import java.util.UUID
import kotlin.concurrent.timerTask

private const val NOTICE_DURATION_MS = 3000
private const val ANIMATION_DURATION_MS = 300

private val noticesSubject = PublishSubject.create<Notice>()

data class Notice(val id: String, val text: String, val kind: NoticeKind) {
    val initialized: MutableState<Boolean> = mutableStateOf(false)
    val visible: MutableState<Boolean> = mutableStateOf(false)
    val uuid: UUID = UUID.randomUUID()
}

enum class NoticeKind(val color: Color, val icon: ImageVector) {
    Information(
        color = Background.mix(Foreground, 0.1f),
        icon = Icons.Filled.Info,
    ),
    Warning(
        color = Background.mix(Catppuccin.Current.red, 0.1f),
        icon = Icons.Filled.Warning,
    ),
}

fun sendNotice(
    id: String,
    text: String,
    kind: NoticeKind = NoticeKind.Information,
) = noticesSubject.onNext(Notice(id, text, kind))

@Composable
fun Notices() {
    val slideAnimSpec = remember { tween<IntOffset>(ANIMATION_DURATION_MS) }
    val fadeAnimSpec = remember { tween<Float>(ANIMATION_DURATION_MS) }
    val notices = remember { mutableStateOf(listOf<Notice>()) }

    DisposableEffect(Unit) {
        val subscription = noticesSubject.subscribe { notices.value = notices.value + it }
        onDispose { subscription.dispose() }
    }

    with(notices.value.firstOrNull() ?: return) {
        LaunchedEffect(initialized.value) {
            if (initialized.value) return@LaunchedEffect

            initialized.value = true
            visible.value = true

            Timer()
                .schedule(
                    timerTask { visible.value = false },
                    NOTICE_DURATION_MS - ANIMATION_DURATION_MS.toLong()
                )
            Timer()
                .schedule(
                    timerTask { notices.value = notices.value.filter { it.uuid != uuid } },
                    NOTICE_DURATION_MS.toLong()
                )
        }

        AnimatedVisibility(
            visible = visible.value,
            enter = fadeIn(fadeAnimSpec) + slideInVertically(slideAnimSpec) { -it },
            exit = fadeOut(fadeAnimSpec) + slideOutVertically(slideAnimSpec) { -it },
        ) {
            Box(Modifier.fillMaxWidth().background(kind.color)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .absolutePadding(
                                top =
                                    with(LocalDensity.current) {
                                        WindowInsets.statusBars.getTop(this).toDp()
                                    }
                            )
                ) {
                    Icon(kind.icon, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text)
                }
            }
        }
    }
}
