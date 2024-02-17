package dev.fr33zing.launcher.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.NextAlarmFlow
import dev.fr33zing.launcher.R
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.utility.launchCalendarApplication
import dev.fr33zing.launcher.data.utility.launchClockApplication
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalTextApi::class)
private fun makeFontFamily(weight: Int) =
    FontFamily(
        Font(
            R.font.roboto_flex,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(weight),
                    FontVariation.width(110.0f),
                ),
        )
    )

private val noFontPaddingTextStyle =
    TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
private val timeSpanStyle = SpanStyle(fontSize = 64.sp, fontFamily = makeFontFamily(100))
private val amPmSpanStyle = SpanStyle(fontSize = 32.sp, fontFamily = makeFontFamily(300))
private val dateSpanStyle = SpanStyle(fontSize = 32.sp, fontFamily = makeFontFamily(200))
private val weekSpanStyle = SpanStyle(fontSize = 14.sp, fontFamily = makeFontFamily(400))
private val spacerStyle = SpanStyle(fontSize = 42.sp, fontFamily = makeFontFamily(200))

val nextAlarmColor = Catppuccin.Current.red
private val nextAlarmSpanStyle =
    SpanStyle(fontSize = 16.sp, fontFamily = makeFontFamily(300), color = nextAlarmColor)
private val nextAlarmAmPmSpanStyle =
    SpanStyle(fontSize = 12.sp, fontFamily = makeFontFamily(300), color = nextAlarmColor)

private fun AnnotatedString.Builder.spacer() {
    withStyle(spacerStyle) { append(" ") }
}

@Composable
fun Clock(nextAlarmFlow: NextAlarmFlow, horizontalPadding: Dp) {
    val context = LocalContext.current
    val preferences = Preferences(context)
    val use24HourTime by preferences.home.use24HourTime.state
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]
    val nextAlarm by
        nextAlarmFlow.map { it?.run { Date(triggerTime) } }.collectAsStateWithLifecycle(null)

    val timeFormat12Hour = remember(locale) { SimpleDateFormat("hh:mm", locale) }
    val timeFormat24Hour = remember(locale) { SimpleDateFormat("HH:mm", locale) }
    val amPmFormat = remember(locale) { SimpleDateFormat("a", locale) }
    val dateFormat = remember(locale) { SimpleDateFormat("EEEE, LLL d", locale) }
    val weekFormat = remember(locale) { SimpleDateFormat("w", locale) }

    var currentTime by remember { mutableStateOf(buildAnnotatedString {}) }
    var currentDate by remember { mutableStateOf(buildAnnotatedString {}) }

    fun updateTime() {
        val now = Date()
        currentTime = buildAnnotatedString {
            withStyle(timeSpanStyle) {
                append(
                    (if (use24HourTime) timeFormat24Hour else timeFormat12Hour)
                        .format(now)
                        .trimStart('0')
                )
            }
            if (!use24HourTime) {
                spacer()
                withStyle(amPmSpanStyle) { append(amPmFormat.format(now)) }
            }

            nextAlarm?.let { nextAlarm ->
                spacer()
                withStyle(nextAlarmSpanStyle) {
                    appendInlineContent("alarm")
                    append(" ")
                    append(
                        (if (use24HourTime) timeFormat24Hour else timeFormat12Hour)
                            .format(nextAlarm)
                            .trimStart('0')
                    )
                }
                if (!use24HourTime) {
                    append(" ")
                    withStyle(nextAlarmAmPmSpanStyle) { append(amPmFormat.format(nextAlarm)) }
                }
            }
        }

        currentDate = buildAnnotatedString {
            withStyle(dateSpanStyle) { append(dateFormat.format(now)) }
            spacer()
            withStyle(weekSpanStyle) {
                append("Week ")
                append(weekFormat.format(now))
            }
        }
    }

    val clockPackage by preferences.home.defaultApplications.clock.state
    val calendarPackage by preferences.home.defaultApplications.calendar.state

    LaunchedEffect(use24HourTime, nextAlarm) { updateTime() }
    DisposableEffect(Unit) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    updateTime()
                }
            }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_TIME_TICK))
        onDispose { context.unregisterReceiver(receiver) }
    }

    Column {
        // TODO add setting to choose default clock and calendar app
        Element(currentTime, horizontalPadding, verticalPadding = 0.dp) {
            launchClockApplication(context, clockPackage)
        }
        Element(currentDate, horizontalPadding, verticalPadding = 8.dp) {
            launchCalendarApplication(context, calendarPackage)
        }
    }
}

@Composable
private fun Element(
    text: AnnotatedString,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication()

    val inlineContents = remember {
        mapOf(
            "alarm" to
                InlineTextContent(
                    Placeholder(
                        width = 1.em,
                        height = 1.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    Icon(
                        Icons.Outlined.Alarm,
                        contentDescription = "alarm",
                        modifier = Modifier.fillMaxSize(),
                        tint = nextAlarmColor,
                    )
                }
        )
    }

    Box(Modifier.fillMaxWidth().clickable(interactionSource, indication, onClick = onClick)) {
        Text(
            text,
            style = noFontPaddingTextStyle,
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            inlineContent = inlineContents,
        )
    }
}
