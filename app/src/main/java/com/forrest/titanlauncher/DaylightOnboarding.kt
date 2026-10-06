package com.forrest.titanlauncher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/*
 * DAYLIGHT ONBOARDING KIT
 *
 * The pieces the onboarding steps are built from, drawn to the
 * D-Welcome / D-Access / D-Email / D-Tasks / D-Ready mockups.
 * Sizes follow the Daylight rule: mockup px x 0.82 = dp, px x 0.70 = sp.
 * Colours come from the theme globals, so light and dark both work.
 */

/*
 * "Ready" / "Connected" green. Not a theme global, so it is picked
 * from the page background: the mockup's deep green on light themes,
 * a lighter green that reads on dark ones.
 */
internal val DaylightSuccessGreen: Color
    get() =
        if (BackgroundBlack.luminance() < 0.5f) {
            Color(0xFF4CC79C)
        } else {
            Color(0xFF0F7A5C)
        }

/*
 * Text and icons drawn on top of the accent colour. White on the
 * light theme (the card colour), dark on dark themes where the
 * accent is lightened.
 */
internal val DaylightOnAccent: Color
    get() = SurfaceBlack

/*
 * "Step 2 of 8" on the left, starting right of the camera, and an
 * optional "Skip" (or a short note) on the right.
 */
@Composable
internal fun DaylightOnboardingHeader(
    stepLabel: String,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 18.dp)
                .padding(start = DaylightCameraLane),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = stepLabel,
            color = SecondaryText,
            fontSize = 8.5.sp,
            lineHeight = 11.sp,
            fontFamily = InterfaceFont
        )

        if (trailing != null) {
            Text(
                text = trailing,
                color = SecondaryText,
                fontSize = 8.5.sp,
                lineHeight = 11.sp,
                fontFamily = InterfaceFont,
                fontWeight =
                    if (onTrailingClick != null) FontWeight.Medium else FontWeight.Normal,
                modifier =
                    if (onTrailingClick != null) {
                        Modifier
                            .clickable { onTrailingClick() }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    } else {
                        Modifier
                    }
            )
        }
    }
}

/*
 * The big bold step title and the muted line under it. Any part of
 * the subtitle equal to [highlight] is drawn in the text colour, like
 * "+buy milk" on the tasks step.
 */
@Composable
internal fun DaylightOnboardingTitle(
    title: String,
    subtitle: String? = null,
    highlight: String? = null
) {

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {

        Text(
            text = title,
            color = PrimaryText,
            fontSize = 20.sp,
            lineHeight = 21.5.sp,
            letterSpacing = (-0.6).sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Bold
        )

        if (subtitle != null) {

            val annotated =
                buildAnnotatedString {
                    val start =
                        if (highlight.isNullOrEmpty()) -1 else subtitle.indexOf(highlight)

                    if (start < 0 || highlight == null) {
                        append(subtitle)
                    } else {
                        append(subtitle.substring(0, start))
                        withStyle(
                            SpanStyle(
                                color = PrimaryText,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(highlight)
                        }
                        append(subtitle.substring(start + highlight.length))
                    }
                }

            Text(
                text = annotated,
                color = SecondaryText,
                fontSize = 10.sp,
                lineHeight = 13.5.sp,
                fontFamily = InterfaceFont
            )
        }
    }
}

/*
 * The small muted line under a card ("The first one you pick is your
 * main app.").
 */
@Composable
internal fun DaylightOnboardingNote(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SecondaryText
) {

    Text(
        text = text,
        color = color,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontFamily = InterfaceFont,
        modifier = modifier
    )
}

/*
 * An accent-coloured text action ("App settings →").
 */
@Composable
internal fun DaylightOnboardingLink(
    text: String,
    onClick: () -> Unit
) {

    Text(
        text = text,
        color = AccentOrange,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontFamily = InterfaceFont,
        fontWeight = FontWeight.SemiBold,
        modifier =
            Modifier
                .clickable { onClick() }
                .padding(vertical = 3.dp)
    )
}

internal enum class DaylightPillKind {
    /* Filled accent: something to do ("Allow", "Connect"). */
    ACCENT,

    /* Quiet grey: optional, or waiting ("Optional", "Checking"). */
    NEUTRAL,

    /* Green tick and label, no fill ("Ready", "Connected"). */
    READY
}

/*
 * The right-hand control on a permission or service row.
 */
@Composable
internal fun DaylightStatusPill(
    label: String,
    kind: DaylightPillKind,
    onClick: (() -> Unit)? = null
) {

    if (kind == DaylightPillKind.READY) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            DaylightCheckIcon(
                color = DaylightSuccessGreen,
                size = 11.5.dp,
                strokeWidth = 2.6f
            )
            Text(
                text = label,
                color = DaylightSuccessGreen,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        return
    }

    val accent = kind == DaylightPillKind.ACCENT

    Text(
        text = label,
        color = if (accent) DaylightOnAccent else PrimaryText,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        fontFamily = InterfaceFont,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier =
            Modifier
                .background(
                    if (accent) AccentOrange else BackgroundBlack,
                    RoundedCornerShape(999.dp)
                )
                .then(
                    if (onClick != null) Modifier.clickable { onClick() } else Modifier
                )
                .padding(
                    horizontal = 11.5.dp,
                    vertical = 5.5.dp
                )
    )
}

/*
 * The tick from the mockups (24-unit path M5 12 l5 5 9-10).
 */
@Composable
internal fun DaylightCheckIcon(
    color: Color,
    size: Dp,
    strokeWidth: Float = 2.6f
) {

    Canvas(
        modifier = Modifier.size(size)
    ) {
        val unit = this.size.width / 24f
        val path =
            Path().apply {
                moveTo(5f * unit, 12f * unit)
                lineTo(10f * unit, 17f * unit)
                lineTo(19f * unit, 7f * unit)
            }
        drawPath(
            path = path,
            color = color,
            style =
                Stroke(
                    width = strokeWidth * unit,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
        )
    }
}

/*
 * The rounded checkbox on the email step: filled accent with a tick
 * when checked, an outlined square when not.
 */
@Composable
internal fun DaylightCheckbox(
    checked: Boolean
) {

    Box(
        modifier =
            Modifier
                .size(18.dp)
                .then(
                    if (checked) {
                        Modifier.background(
                            AccentOrange,
                            RoundedCornerShape(5.5.dp)
                        )
                    } else {
                        Modifier.border(
                            1.2.dp,
                            TertiaryText.copy(alpha = 0.55f),
                            RoundedCornerShape(5.5.dp)
                        )
                    }
                ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            DaylightCheckIcon(
                color = DaylightOnAccent,
                size = 10.5.dp,
                strokeWidth = 3f
            )
        }
    }
}

/*
 * The soft accent "Main" tag.
 */
@Composable
internal fun DaylightAccentTag(
    text: String
) {

    Text(
        text = text,
        color = AccentOrange,
        fontSize = 8.5.sp,
        lineHeight = 10.sp,
        fontFamily = InterfaceFont,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier =
            Modifier
                .background(
                    AccentOrange.copy(alpha = 0.16f),
                    RoundedCornerShape(999.dp)
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = 2.5.dp
                )
    )
}

/*
 * The welcome mark: 24 hour dots around an accent key with a chevron.
 * Mockup: 150 px square, dots 9 px on a 67 px radius, key 54 px.
 */
@Composable
internal fun DaylightWelcomeRing(
    modifier: Modifier = Modifier
) {

    val doneColor = PrimaryText
    val pendingColor = ProductivityGray
    val distractedColor = ProductivityRed
    val accent = AccentOrange
    val onAccent = DaylightOnAccent

    Box(
        modifier = modifier.size(123.dp),
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier.size(123.dp)
        ) {
            val unit = size.width / 150f

            for (index in 0 until 24) {
                val angle =
                    Math.toRadians(index * 15.0 - 90.0)

                val color =
                    when {
                        index == 17 -> accent
                        index == 9 || index == 14 -> distractedColor
                        index < 18 -> doneColor
                        else -> pendingColor
                    }

                drawCircle(
                    color = color,
                    radius = 4.5f * unit,
                    center =
                        Offset(
                            x = (75f + 67f * cos(angle).toFloat()) * unit,
                            y = (75f + 67f * sin(angle).toFloat()) * unit
                        )
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .background(
                        accent,
                        RoundedCornerShape(15.dp)
                    ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(21.dp)
            ) {
                val unit = size.width / 24f
                val path =
                    Path().apply {
                        moveTo(8f * unit, 6f * unit)
                        lineTo(14f * unit, 12f * unit)
                        lineTo(8f * unit, 18f * unit)
                    }
                drawPath(
                    path = path,
                    color = onAccent,
                    style =
                        Stroke(
                            width = 2.6f * unit,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                )
            }
        }
    }
}

internal enum class DaylightTileIcon {
    /* A rounded square with a tick: Google Tasks. */
    CHECK_SQUARE,

    /* Three lines: Todoist. */
    LINES,

    /* A question mark: the assistant. */
    QUESTION,

    /* A house: default home. */
    HOME
}

/*
 * The 40 px rounded tile at the start of a service card.
 */
@Composable
internal fun DaylightServiceTile(
    icon: DaylightTileIcon,
    tint: Color
) {

    Box(
        modifier =
            Modifier
                .size(33.dp)
                .background(
                    tint.copy(alpha = 0.16f),
                    RoundedCornerShape(10.dp)
                ),
        contentAlignment = Alignment.Center
    ) {

        if (icon == DaylightTileIcon.QUESTION) {
            Text(
                text = "?",
                color = tint,
                fontSize = 13.sp,
                lineHeight = 14.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.Bold
            )
        } else {

        Canvas(
            modifier = Modifier.size(16.5.dp)
        ) {
            val unit = size.width / 24f
            val stroke =
                Stroke(
                    width = 2f * unit,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

            when (icon) {
                DaylightTileIcon.CHECK_SQUARE -> {
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(3f * unit, 3f * unit),
                        size = Size(18f * unit, 18f * unit),
                        cornerRadius = CornerRadius(2f * unit, 2f * unit),
                        style = stroke
                    )
                    drawPath(
                        path =
                            Path().apply {
                                moveTo(8f * unit, 12f * unit)
                                lineTo(11f * unit, 15f * unit)
                                lineTo(16f * unit, 9f * unit)
                            },
                        color = tint,
                        style = stroke
                    )
                }

                DaylightTileIcon.LINES -> {
                    drawPath(
                        path =
                            Path().apply {
                                moveTo(4f * unit, 7f * unit)
                                lineTo(20f * unit, 7f * unit)
                                moveTo(4f * unit, 12f * unit)
                                lineTo(20f * unit, 12f * unit)
                                moveTo(4f * unit, 17f * unit)
                                lineTo(14f * unit, 17f * unit)
                            },
                        color = tint,
                        style = stroke
                    )
                }

                DaylightTileIcon.HOME -> {
                    drawPath(
                        path =
                            Path().apply {
                                moveTo(4f * unit, 11f * unit)
                                lineTo(12f * unit, 4f * unit)
                                lineTo(20f * unit, 11f * unit)
                                moveTo(6f * unit, 10f * unit)
                                lineTo(6f * unit, 20f * unit)
                                lineTo(18f * unit, 20f * unit)
                                lineTo(18f * unit, 10f * unit)
                            },
                        color = tint,
                        style = stroke
                    )
                }

                DaylightTileIcon.QUESTION -> Unit
            }
        }
        }
    }
}

/*
 * The header row of a service card: tile, title and detail, and a
 * status pill or anything else on the right.
 */
@Composable
internal fun DaylightServiceHeader(
    icon: DaylightTileIcon,
    tint: Color,
    title: String,
    detail: String?,
    trailing: @Composable () -> Unit = {}
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        DaylightServiceTile(
            icon = icon,
            tint = tint
        )

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
                    fontSize = 8.5.sp,
                    lineHeight = 11.sp,
                    fontFamily = InterfaceFont
                )
            }
        }

        trailing()
    }
}

/*
 * A secret field on the page-grey well inside a card, with an
 * optional dark "Save" button at the end (D-Tasks).
 */
@Composable
internal fun DaylightSecretField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 34.dp)
                .background(
                    BackgroundBlack,
                    RoundedCornerShape(10.dp)
                )
                .padding(
                    start = 10.dp,
                    end = 6.dp,
                    top = 5.dp,
                    bottom = 5.dp
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        BasicTextField(
            value = value,
            onValueChange = {
                val cleaned =
                    it.replace("\n", "")

                if (cleaned.length <= 512) {
                    onValueChange(cleaned)
                }
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            textStyle =
                TextStyle(
                    color = PrimaryText,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontFamily = InterfaceFont
                ),
            cursorBrush = SolidColor(AccentOrange),
            modifier =
                Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = TertiaryText,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            fontFamily = InterfaceFont,
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
            }
        )

        if (actionLabel != null && onAction != null) {

            val enabled = value.isNotBlank()

            Text(
                text = actionLabel,
                color = SurfaceBlack,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier =
                    Modifier
                        .background(
                            if (enabled) PrimaryText else PrimaryText.copy(alpha = 0.35f),
                            RoundedCornerShape(6.5.dp)
                        )
                        .clickable(enabled = enabled) { onAction() }
                        .padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        )
            )
        }
    }
}

/*
 * One cell of the "You're set" grid: an accent square holding the
 * command character, and what it does.
 */
@Composable
internal fun DaylightCommandCard(
    symbol: String,
    label: String,
    modifier: Modifier = Modifier,
    neutral: Boolean = false
) {

    Row(
        modifier =
            modifier
                .background(
                    SurfaceBlack,
                    RoundedCornerShape(11.5.dp)
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Box(
            modifier =
                Modifier
                    .size(24.5.dp)
                    .background(
                        if (neutral) PrimaryText else AccentOrange,
                        RoundedCornerShape(7.5.dp)
                    ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                color = if (neutral) SurfaceBlack else DaylightOnAccent,
                fontSize = if (symbol.length > 1) 9.sp else 11.sp,
                lineHeight = 12.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        Text(
            text = label,
            color = PrimaryText,
            fontSize = 9.8.sp,
            lineHeight = 12.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

/*
 * The quiet "Back" button that sits left of the primary action. On the
 * card colour, since the kit's secondary button is page-coloured and
 * would vanish against the onboarding background.
 */
@Composable
internal fun DaylightBackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Box(
        modifier =
            modifier
                .widthIn(min = 72.dp)
                .heightIn(min = 40.dp)
                .background(
                    SurfaceBlack,
                    RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Back",
            color = PrimaryText,
            fontSize = 11.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/*
 * The bottom of every step: an optional Back, then the big accent
 * primary action.
 */
@Composable
internal fun DaylightOnboardingActions(
    primaryLabel: String,
    onPrimary: () -> Unit,
    onBack: (() -> Unit)? = null
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        if (onBack != null) {
            DaylightBackButton(
                onClick = onBack
            )
        }

        DaylightPrimaryButton(
            label = primaryLabel,
            modifier = Modifier.weight(1f),
            onClick = onPrimary
        )
    }
}
