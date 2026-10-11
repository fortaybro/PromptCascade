package com.forrest.titanlauncher

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * DAYLIGHT HOME KIT
 *
 * The pieces of the home screen (mockup C-Home): the quick-reply pill
 * and small icons in the header, the one white card of info rows, the
 * 2 x 4 grid of app tiles, and the confirmation shown after sending.
 *
 * Sizes follow the mockup: px x 0.82 = dp, px x 0.70 = sp.
 */

/*
 * Header button size. 32 dp (was 24) so they're easy to hit on the
 * phone; icons and text grow with them.
 */
private val HeaderButtonSize = 32.dp
private val HeaderIconSize = 16.dp

/*
 * The header's main button: bolt + number of replies waiting.
 */
@Composable
internal fun DaylightQuickReplyPill(
    waiting: Int,
    onClick: () -> Unit
) {

    /*
     * A missed call turns the bolt fire-coloured and pulsing until it
     * is dealt with in quick reply (see MissedCalls.kt).
     */
    val missed =
        MissedCallsWaiting.size

    val total =
        waiting + missed

    val pulse =
        androidx.compose.animation.core.rememberInfiniteTransition(
            label = "missedCallPulse"
        )

    val pulseAlpha by
    pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec =
            androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(700),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
        label = "missedCallPulseAlpha"
    )

    val tint =
        when {
            missed > 0 -> MissedCallFire
            waiting > 0 -> AccentOrange
            else -> PrimaryText
        }

    Row(
        modifier =
            Modifier
                .height(HeaderButtonSize)
                .background(
                    if (missed > 0) MissedCallFire.copy(alpha = 0.16f * pulseAlpha) else SurfaceBlack,
                    RoundedCornerShape(999.dp)
                )
                .border(
                    if (missed > 0) 1.2.dp else 0.8.dp,
                    if (missed > 0) MissedCallFire.copy(alpha = pulseAlpha) else BorderGray,
                    RoundedCornerShape(999.dp)
                )
                .clip(RoundedCornerShape(999.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        Icon(
            imageVector = Icons.Outlined.Bolt,
            contentDescription =
                when {
                    missed > 0 -> "quick reply, missed call"
                    waiting > 0 -> "quick reply, $waiting waiting"
                    else -> "quick reply"
                },
            tint =
                if (missed > 0) tint.copy(alpha = pulseAlpha) else tint,
            modifier = Modifier.size(HeaderIconSize)
        )

        if (
            total > 0
        ) {
            Text(
                text = total.toString(),
                color = tint,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/*
 * The quieter header buttons (quick toggles, favorites), sized to sit
 * beside the pill.
 */
@Composable
internal fun DaylightHeaderIcon(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = SecondaryText,
    onClick: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .size(HeaderButtonSize)
                .background(
                    SurfaceBlack,
                    CircleShape
                )
                .border(
                    0.8.dp,
                    BorderGray,
                    CircleShape
                )
                .clip(CircleShape)
                .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(HeaderIconSize)
        )
    }
}

/*
 * Battery level in the header. Accent while charging or in battery
 * saver, like HeaderBatteryButton.
 */
@Composable
internal fun DaylightHeaderBattery(
    snapshot: BatterySnapshot,
    onClick: () -> Unit
) {

    val highlighted =
        snapshot.charging ||
                snapshot.powerSave

    Box(
        modifier =
            Modifier
                .height(HeaderButtonSize)
                .widthIn(min = HeaderButtonSize)
                .background(
                    SurfaceBlack,
                    RoundedCornerShape(999.dp)
                )
                .border(
                    0.8.dp,
                    if (highlighted) AccentOrange else BorderGray,
                    RoundedCornerShape(999.dp)
                )
                .clip(RoundedCornerShape(999.dp))
                .clickable { onClick() }
                .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = snapshot.level.toString(),
            color = if (highlighted) AccentOrange else SecondaryText,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

/*
 * One row of the home info card: a dot, a bold lead, a muted detail
 * and a small right-aligned meta ("2 texts", "14m", "next").
 */
internal data class DaylightHomeInfoRow(
    val lead: String,
    val detail: String,
    val meta: String,
    val filledDot: Boolean,
    val onClick: () -> Unit,
    /*
     * Drawn as a solid accent pill so it stands out from the other
     * rows (new texts).
     */
    val highlighted: Boolean = false
)

@Composable
internal fun DaylightHomeInfoCard(
    rows: List<DaylightHomeInfoRow>,
    modifier: Modifier = Modifier
) {

    if (
        rows.isEmpty()
    ) {
        return
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    HomeInfoCardSurface,
                    RoundedCornerShape(15.dp)
                )
                .padding(horizontal = 13.dp)
    ) {

        rows.forEachIndexed { index, row ->

            if (
                index > 0
            ) {
                DaylightRowDivider()
            }

            val leadColor =
                if (row.highlighted) BackgroundBlack else PrimaryText

            val detailColor =
                if (row.highlighted) BackgroundBlack.copy(alpha = 0.78f) else SecondaryText

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (row.highlighted) {
                                Modifier
                                    .padding(vertical = 5.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(AccentOrange)
                                    .clickable { row.onClick() }
                                    .padding(horizontal = 9.dp, vertical = 7.dp)
                            } else {
                                Modifier
                                    .clickable { row.onClick() }
                                    .padding(vertical = 8.dp)
                            }
                        ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(6.5.dp)
                            .then(
                                if (row.highlighted) {
                                    Modifier.background(
                                        BackgroundBlack,
                                        CircleShape
                                    )
                                } else if (row.filledDot) {
                                    Modifier.background(
                                        AccentOrange,
                                        CircleShape
                                    )
                                } else {
                                    Modifier.border(
                                        1.2.dp,
                                        PrimaryText,
                                        CircleShape
                                    )
                                }
                            )
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text =
                        buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = leadColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) {
                                append(row.lead)
                            }
                            if (row.detail.isNotBlank()) {
                                append(" ")
                                withStyle(
                                    SpanStyle(
                                        color = detailColor
                                    )
                                ) {
                                    append(row.detail)
                                }
                            }
                        },
                    color = PrimaryText,
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp,
                    fontFamily = InterfaceFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (
                    row.meta.isNotBlank()
                ) {
                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = row.meta,
                        color = detailColor,
                        fontWeight =
                            if (row.highlighted) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 8.5.sp,
                        lineHeight = 10.sp,
                        fontFamily = InterfaceFont,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/*
 * The app tiles: rows of four equal-width rounded tiles, icon over
 * label (60 px tall in the mockup).
 */
@Composable
internal fun DaylightHomeTileGrid(
    rows: List<List<AppButtonData>>,
    modifier: Modifier = Modifier
) {

    Column(
        modifier =
            modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(6.5.dp)
    ) {

        rows.forEach { buttons ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.5.dp)
            ) {

                buttons.forEach { button ->

                    DaylightHomeTile(
                        button = button,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DaylightHomeTile(
    button: AppButtonData,
    modifier: Modifier = Modifier
) {

    /*
     * The settings for icon size, label size and corners still apply,
     * scaled so the defaults land on the mockup's 18 dp icon, 9 sp
     * label and 13 dp corners.
     */
    val corner =
        (HomeButtonCornerRadiusDp * 0.75f).dp

    Column(
        modifier =
            modifier
                .height(49.dp)
                .background(
                    HomeButtonSurface,
                    RoundedCornerShape(corner)
                )
                .clickable { button.onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = button.icon,
            contentDescription = button.label,
            tint = PrimaryText,
            modifier =
                Modifier.size(
                    (HomeButtonIconSizeDp * 0.82f).dp
                )
        )

        Spacer(
            modifier = Modifier.height(3.dp)
        )

        Text(
            text = button.label,
            color = SecondaryText,
            fontSize = (HomeButtonLabelSizeSp * 0.86f).sp,
            lineHeight = (HomeButtonLabelSizeSp * 1.05f).sp,
            fontFamily = InterfaceFont,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/*
 * "5m", "3h", "2d" since a timestamp; "now" under a minute.
 */
internal fun daylightAgo(
    timestamp: Long,
    now: Long = System.currentTimeMillis()
): String {

    if (
        timestamp <= 0L
    ) {
        return ""
    }

    val minutes =
        ((now - timestamp) / 60_000L)
            .coerceAtLeast(0L)

    return when {
        minutes < 1L -> "now"
        minutes < 60L -> "${minutes}m"
        minutes < 60L * 24L -> "${minutes / 60L}h"
        else -> "${minutes / (60L * 24L)}d"
    }
}

/*
 * The confirmation above the prompt.
 *
 * A success ("✓ SENT · OPENING MESSAGES") is a centred white card with
 * an accent check circle, a title ("Sent") and a muted line
 * ("Opening messages"), as in mockup E-Sent. Anything else is a
 * simple pill.
 */
@Composable
internal fun DaylightStatusConfirmation(
    statusText: String,
    modifier: Modifier = Modifier
) {

    val trimmed =
        statusText.trim()

    Box(
        modifier =
            modifier.fillMaxWidth(),
        contentAlignment =
            Alignment.Center
    ) {

        if (
            trimmed.startsWith("✓")
        ) {

            val parts =
                trimmed
                    .removePrefix("✓")
                    .trim()
                    .split(" · ")
                    .map { daylightSentenceCase(it.trim()) }
                    .filter { it.isNotBlank() }

            Column(
                modifier =
                    Modifier
                        .widthIn(max = 246.dp)
                        .fillMaxWidth()
                        .background(
                            SurfaceBlack,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(
                            horizontal = 14.dp,
                            vertical = 13.dp
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .background(
                                AccentOrange,
                                CircleShape
                            ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = BackgroundBlack,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = parts.firstOrNull() ?: "Done",
                    color = PrimaryText,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    letterSpacing = (-0.3).sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (
                    parts.size > 1
                ) {
                    Text(
                        text = parts.drop(1).joinToString(" · "),
                        color = SecondaryText,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontFamily = InterfaceFont,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

        } else {

            Text(
                text = daylightSentenceCase(trimmed),
                color = PrimaryText,
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .background(
                            SurfaceBlack,
                            RoundedCornerShape(999.dp)
                        )
                        .border(
                            0.75.dp,
                            BorderGray,
                            RoundedCornerShape(999.dp)
                        )
                        .padding(
                            horizontal = 13.dp,
                            vertical = 6.dp
                        )
            )
        }
    }
}

/*
 * Status texts are written in capitals ("COULD NOT OPEN SETTINGS");
 * Daylight shows them in sentence case. Mixed-case text is left as is.
 */
internal fun daylightSentenceCase(
    text: String
): String {

    if (
        text.isBlank() ||
        text.any { it.isLowerCase() }
    ) {
        return text
    }

    return text
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}
