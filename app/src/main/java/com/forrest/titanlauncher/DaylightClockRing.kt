package com.forrest.titanlauncher

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.forrest.titanlauncher.usage.HourProductivityStatus
import kotlin.math.cos
import kotlin.math.sin

/*
 * DAYLIGHT CLOCK RING
 *
 * Daylight's signature: the time in the middle of 24 dots, one per
 * hour of the day, starting with midnight at the top and running
 * clockwise.
 *
 *  - hours spent mostly on useful apps are drawn in the text colour
 *  - hours where distracting apps took over 20 minutes are orange
 *  - hours still to come, or with no data, are a soft grey
 *  - the current hour is a larger accent dot with a halo
 *
 * With the productivity dots turned off in settings, only the time is
 * shown.
 *
 * subtitle: an optional line under the time ("68° · sunny" on home).
 * When given it replaces the small am/pm line, as in the mockup.
 */
@Composable
internal fun DaylightClockRing(
    time: String,
    statuses: List<HourProductivityStatus>,
    currentHour: Int,
    showDots: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    timeFontSize: TextUnit = 27.sp
) {

    val doneColor = PrimaryText
    val idleColor = ProductivityGray
    val distractedColor = ProductivityRed
    val accent = AccentOrange
    val futureFill = SurfaceBlack

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        if (
            showDots
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {

                /*
                 * Mockup geometry: 9 px dots on an 80 px radius inside
                 * a 176 px ring.
                 */
                val dotRadius =
                    size.minDimension * 0.0256f

                val ringRadius =
                    size.minDimension * 0.4545f

                for (hour in 0 until 24) {

                    val angle =
                        Math.toRadians(
                            hour * 15.0 - 90.0
                        )

                    val point =
                        Offset(
                            center.x + ringRadius * cos(angle).toFloat(),
                            center.y + ringRadius * sin(angle).toFloat()
                        )

                    if (
                        hour == currentHour
                    ) {
                        drawCircle(
                            color = accent.copy(alpha = 0.28f),
                            radius = dotRadius * 1.55f,
                            center = point
                        )

                        drawCircle(
                            color = accent,
                            radius = dotRadius * 0.9f,
                            center = point
                        )
                    } else if (
                        hour > currentHour &&
                        (statuses.getOrNull(hour) ?: HourProductivityStatus.GRAY) ==
                        HourProductivityStatus.GRAY
                    ) {

                        /*
                         * Hours still to come: a hollow dot.
                         */
                        drawCircle(
                            color = futureFill,
                            radius = dotRadius,
                            center = point
                        )

                        drawCircle(
                            color = idleColor,
                            radius = dotRadius - dotRadius * 0.17f,
                            center = point,
                            style = Stroke(width = dotRadius * 0.34f)
                        )
                    } else {

                        val color =
                            when (
                                statuses.getOrNull(hour)
                                    ?: HourProductivityStatus.GRAY
                            ) {
                                HourProductivityStatus.GRAY -> idleColor
                                HourProductivityStatus.PRODUCTIVE -> doneColor
                                HourProductivityStatus.UNPRODUCTIVE -> distractedColor
                            }

                        drawCircle(
                            color = color,
                            radius = dotRadius,
                            center = point
                        )
                    }
                }
            }
        }

        /*
         * "5:19 pm" is shown as a large "5:19" with a small "pm"
         * underneath, so the hour and minute read at a glance.
         */
        val parts =
            time
                .trim()
                .split(" ")

        /*
         * Kept inside the ring: with the dots showing, the time and
         * weather may only use the middle of the circle, so a long
         * forecast ("86° · mostly sunny") never runs into the dots.
         */
        Column(
            modifier =
                if (showDots) Modifier.fillMaxWidth(0.66f) else Modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = parts.first(),
                color = PrimaryText,
                fontFamily = InterfaceFont,
                fontSize = timeFontSize,
                lineHeight = timeFontSize * 1.04f,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = timeFontSize * (-0.035f)
            )

            if (
                subtitle != null
            ) {
                /*
                 * Home passes "86° · mostly sunny": show the degrees
                 * and a weather picture instead of the words. Any
                 * other subtitle is shown as text, trimmed to fit.
                 */
                val temperature =
                    subtitle.substringBefore(" · ", "")

                val condition =
                    subtitle.substringAfter(" · ", "")

                if (
                    temperature.isNotBlank() &&
                    condition.isNotBlank()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = temperature,
                            color = SecondaryText,
                            fontFamily = InterfaceFont,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            maxLines = 1
                        )

                        WeatherIcon(
                            kind = weatherIconFor(condition),
                            size = 14.dp
                        )
                    }
                } else {
                    Text(
                        text = subtitle,
                        color = SecondaryText,
                        fontFamily = InterfaceFont,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (
                parts.size > 1
            ) {
                Text(
                    text = parts.drop(1).joinToString(" "),
                    color = SecondaryText,
                    fontFamily = InterfaceFont,
                    fontSize = 10.sp,
                    lineHeight = 11.sp
                )
            }
        }
    }
}
