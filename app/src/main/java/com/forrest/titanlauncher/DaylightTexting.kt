package com.forrest.titanlauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forrest.titanlauncher.contacts.Contact

/*
 * DAYLIGHT TEXTING
 *
 * The pieces the prompt shows while texting with "@name" and "@@":
 * contact rows with initials, the "message to" card once a contact
 * is locked in, and chips for the people in a group.
 */

/*
 * "Sarah Miller" -> "SM", "Mom" -> "M".
 */
internal fun contactInitials(
    name: String
): String {

    val words =
        name
            .trim()
            .split(" ")
            .filter {
                it.isNotBlank() &&
                        it.first().isLetterOrDigit()
            }

    return when {
        words.isEmpty() -> "#"
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words[0].take(1) + words.last().take(1)).uppercase()
    }
}

/*
 * Initials in a circle (36 px in the mockups).
 *
 *  - filled: the highlighted row or the locked contact, accent with
 *    light initials
 *  - neutral: a plain grey circle, for everyone else
 *  - otherwise: a soft accent tint (another number for the same
 *    person as the highlighted row)
 */
@Composable
internal fun DaylightAvatar(
    label: String,
    filled: Boolean,
    sizeDp: Int = 28,
    neutral: Boolean = false
) {

    Box(
        modifier =
            Modifier
                .size(
                    sizeDp.dp
                )
                .background(
                    when {
                        filled -> AccentOrange
                        neutral -> BorderGray
                        else -> AccentOrange.copy(alpha = 0.16f)
                    },
                    CircleShape
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = label,
            color =
                when {
                    filled -> BackgroundBlack
                    neutral -> PrimaryText
                    else -> AccentOrange
                },
            fontSize = if (sizeDp >= 30) 10.sp else 9.sp,
            lineHeight = 11.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/*
 * "MESSAGE TO", "GROUP TEXT · 2 PEOPLE", "NEW EMAIL TO": the accent
 * small capitals at the top of a prompt card (11 px in the mockups).
 */
@Composable
internal fun DaylightPromptLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        color = AccentOrange,
        fontSize = 7.7.sp,
        lineHeight = 9.5.sp,
        letterSpacing = 0.9.sp,
        fontFamily = InterfaceFont,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/*
 * The muted, centred line between a prompt card and the prompt
 * ("Tap a name to lock it in, or keep typing").
 */
@Composable
internal fun DaylightPromptHint(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = SecondaryText,
        fontSize = 8.5.sp,
        lineHeight = 10.5.sp,
        fontFamily = InterfaceFont,
        textAlign = TextAlign.Center,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp
                )
    )
}

/*
 * One suggested contact. The part of the name already typed is drawn
 * in the accent colour; the highlighted row is the one Tab or Enter
 * picks.
 */
@Composable
internal fun DaylightContactRow(
    contact: Contact,
    selected: Boolean,
    query: String,
    onClick: () -> Unit,
    trailing: String? = null,
    /*
     * Another number or address for the person on the highlighted
     * row: drawn with a tinted avatar instead of a grey one.
     */
    sameAsSelected: Boolean = false,
    showDetail: Boolean = true,
    avatarSizeDp: Int = 30,
    /*
     * The soft accent row background. Follows selected unless a
     * caller wants the highlight without the filled avatar (group
     * suggestions).
     */
    highlighted: Boolean = selected
) {

    val name =
        contact.name

    val matchLength =
        if (
            query.isNotBlank() &&
            name.startsWith(query.trim(), ignoreCase = true)
        ) {
            query.trim().length
        } else {
            0
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(
                    min = 36.dp
                )
                .background(
                    if (highlighted) AccentOrange.copy(alpha = 0.08f) else Color.Transparent,
                    RoundedCornerShape(
                        10.dp
                    )
                )
                .clickable {
                    onClick()
                }
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        DaylightAvatar(
            label = contactInitials(name),
            filled = selected,
            sizeDp = avatarSizeDp,
            neutral = !selected && !sameAsSelected
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text =
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(color = AccentOrange)
                        ) {
                            append(name.take(matchLength))
                        }
                        append(name.drop(matchLength))
                    },
                color = PrimaryText,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (
                showDetail &&
                contact.phoneNumber.isNotBlank()
            ) {
                Text(
                    text = contact.phoneNumber,
                    color = SecondaryText,
                    fontSize = 8.5.sp,
                    lineHeight = 10.5.sp,
                    fontFamily = InterfaceFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val hint =
            trailing
                ?: if (selected) "Tab" else null

        if (
            hint != null
        ) {
            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = hint.replaceFirstChar { it.uppercase() },
                color = AccentOrange,
                fontSize = 8.5.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/*
 * Shown once a contact is locked in: who the message goes to, with a
 * button that unlocks it. Drawn inside the prompt's white card; the
 * Enter / Backspace hint sits under the card (DaylightPromptHint).
 */
@Composable
internal fun DaylightLockedContactCard(
    contact: Contact,
    onUnlock: (() -> Unit)? = null
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        DaylightAvatar(
            label = contactInitials(contact.name),
            filled = true,
            sizeDp = 30
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            DaylightPromptLabel(
                text = "Message to"
            )

            Text(
                text =
                    if (contact.phoneNumber.isBlank()) {
                        contact.name
                    } else {
                        contact.name + " · " + contact.phoneNumber
                    },
                color = PrimaryText,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (
            onUnlock != null
        ) {
            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Box(
                modifier =
                    Modifier
                        .size(26.dp)
                        .background(
                            BackgroundBlack,
                            CircleShape
                        )
                        .clickable {
                            onUnlock()
                        },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "unlock contact",
                    tint = PrimaryText,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/*
 * The people already in a group text, as accent chips that scroll
 * sideways when the group gets long. Drawn inside the prompt card.
 */
@Composable
internal fun DaylightGroupChips(
    names: List<String>
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 4.dp,
                    vertical = 2.dp
                )
    ) {

        DaylightPromptLabel(
            text = "Group text · " + names.size + if (names.size == 1) " person" else " people"
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Row(
            modifier =
                Modifier.horizontalScroll(
                    rememberScrollState()
                ),
            horizontalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {
            names.forEach { name ->
                Text(
                    text = name,
                    color = BackgroundBlack,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .background(
                                AccentOrange,
                                RoundedCornerShape(999.dp)
                            )
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                )
            }
        }
    }
}

/*
 * The hint under the group card, once there is someone to text.
 */
internal fun daylightGroupHint(
    count: Int
): String? =
    if (count >= 2) {
        "Enter opens the group in Messages, or type a message to send to everyone"
    } else {
        null
    }
