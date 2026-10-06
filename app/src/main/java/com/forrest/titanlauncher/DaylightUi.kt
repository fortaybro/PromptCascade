package com.forrest.titanlauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * DAYLIGHT UI KIT
 *
 * The shared building blocks every Daylight screen is made from, so
 * home, hub, quick reply, settings, onboarding and email all look like
 * one app. Colours always come from the theme globals (BackgroundBlack,
 * SurfaceBlack, BorderGray, PrimaryText, SecondaryText, AccentOrange),
 * so light, dark and auto all work without per-screen code.
 *
 * Sizing: the mockups are drawn 440 px wide; TitanUiScale renders a
 * 360 dp wide canvas, and the app's text is scaled up a further 1.16x.
 * So mockup px x 0.82 = dp, and mockup px x 0.70 = sp.
 *
 * Camera: the Titan 2 Elite's camera is in the top-left corner.
 * A screen's first row starts DaylightCameraLane in from the left.
 */

internal val DaylightCameraLane: Dp =
    CameraSafeStartPadding

/*
 * Page padding used by every Daylight screen (16 px / 20 px in the
 * mockups).
 */
internal val DaylightPageHorizontal: Dp = 16.dp
internal val DaylightPageVertical: Dp = 12.dp

/*
 * A white (dark grey in dark mode) rounded card.
 */
@Composable
internal fun DaylightCard(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    SurfaceBlack,
                    RoundedCornerShape(14.dp)
                )
                .padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding
                ),
        content = content
    )
}

/*
 * The thin line between rows inside a card.
 */
@Composable
internal fun DaylightRowDivider() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(0.75.dp)
                .background(BorderGray.copy(alpha = 0.7f))
    )
}

/*
 * "NOW", "EARLIER", "HOME ELEMENTS": small spaced capitals.
 */
@Composable
internal fun DaylightSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SecondaryText
) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = 8.sp,
        lineHeight = 10.sp,
        letterSpacing = 1.sp,
        fontFamily = InterfaceFont,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

/*
 * A page title: an optional small caption over a large bold title,
 * starting right of the camera.
 */
@Composable
internal fun DaylightScreenTitle(
    title: String,
    caption: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {

    Column(
        modifier =
            modifier.padding(
                start = DaylightCameraLane
            )
    ) {

        if (caption != null) {
            Text(
                text = caption,
                color = SecondaryText,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontFamily = InterfaceFont
            )
        }

        Text(
            text = title,
            color = PrimaryText,
            fontSize = 22.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.5).sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                color = SecondaryText,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontFamily = InterfaceFont
            )
        }
    }
}

/*
 * The big accent button ("Continue", "Open in Spark", "Send").
 */
@Composable
internal fun DaylightPrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .background(
                    if (enabled) AccentOrange else AccentOrange.copy(alpha = 0.4f),
                    RoundedCornerShape(12.dp)
                )
                .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = BackgroundBlack,
            fontSize = 11.5.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

/*
 * The quiet grey button next to a primary one ("Reply", "Mark read").
 */
@Composable
internal fun DaylightSecondaryButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Text(
        text = label,
        color = PrimaryText,
        fontSize = 10.5.sp,
        fontFamily = InterfaceFont,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier =
            modifier
                .background(
                    BackgroundBlack,
                    RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .padding(vertical = 10.dp)
    )
}

/*
 * A rounded chip; filled in the accent colour when selected.
 */
@Composable
internal fun DaylightChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Text(
        text = label,
        color = if (selected) BackgroundBlack else PrimaryText,
        fontSize = 10.sp,
        lineHeight = 11.sp,
        fontFamily = InterfaceFont,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        modifier =
            Modifier
                .background(
                    if (selected) AccentOrange else SurfaceBlack,
                    RoundedCornerShape(999.dp)
                )
                .border(
                    0.75.dp,
                    if (selected) AccentOrange else BorderGray,
                    RoundedCornerShape(999.dp)
                )
                .clickable { onClick() }
                .padding(
                    horizontal = 11.dp,
                    vertical = 5.dp
                )
    )
}

/*
 * The pill on/off switch used in settings and onboarding.
 */
@Composable
internal fun DaylightSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Box(
        modifier =
            Modifier
                .width(38.dp)
                .height(23.dp)
                .background(
                    if (checked) AccentOrange else BorderGray,
                    RoundedCornerShape(999.dp)
                )
                .clickable { onCheckedChange(!checked) }
                .padding(2.5.dp),
        contentAlignment =
            if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier =
                Modifier
                    .size(18.dp)
                    .background(
                        SurfaceBlack,
                        CircleShape
                    )
        )
    }
}

/*
 * A settings-style row: title, optional detail line, and whatever
 * control sits on the right.
 */
@Composable
internal fun DaylightListRow(
    title: String,
    detail: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .then(
                    if (onClick != null) Modifier.clickable { onClick() } else Modifier
                )
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = PrimaryText,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.SemiBold
            )
            if (detail != null) {
                Text(
                    text = detail,
                    color = SecondaryText,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontFamily = InterfaceFont
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        trailing()
    }
}

/*
 * Light / Dark / Auto style segmented control.
 */
@Composable
internal fun DaylightSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {

    Row(
        modifier =
            Modifier
                .background(
                    BackgroundBlack,
                    RoundedCornerShape(9.dp)
                )
                .padding(2.5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Text(
                text = option,
                color = if (selected) PrimaryText else SecondaryText,
                fontSize = 9.5.sp,
                fontFamily = InterfaceFont,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier =
                    Modifier
                        .background(
                            if (selected) SurfaceBlack else Color.Transparent,
                            RoundedCornerShape(7.dp)
                        )
                        .clickable { onSelect(index) }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }
    }
}
