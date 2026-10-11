package com.forrest.titanlauncher

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forrest.titanlauncher.mail.GmailPackage
import com.forrest.titanlauncher.notifications.NotificationCenter

/*
 * DAYLIGHT EMAIL
 *
 * Email in Daylight works the way texting does: Daylight shows what
 * is new, and your own mail app opens it. These are the pieces for
 * that: the "write in" chips under the address list, and the preview
 * card shown when an email is tapped in the hub.
 */

/*
 * The mail app the "write in" chips offer: the one chosen in
 * settings, otherwise the first email app picked in onboarding that
 * is not Gmail (Gmail is what Daylight itself sends through).
 */
internal fun writeInMailApp(): String? =
    MailComposeAppPackage
        .ifBlank {
            EnabledMailApps
                .firstOrNull {
                    it != GmailPackage
                }
                .orEmpty()
        }
        .takeIf {
            it.isNotBlank()
        }

internal fun mailAppLabel(
    context: Context,
    packageName: String
): String =
    runCatching {
        val packageManager =
            context.packageManager

        packageManager
            .getApplicationLabel(
                packageManager.getApplicationInfo(
                    packageName,
                    0
                )
            )
            .toString()
    }
        .getOrDefault(
            "mail app"
        )

/*
 * Left/right on the address list: swap between writing here and in
 * the mail app, for this one email.
 */
internal fun toggleMailWriteTarget() {

    val app =
        writeInMailApp()
            ?: return

    val current =
        MailWriteInOnce
            ?: MailComposeAppPackage

    MailWriteInOnce =
        if (current.isBlank()) app else ""
}

@Composable
internal fun DaylightWriteInChips() {

    val context =
        LocalContext.current

    /*
     * Each address list starts from the setting, not from whatever
     * was picked the last time.
     */
    LaunchedEffect(Unit) {
        MailWriteInOnce =
            null
    }

    val app =
        writeInMailApp()

    val target =
        MailWriteInOnce
            ?: MailComposeAppPackage

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 4.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp
            )
    ) {

        Text(
            text = "write in",
            color = SecondaryText,
            fontSize = 10.sp,
            fontFamily = InterfaceFont
        )

        DaylightChoiceChip(
            label = "Prompt Cascade",
            selected = target.isBlank(),
            onClick = {
                MailWriteInOnce =
                    ""
            }
        )

        if (
            app != null
        ) {
            DaylightChoiceChip(
                label =
                    mailAppLabel(
                        context,
                        app
                    ),
                selected = target.isNotBlank(),
                onClick = {
                    MailWriteInOnce =
                        app
                }
            )
        }
    }
}

/*
 * The card shown when an email from another mail app is tapped in
 * the hub. It shows what the notification carries (sender, subject,
 * preview) and hands the full email to its own app. Reply works for
 * apps whose notifications have a Reply button.
 */
@Composable
internal fun DaylightMailPreview(
    item: HubDisplayItem,
    onDismiss: () -> Unit
) {

    val context =
        LocalContext.current

    val key =
        item.notificationKey

    val notification =
        remember(key) {
            NotificationCenter
                .notifications
                .value
                .firstOrNull {
                    it.key == key
                }
        }

    val appName =
        notification
            ?.appName
            ?.ifBlank { null }
            ?: "its app"

    var replying by remember {
        mutableStateOf(false)
    }

    var replyText by remember {
        mutableStateOf("")
    }

    var notice by remember {
        mutableStateOf("")
    }

    val cardFocus =
        remember {
            FocusRequester()
        }

    val replyFocus =
        remember {
            FocusRequester()
        }

    LaunchedEffect(replying) {
        if (replying) {
            replyFocus.requestFocus()
        } else {
            cardFocus.requestFocus()
        }
    }

    fun openInApp() {
        if (key != null) {
            NotificationCenter.openNotification(
                key = key,
                context = context
            )
        }
        onDismiss()
    }

    fun clear() {
        if (key != null) {
            NotificationCenter.dismissNotification(
                key
            )
        }
        onDismiss()
    }

    fun sendReply() {

        val message =
            replyText.trim()

        if (
            key == null ||
            message.isBlank()
        ) {
            return
        }

        val sent =
            NotificationCenter.sendReply(
                context = context,
                key = key,
                message = message
            )

        if (sent) {
            NotificationCenter.dismissNotification(
                key
            )
            onDismiss()
        } else {
            notice =
                "could not send. open it in $appName instead"
        }
    }

    val (subject, previewBody) =
        splitMailNotificationText(
            notification
                ?.text
                ?.ifBlank { null }
                ?: item.preview.removePrefix("$appName · ")
        )

    BackHandler {
        if (replying) {
            replying = false
        } else {
            onDismiss()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = 0.16f
                    )
                )
                .clickable(
                    interactionSource =
                        remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss()
                }
                .padding(
                    horizontal = DaylightPageHorizontal,
                    vertical = DaylightPageVertical
                ),
        contentAlignment =
            Alignment.BottomCenter
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .background(
                        SurfaceBlack,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable(
                        interactionSource =
                            remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Taps inside the card stay inside it.
                    }
                    .focusRequester(cardFocus)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            false
                        } else when (event.key) {
                            Key.Escape -> {
                                if (replying) replying = false else onDismiss()
                                true
                            }
                            Key.Enter -> {
                                if (replying) sendReply() else openInApp()
                                true
                            }
                            else -> false
                        }
                    }
                    .padding(15.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            /*
             * Sender, app and age, with a round close button.
             */
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                DaylightAvatar(
                    label = contactInitials(item.title),
                    filled = true,
                    sizeDp = 33
                )

                Spacer(Modifier.width(8.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = PrimaryText,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = appName + " · " + hubRelativeTime(item.timestamp, long = true),
                        color = SecondaryText,
                        fontSize = 8.5.sp,
                        lineHeight = 10.sp,
                        fontFamily = InterfaceFont,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.width(8.dp))

                MailRoundIconButton(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "close",
                    background = BackgroundBlack,
                    onClick = onDismiss
                )
            }

            /*
             * Subject, the preview text, and where the rest lives.
             */
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                if (subject.isNotBlank()) {
                    Text(
                        text = subject,
                        color = PrimaryText,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        letterSpacing = (-0.3).sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (previewBody.isNotBlank()) {
                    Text(
                        text = previewBody,
                        color = PrimaryText,
                        fontSize = 10.sp,
                        lineHeight = 14.5.sp,
                        fontFamily = InterfaceFont,
                        maxLines = 7,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "Preview from the notification · the full email is in $appName",
                    color = SecondaryText,
                    fontSize = 8.5.sp,
                    lineHeight = 10.5.sp,
                    fontFamily = InterfaceFont
                )

                if (notice.isNotBlank()) {
                    Text(
                        text = notice,
                        color = ProductivityRed,
                        fontSize = 8.5.sp,
                        lineHeight = 10.5.sp,
                        fontFamily = InterfaceFont
                    )
                }
            }

            if (replying) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 39.dp)
                            .border(
                                1.5.dp,
                                AccentOrange,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(
                                start = 12.dp,
                                end = 5.dp,
                                top = 5.dp,
                                bottom = 5.dp
                            ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        textStyle =
                            TextStyle(
                                color = PrimaryText,
                                fontSize = 10.5.sp,
                                fontFamily = InterfaceFont
                            ),
                        cursorBrush = SolidColor(AccentOrange),
                        modifier =
                            Modifier
                                .weight(1f)
                                .focusRequester(replyFocus)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Send",
                        color = BackgroundBlack,
                        fontSize = 10.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.SemiBold,
                        modifier =
                            Modifier
                                .background(
                                    AccentOrange,
                                    RoundedCornerShape(9.dp)
                                )
                                .clickable { sendReply() }
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                )
                    )
                }

            } else {

                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(39.dp)
                                .background(
                                    AccentOrange,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { openInApp() },
                        horizontalArrangement =
                            Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Open in $appName",
                            color = BackgroundBlack,
                            fontSize = 10.5.sp,
                            fontFamily = InterfaceFont,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Outlined.NorthEast,
                            contentDescription = null,
                            tint = BackgroundBlack,
                            modifier = Modifier.size(11.5.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {

                        if (notification?.canReply == true) {
                            DaylightSecondaryButton(
                                label = "Quick reply",
                                modifier = Modifier.weight(1f),
                                onClick = { replying = true }
                            )
                        }

                        DaylightSecondaryButton(
                            label = "Clear",
                            modifier = Modifier.weight(1f),
                            onClick = { clear() }
                        )
                    }
                }
            }
        }
    }
}

/*
 * A 30 dp round icon button: the close button on the mail preview,
 * compose and inbox headers.
 */
@Composable
internal fun MailRoundIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    background: Color = SurfaceBlack,
    onClick: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .size(30.dp)
                .background(
                    background,
                    CircleShape
                )
                .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = PrimaryText,
            modifier = Modifier.size(11.5.dp)
        )
    }
}

/*
 * Mail-app notifications usually carry "Subject\npreview" in their
 * expanded text. Returns (subject, preview); a single short line is
 * taken as the subject, a single long one as the preview.
 */
internal fun splitMailNotificationText(
    text: String
): Pair<String, String> {

    val trimmed =
        text.trim()

    val newline =
        trimmed.indexOf('\n')

    if (newline > 0) {
        return trimmed.substring(0, newline).trim() to
                trimmed.substring(newline + 1).trim()
    }

    return if (trimmed.length <= 80) {
        trimmed to ""
    } else {
        "" to trimmed
    }
}
