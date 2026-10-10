package com.forrest.titanlauncher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/*
 * WEATHER ICON
 *
 * The small picture beside the temperature under the clock, drawn
 * here rather than taken from an icon set so it matches Daylight
 * and needs no images:
 *
 *   Sunny          sun
 *   Clear          moon
 *   Mostly sunny / Partly cloudy   sun (or moon at night) behind a cloud
 *   Cloudy         cloud
 *   Drizzle / Rain / Showers       cloud with rain drops
 *   Snow           cloud with snowflakes
 *   Thunderstorms  cloud with a lightning bolt
 *   Foggy          fog lines
 */
internal enum class WeatherIconKind {
    SUN,
    MOON,
    SUN_CLOUD,
    MOON_CLOUD,
    CLOUD,
    RAIN,
    SNOW,
    STORM,
    FOG
}

/*
 * From the condition WeatherRepository gives ("Sunny", "Mostly
 * clear", "Rain"...). "Partly cloudy" has no day/night, so the hour
 * decides between sun and moon.
 */
internal fun weatherIconFor(
    condition: String
): WeatherIconKind {

    val text =
        condition.trim().lowercase()

    val hour =
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    val daytime =
        hour in 6..19

    return when {
        text.contains("thunder") -> WeatherIconKind.STORM
        text.contains("snow") -> WeatherIconKind.SNOW
        text.contains("rain") ||
                text.contains("drizzle") ||
                text.contains("shower") -> WeatherIconKind.RAIN
        text.contains("fog") -> WeatherIconKind.FOG
        text == "sunny" -> WeatherIconKind.SUN
        text == "clear" -> WeatherIconKind.MOON
        text == "mostly sunny" -> WeatherIconKind.SUN_CLOUD
        text == "mostly clear" -> WeatherIconKind.MOON_CLOUD
        text.contains("partly") ->
            if (daytime) WeatherIconKind.SUN_CLOUD else WeatherIconKind.MOON_CLOUD
        else -> WeatherIconKind.CLOUD
    }
}

private val SunColor =
    Color(0xFFF5B83D)

private val RainColor =
    Color(0xFF6E9BFF)

@Composable
internal fun WeatherIcon(
    kind: WeatherIconKind,
    size: Dp = 14.dp,
    modifier: Modifier = Modifier
) {

    val cloudColor =
        SecondaryText

    val moonColor =
        PrimaryText

    val background =
        BackgroundBlack

    Canvas(
        modifier = modifier.size(size)
    ) {

        val s =
            this.size.minDimension

        when (kind) {

            WeatherIconKind.SUN ->
                drawSun(Offset(s * 0.5f, s * 0.5f), s * 0.2f, s)

            WeatherIconKind.MOON ->
                drawMoon(Offset(s * 0.5f, s * 0.5f), s * 0.32f, moonColor, background)

            WeatherIconKind.SUN_CLOUD -> {
                drawSun(Offset(s * 0.36f, s * 0.36f), s * 0.15f, s * 0.75f)
                drawCloud(s, cloudColor, background, top = 0.38f)
            }

            WeatherIconKind.MOON_CLOUD -> {
                drawMoon(Offset(s * 0.38f, s * 0.34f), s * 0.22f, moonColor, background)
                drawCloud(s, cloudColor, background, top = 0.38f)
            }

            WeatherIconKind.CLOUD ->
                drawCloud(s, cloudColor, background, top = 0.22f)

            WeatherIconKind.RAIN -> {
                drawCloud(s, cloudColor, background, top = 0.08f, scale = 0.86f)
                for (i in 0..2) {
                    val x = s * (0.30f + i * 0.2f)
                    drawLine(
                        color = RainColor,
                        start = Offset(x, s * 0.72f),
                        end = Offset(x - s * 0.07f, s * 0.94f),
                        strokeWidth = s * 0.08f,
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherIconKind.SNOW -> {
                drawCloud(s, cloudColor, background, top = 0.08f, scale = 0.86f)
                for (i in 0..2) {
                    drawCircle(
                        color = PrimaryText,
                        radius = s * 0.055f,
                        center = Offset(s * (0.28f + i * 0.22f), s * (if (i == 1) 0.9f else 0.8f))
                    )
                }
            }

            WeatherIconKind.STORM -> {
                drawCloud(s, cloudColor, background, top = 0.06f, scale = 0.86f)
                val bolt =
                    Path().apply {
                        moveTo(s * 0.56f, s * 0.6f)
                        lineTo(s * 0.4f, s * 0.8f)
                        lineTo(s * 0.52f, s * 0.8f)
                        lineTo(s * 0.44f, s * 0.99f)
                        lineTo(s * 0.66f, s * 0.74f)
                        lineTo(s * 0.54f, s * 0.74f)
                        close()
                    }
                drawPath(bolt, SunColor)
            }

            WeatherIconKind.FOG -> {
                listOf(0.32f, 0.52f, 0.72f).forEachIndexed { index, y ->
                    val inset = if (index == 1) 0.1f else 0.18f
                    drawLine(
                        color = cloudColor,
                        start = Offset(s * inset, s * y),
                        end = Offset(s * (1f - inset), s * y),
                        strokeWidth = s * 0.09f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSun(
    center: Offset,
    radius: Float,
    span: Float
) {
    drawCircle(
        color = SunColor,
        radius = radius,
        center = center
    )

    val inner = radius * 1.45f
    val outer = span * 0.46f

    for (i in 0 until 8) {
        val angle = Math.toRadians(i * 45.0)
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        drawLine(
            color = SunColor,
            start = Offset(center.x + dx * inner, center.y + dy * inner),
            end = Offset(center.x + dx * outer, center.y + dy * outer),
            strokeWidth = span * 0.075f,
            cap = StrokeCap.Round
        )
    }
}

/*
 * A crescent: a full disc with a background-coloured disc over one
 * side.
 */
private fun DrawScope.drawMoon(
    center: Offset,
    radius: Float,
    color: Color,
    background: Color
) {
    drawCircle(
        color = color,
        radius = radius,
        center = center
    )

    drawCircle(
        color = background,
        radius = radius * 0.86f,
        center = Offset(center.x + radius * 0.5f, center.y - radius * 0.32f)
    )
}

/*
 * A cloud across the lower part of the icon: a flat base and two
 * puffs. A thin background outline separates it from a sun or moon
 * peeking out behind.
 */
private fun DrawScope.drawCloud(
    s: Float,
    color: Color,
    background: Color,
    top: Float,
    scale: Float = 1f
) {

    val width = s * 0.86f * scale
    val left = (s - width) / 2f
    val baseTop = s * top + s * 0.26f * scale
    val baseHeight = s * 0.26f * scale

    val bigPuff = Offset(left + width * 0.58f, baseTop + s * 0.02f)
    val smallPuff = Offset(left + width * 0.32f, baseTop + s * 0.07f * scale)

    val outline = s * 0.05f

    listOf(
        background to outline,
        color to 0f
    ).forEach { (paint, grow) ->

        drawRoundRect(
            color = paint,
            topLeft = Offset(left - grow, baseTop - grow),
            size = Size(width + grow * 2, baseHeight + grow * 2),
            cornerRadius = CornerRadius(baseHeight / 2f + grow)
        )

        drawCircle(
            color = paint,
            radius = s * 0.22f * scale + grow,
            center = bigPuff
        )

        drawCircle(
            color = paint,
            radius = s * 0.15f * scale + grow,
            center = smallPuff
        )
    }
}
