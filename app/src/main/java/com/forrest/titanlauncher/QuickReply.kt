package com.forrest.titanlauncher

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.asImageBitmap
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forrest.titanlauncher.contacts.Contact
import com.forrest.titanlauncher.messages.MmsSender
import com.forrest.titanlauncher.messages.SmsMessage
import com.forrest.titanlauncher.notifications.HubNotification
import com.forrest.titanlauncher.notifications.NotificationMessage
import com.forrest.titanlauncher.notifications.NotificationCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/*
 * QUICK REPLY
 *
 * The bolt button in the home header. Shows recent incoming texts and
 * any notification the posting app marked as replyable, and lets the
 * user answer without leaving home.
 *
 * Texts go out through the launcher's own send path. Notifications
 * fire their own direct-reply action, the same one the system shade
 * uses, so replying to a chat app works without that app ever being
 * opened.
 */


/*
 * Four Daylight rows (about 49.dp each with their divider). The list
 * caps here and scrolls, so the card is the same height whether four
 * alerts are waiting or twelve.
 */
private val QuickReplyListMaxHeight =
    200.dp

internal data class QuickReplyEntry(
    val id: String,
    val title: String,
    val preview: String,
    val timestamp: Long,
    val isMessage: Boolean,
    val phoneNumber: String? = null,
    val photoUri: String? = null,
    val notificationKey: String? = null,
    val canReply: Boolean = true,
    val unread: Boolean = false,

    /*
     * Present on group threads. Replying needs every participant, not
     * just whoever sent the message being answered.
     */
    val threadKey: String = "",
    val participants: List<String> = emptyList(),
    val isGroup: Boolean = false,

    /*
     * The app that posted a notification, so its icon can stand in
     * for the generic glyph.
     */
    val sourcePackage: String? = null,

    /*
     * The posting app's name ("Slack"), for the card's open button and
     * its "Open in ..." pill. Null for texts.
     */
    val appName: String? = null,

    /*
     * Incoming texts in the thread not yet read, shown on the
     * person's chip ("Sarah · 2"). Zero for notifications.
     */
    val unreadCount: Int = 0,

    /*
     * Recent messages, newest last: what a MessagingStyle
     * notification carried, or the thread's latest texts. Empty for
     * apps that post a plain notification.
     */
    val messages: List<NotificationMessage> = emptyList()
)


/*
 * Builds the combined list: incoming texts newest first, then any
 * replyable notifications, capped so the card stays a sensible height.
 */
internal fun buildQuickReplyEntries(
    recentMessages: List<SmsMessage>,
    notifications: List<HubNotification>,
    contactNameFor: (String) -> String?,
    contactPhotoFor: (String) -> String?,
    limit: Int = 12
): List<QuickReplyEntry> {

    /*
     * Every text, both directions, by the same thread key the entries
     * use, so the card can show the recent exchange as bubbles.
     */
    val threadMessagesByKey =
        recentMessages
            .groupBy {
                it.threadKey
                    .ifBlank {
                        it.phoneNumber
                    }
            }

    val messageEntries =
        recentMessages
            .filter {
                it.incoming
            }
            .groupBy {

                /*
                 * By thread, so a group is one entry rather than one
                 * per member who has written.
                 */
                it.threadKey
                    .ifBlank {
                        it.phoneNumber
                    }
            }
            .values
            .mapNotNull { thread ->

                /*
                 * Only threads still waiting on the user appear here.
                 * Replying marks the thread read, which is what makes
                 * the entry drop out of the list and the bolt go dim.
                 */
                if (
                    thread.none {
                        it.incoming &&
                                !it.isRead
                    }
                ) {
                    return@mapNotNull null
                }

                val latest =
                    thread
                        .maxByOrNull {
                            it.timestamp
                        }
                        ?: return@mapNotNull null

                val isGroup =
                    thread.any {
                        it.isGroup
                    }

                val threadKey =
                    latest.threadKey
                        .ifBlank {
                            latest.phoneNumber
                        }

                val participants =
                    threadKey
                        .split(",")
                        .filter {
                            it.isNotBlank()
                        }

                val recentExchange =
                    threadMessagesByKey[threadKey]
                        .orEmpty()
                        .sortedBy {
                            it.timestamp
                        }
                        .takeLast(
                            8
                        )
                        .map { sms ->

                            NotificationMessage(
                                sender =
                                    if (
                                        isGroup &&
                                        sms.incoming
                                    ) {
                                        val senderNumber =
                                            sms.senderNumber
                                                ?: sms.phoneNumber

                                        (
                                                contactNameFor(
                                                    senderNumber
                                                )
                                                    ?: senderNumber
                                                )
                                            .substringBefore(" ")
                                    } else {
                                        ""
                                    },
                                text =
                                    sms.body,
                                timestamp =
                                    sms.timestamp,
                                fromUser =
                                    !sms.incoming
                            )
                        }

                QuickReplyEntry(
                    unread =
                        thread.any {
                            it.incoming &&
                                    !it.isRead
                        },
                    unreadCount =
                        thread.count {
                            it.incoming &&
                                    !it.isRead
                        },
                    messages =
                        recentExchange,
                    id =
                        "sms:" + threadKey,
                    threadKey =
                        threadKey,
                    participants =
                        participants,
                    isGroup =
                        isGroup,
                    title =
                        if (
                            isGroup
                        ) {

                            participants
                                .map { number ->

                                    (
                                            contactNameFor(
                                                number
                                            )
                                                ?: number
                                            )
                                        .substringBefore(" ")
                                }
                                .joinToString(
                                    separator = ", "
                                )

                        } else {

                            contactNameFor(
                                latest.phoneNumber
                            )
                                ?: latest.phoneNumber
                        },
                    preview =
                        latest.body,
                    timestamp =
                        latest.timestamp,
                    isMessage =
                        true,
                    phoneNumber =
                        latest.phoneNumber,
                    photoUri =
                        if (
                            isGroup
                        ) {
                            null
                        } else {
                            contactPhotoFor(
                                latest.phoneNumber
                            )
                        }
                )
            }

    val notificationEntries =
        notifications
            .filter {
                it.canReply
            }
            .map { notification ->

                QuickReplyEntry(
                    unread =
                        true,
                    id =
                        "noti:" + notification.key,
                    title =
                        notification.title
                            .ifBlank {
                                notification.appName
                            },
                    preview =
                        notification.text,
                    timestamp =
                        notification.timestamp,
                    isMessage =
                        false,
                    notificationKey =
                        notification.key,
                    messages =
                        notification.messages,
                    sourcePackage =
                        notification.packageName,
                    appName =
                        notification.appName
                            .ifBlank {
                                null
                            }
                )
            }

    return (messageEntries + notificationEntries)
        .sortedByDescending {
            it.timestamp
        }
        .take(
            limit
        )
}


/*
 * DAYLIGHT QUICK REPLY
 *
 * Sizes follow the mockup (C-QuickReply): mockup px x 0.82 = dp and
 * px x 0.70 = sp. Colours are theme globals only, so light and dark
 * both work.
 */
private val QuickReplyPageGutter = 16.dp
private val QuickReplyCardRadius = 16.dp
private val QuickReplyCardPadding = 13.dp
private val QuickReplyAvatarSize = 30

@Composable
internal fun QuickReplyOverlay(
    entries: List<QuickReplyEntry>,
    onSendMessage: (Contact, String) -> Unit,
    onMarkConversationRead: (String) -> Unit,
    onOpenConversation: (Contact) -> Unit,
    onStatus: (String) -> Unit = {},
    /*
     * A reply went out and nothing else is waiting: the caller closes
     * quick reply and shows "All caught up".
     */
    onAllCaughtUp: () -> Unit = {},
    onDismiss: () -> Unit
) {

    val context =
        LocalContext.current

    val messageSounds =
        rememberMessageSounds()

    /*
     * Opens on the compact list of everyone waiting. Tapping one opens
     * that conversation with the reply field; Back returns to the list.
     */
    var selectedEntry by remember {
        mutableStateOf<QuickReplyEntry?>(
            null
        )
    }

    var replyText by remember {
        mutableStateOf(
            ""
        )
    }

    /*
     * A missed call opened from the list: its card with Call back,
     * Text and Remind me.
     */
    var selectedMissedCall by remember {
        mutableStateOf<MissedCall?>(
            null
        )
    }

    var notice by remember {
        mutableStateOf(
            ""
        )
    }

    val replyFocus =
        remember {
            FocusRequester()
        }

    /*
     * Clearing without answering: a text is marked read, a
     * notification is dismissed outright. Either way the entry stops
     * being outstanding and drops out of the list.
     */
    fun dismissEntry(
        entry: QuickReplyEntry
    ) {

        if (
            entry.isMessage
        ) {

            entry.phoneNumber
                ?.let(
                    onMarkConversationRead
                )

        } else {

            entry.notificationKey
                ?.let {
                    NotificationCenter
                        .dismissNotification(
                            it
                        )
                }
        }
    }

    fun closeReply() {

        selectedEntry =
            null

        selectedMissedCall =
            null

        replyText =
            ""
    }

    fun selectEntry(
        entry: QuickReplyEntry
    ) {

        notice =
            ""

        replyText =
            ""

        selectedEntry =
            entry
    }

    /*
     * A text opens its thread in the default messaging app (or
     * Prompt's own thread when Prompt is the default); a notification
     * fires its own content intent, the deep link the posting app
     * supplied, so Slack lands on that chat.
     *
     * Fired before the overlay closes for notifications: dismissing
     * first can clear the notification this depends on.
     */
    fun openFullConversation(
        entry: QuickReplyEntry
    ) {

        val number =
            entry.phoneNumber

        val key =
            entry.notificationKey

        if (
            entry.isMessage &&
            number != null
        ) {

            closeReply()
            onDismiss()

            if (
                !openThreadInMessagingAppIfNotDefault(
                    context,
                    number
                )
            ) {
                onOpenConversation(
                    Contact(
                        name =
                            entry.title,
                        phoneNumber =
                            number
                    )
                )
            }

        } else if (
            key != null
        ) {

            NotificationCenter
                .openNotification(
                    key =
                        key,
                    context =
                        context
                )

            closeReply()
            onDismiss()
        }
    }

    /*
     * After a reply goes out: if that was the last thing waiting,
     * quick reply closes and home shows "All caught up"; otherwise the
     * usual "sent" confirmation and back to the list.
     */
    fun finishSent(
        sentEntry: QuickReplyEntry
    ) {

        val othersWaiting =
            entries.any {
                it.id != sentEntry.id &&
                        (it.unread || !it.isMessage)
            }

        closeReply()

        if (
            othersWaiting
        ) {
            onStatus(
                "✓ MESSAGE SENT"
            )
        } else {
            onAllCaughtUp()
        }
    }

    fun sendReply() {

        val entry =
            selectedEntry
                ?: return

        val message =
            replyText.trim()

        if (
            message.isBlank()
        ) {
            return
        }

        messageSounds
            .playQuickReplySend()

        if (
            entry.isMessage &&
            entry.isGroup
        ) {

            /*
             * A group reply is an MMS to every participant, the same
             * path the conversation screen uses.
             */
            MmsSender.send(
                context =
                    context,
                recipients =
                    entry.participants,
                body =
                    message,
                threadKey =
                    entry.threadKey
            )

            onMarkConversationRead(
                entry.threadKey
            )

            finishSent(entry)
            return
        }

        if (
            entry.isMessage
        ) {

            val number =
                entry.phoneNumber
                    ?: return

            DaylightThreadLog.recordReply(
                context,
                entry,
                message
            )

            onSendMessage(
                Contact(
                    name =
                        entry.title,
                    phoneNumber =
                        number
                ),
                message
            )

            /*
             * Answering clears the alert, so the bolt stops glowing
             * for something already dealt with.
             */
            onMarkConversationRead(
                number
            )

            /*
             * Back to the list rather than out of the overlay. The
             * entry disappears on its own once the thread reads as
             * handled, so what is left is what still needs answering.
             */
            finishSent(entry)
            return
        }

        val key =
            entry.notificationKey
                ?: return

        val sent =
            NotificationCenter.sendReply(
                context =
                    context,
                key =
                    key,
                message =
                    message
            )

        if (
            sent
        ) {
            DaylightThreadLog.recordReply(
                context,
                entry,
                message
            )

            NotificationCenter
                .dismissNotification(
                    key
                )

            finishSent(entry)
        } else {
            notice =
                "could not send reply"
        }
    }

    BackHandler {

        if (
            selectedEntry != null ||
            selectedMissedCall != null
        ) {
            closeReply()
        } else {
            onDismiss()
        }
    }

    /*
     * Home blurs itself behind quick reply; this only dims it, with a
     * cool grey in light mode and a darker veil in dark mode.
     */
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BorderGray.copy(
                        alpha = 0.6f
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures {
                        onDismiss()
                    }
                },
        contentAlignment =
            Alignment.TopCenter
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            // Consume taps inside the panel.
                        }
                    }
                    .onPreviewKeyEvent { event ->

                        if (
                            event.type !=
                            KeyEventType.KeyDown
                        ) {
                            false
                        } else if (
                            event.key == Key.Escape
                        ) {

                            if (
                                selectedEntry != null ||
                                selectedMissedCall != null
                            ) {
                                closeReply()
                            } else {
                                onDismiss()
                            }

                            true
                        } else {
                            false
                        }
                    }
                    .padding(
                        vertical = 13.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {

            val activeEntry =
                selectedEntry

            val activeMissedCall =
                selectedMissedCall

            if (
                activeEntry == null &&
                activeMissedCall != null
            ) {

                /*
                 * MISSED CALL CARD: ✕ goes back to the list.
                 */
                /*
                 * Dropped below the Titan's top-left camera so the
                 * avatar isn't clipped.
                 */
                androidx.compose.foundation.layout.Box(
                    modifier =
                        Modifier.padding(
                            start = QuickReplyPageGutter,
                            end = QuickReplyPageGutter,
                            top = 30.dp
                        )
                ) {
                    MissedCallActionsCard(
                        call =
                            activeMissedCall,
                        onCallBack = {
                            MissedCallStore.markHandled(
                                context,
                                activeMissedCall
                            )
                            closeReply()
                            onDismiss()
                            dialMissedCall(
                                context,
                                activeMissedCall
                            )
                        },
                        onText = {
                            MissedCallStore.markHandled(
                                context,
                                activeMissedCall
                            )

                            /*
                             * Write the text right here, like any
                             * other quick reply.
                             */
                            selectEntry(
                                QuickReplyEntry(
                                    id =
                                        "missed-call:" + activeMissedCall.id,
                                    title =
                                        activeMissedCall.name,
                                    preview =
                                        "📞 Missed call",
                                    timestamp =
                                        activeMissedCall.timestamp,
                                    isMessage =
                                        true,
                                    phoneNumber =
                                        activeMissedCall.number
                                )
                            )

                            selectedMissedCall =
                                null
                        },
                        onRemind = {
                            pickCallBackTime(
                                context
                            ) { millis ->

                                val result =
                                    scheduleCallBack(
                                        context,
                                        activeMissedCall,
                                        millis
                                    )

                                if (
                                    result.startsWith("✓")
                                ) {
                                    closeReply()
                                    onStatus(result)
                                    onDismiss()
                                } else {
                                    notice =
                                        result.lowercase()
                                }
                            }
                        },
                        onClose = {
                            closeReply()
                        }
                    )
                }

                if (
                    notice.isNotBlank()
                ) {
                    QuickReplyNotice(
                        text =
                            notice,
                        modifier =
                            Modifier.padding(
                                horizontal = QuickReplyPageGutter
                            )
                    )
                }

            } else if (
                activeEntry == null
            ) {

                /*
                 * Indented past the Titan's top-left camera.
                 */
                Text(
                    text =
                        "Quick reply",
                    color =
                        PrimaryText,
                    fontSize =
                        11.sp,
                    lineHeight =
                        13.sp,
                    fontFamily =
                        InterfaceFont,
                    fontWeight =
                        FontWeight.SemiBold,
                    modifier =
                        Modifier
                            .padding(
                                start = DaylightCameraLane,
                                top = 2.dp
                            )
                )

                /*
                 * Missed calls first, in fire colour. Tap one for Call
                 * back / Text / Remind me; ✕ clears it.
                 */
                val missedCalls =
                    MissedCallsWaiting

                if (
                    missedCalls.isNotEmpty()
                ) {
                    Column(
                        modifier =
                            Modifier
                                .padding(
                                    horizontal = QuickReplyPageGutter
                                )
                                .fillMaxWidth()
                                .background(
                                    SurfaceBlack,
                                    RoundedCornerShape(
                                        QuickReplyCardRadius
                                    )
                                )
                    ) {
                        missedCalls.forEachIndexed { index, call ->

                            if (
                                index > 0
                            ) {
                                Box(
                                    modifier =
                                        Modifier.padding(
                                            horizontal = QuickReplyCardPadding
                                        )
                                ) {
                                    DaylightRowDivider()
                                }
                            }

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            notice =
                                                ""

                                            selectedMissedCall =
                                                call
                                        }
                                        .padding(
                                            horizontal = QuickReplyCardPadding,
                                            vertical = 9.dp
                                        ),
                                verticalAlignment =
                                    Alignment.CenterVertically,
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        10.dp
                                    )
                            ) {

                                Box(
                                    modifier =
                                        Modifier
                                            .size(
                                                QuickReplyAvatarSize.dp
                                            )
                                            .background(
                                                MissedCallFire.copy(alpha = 0.18f),
                                                CircleShape
                                            ),
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    Text(
                                        text = "📞",
                                        fontSize = 12.sp
                                    )
                                }

                                Column(
                                    modifier =
                                        Modifier.weight(1f)
                                ) {
                                    Text(
                                        text =
                                            call.name,
                                        color =
                                            PrimaryText,
                                        fontSize =
                                            11.sp,
                                        lineHeight =
                                            13.sp,
                                        fontFamily =
                                            InterfaceFont,
                                        fontWeight =
                                            FontWeight.SemiBold,
                                        maxLines =
                                            1,
                                        overflow =
                                            TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text =
                                            "Missed call",
                                        color =
                                            MissedCallFire,
                                        fontSize =
                                            9.sp,
                                        lineHeight =
                                            11.sp,
                                        fontFamily =
                                            InterfaceFont
                                    )
                                }

                                Text(
                                    text =
                                        "✕",
                                    color =
                                        SecondaryText,
                                    fontSize =
                                        10.sp,
                                    fontFamily =
                                        InterfaceFont,
                                    modifier =
                                        Modifier
                                            .clickable {
                                                MissedCallStore.markHandled(
                                                    context,
                                                    call
                                                )
                                            }
                                            .padding(6.dp)
                                )
                            }
                        }
                    }
                }

                if (
                    entries.isNotEmpty() ||
                    missedCalls.isEmpty()
                )
                Column(
                    modifier =
                        Modifier
                            .padding(
                                horizontal = QuickReplyPageGutter
                            )
                            .fillMaxWidth()
                            .background(
                                SurfaceBlack,
                                RoundedCornerShape(
                                    QuickReplyCardRadius
                                )
                            )
                ) {

                    if (
                        entries.isEmpty()
                    ) {

                        QuickReplyEmptyState()

                    } else {

                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(
                                        max =
                                            QuickReplyListMaxHeight
                                    )
                        ) {

                            itemsIndexed(
                                items =
                                    entries,
                                key = { _, entry ->
                                    entry.id
                                }
                            ) { index, entry ->

                                if (
                                    index > 0
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.padding(
                                                horizontal = QuickReplyCardPadding
                                            )
                                    ) {
                                        DaylightRowDivider()
                                    }
                                }

                                QuickReplyRow(
                                    entry =
                                        entry,
                                    onDismissEntry = {
                                        dismissEntry(
                                            entry
                                        )
                                    },
                                    onClick = {
                                        selectEntry(
                                            entry
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                if (
                    notice.isNotBlank()
                ) {
                    QuickReplyNotice(
                        text =
                            notice,
                        modifier =
                            Modifier.padding(
                                horizontal = QuickReplyPageGutter
                            )
                    )
                }

            } else {

                /*
                 * DAYLIGHT PEOPLE STRIP
                 *
                 * Everyone waiting on you, in a row you can swipe.
                 * Tapping a name switches the conversation below.
                 * Names fade out as they slide toward the camera.
                 */
                if (
                    entries.size > 1
                ) {
                    QuickReplyPeopleStrip(
                        entries =
                            entries,
                        selectedId =
                            activeEntry.id,
                        onSelect = { entry ->
                            selectEntry(
                                entry
                            )
                        }
                    )
                }

                LaunchedEffect(
                    activeEntry.id
                ) {
                    replyFocus
                        .requestFocus()
                }

                /*
                 * The stack: two thin cards peeking out above the
                 * conversation card, hinting at the others waiting.
                 */
                Box(
                    modifier =
                        Modifier
                            .padding(
                                horizontal = QuickReplyPageGutter
                            )
                            .fillMaxWidth()
                ) {

                    Box(
                        modifier =
                            Modifier
                                .padding(
                                    horizontal = 11.5.dp
                                )
                                .fillMaxWidth()
                                .height(
                                    25.dp
                                )
                                .background(
                                    lerp(
                                        BorderGray,
                                        SurfaceBlack,
                                        0.4f
                                    ),
                                    RoundedCornerShape(
                                        topStart = QuickReplyCardRadius,
                                        topEnd = QuickReplyCardRadius
                                    )
                                )
                    )

                    Box(
                        modifier =
                            Modifier
                                .padding(
                                    start = 6.dp,
                                    end = 6.dp,
                                    top = 6.5.dp
                                )
                                .fillMaxWidth()
                                .height(
                                    25.dp
                                )
                                .background(
                                    lerp(
                                        BorderGray,
                                        SurfaceBlack,
                                        0.75f
                                    ),
                                    RoundedCornerShape(
                                        topStart = QuickReplyCardRadius,
                                        topEnd = QuickReplyCardRadius
                                    )
                                )
                    )

                    Column(
                        modifier =
                            Modifier
                                .padding(
                                    top = 13.dp
                                )
                                .fillMaxWidth()
                                .background(
                                    SurfaceBlack,
                                    RoundedCornerShape(
                                        QuickReplyCardRadius
                                    )
                                )
                                .padding(
                                    QuickReplyCardPadding
                                ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        QuickReplyCardHeader(
                            entry =
                                activeEntry,
                            onOpenFull = {
                                openFullConversation(
                                    activeEntry
                                )
                            },
                            /*
                             * X goes back to the list of everyone
                             * waiting, not all the way home.
                             */
                            onClose = {
                                closeReply()
                            }
                        )

                        QuickReplyBubbles(
                            entry =
                                activeEntry
                        )

                        QuickReplyField(
                            value =
                                replyText,
                            onValueChange = {
                                replyText = it
                            },
                            placeholder =
                                "Reply to " + quickReplyFirstName(
                                    activeEntry
                                ),
                            focusRequester =
                                replyFocus,
                            onSend = {
                                sendReply()
                            }
                        )

                        if (
                            notice.isNotBlank()
                        ) {
                            QuickReplyNotice(
                                text =
                                    notice
                            )
                        }
                    }
                }
            }
        }
    }
}


/*
 * "Sarah Miller" -> "Sarah". Numbers and group titles stay whole,
 * since their first word means nothing on its own.
 */
private fun quickReplyFirstName(
    entry: QuickReplyEntry
): String {

    val title =
        entry.title.trim()

    return if (
        !entry.isGroup &&
        title.firstOrNull()?.isLetter() == true
    ) {
        title.substringBefore(" ")
    } else {
        title
    }
}


/*
 * Initials for the accent circle. A group shows how many people are
 * in it, counting the user.
 */
private fun quickReplyAvatarLabel(
    entry: QuickReplyEntry
): String =
    if (
        entry.isGroup &&
        entry.participants.isNotEmpty()
    ) {
        (entry.participants.size + 1).toString()
    } else {
        contactInitials(
            entry.title
        )
    }


/*
 * The contact's photo when there is one, otherwise their initials in
 * an accent circle.
 */
@Composable
private fun QuickReplyAvatar(
    entry: QuickReplyEntry,
    sizeDp: Int = QuickReplyAvatarSize
) {

    val photo =
        rememberContactPhoto(
            entry.photoUri
        )

    if (
        photo != null
    ) {

        Image(
            bitmap =
                photo,
            contentDescription =
                entry.title,
            contentScale =
                ContentScale.Crop,
            modifier =
                Modifier
                    .size(
                        sizeDp.dp
                    )
                    .clip(
                        CircleShape
                    )
        )

    } else {

        DaylightAvatar(
            label =
                quickReplyAvatarLabel(
                    entry
                ),
            filled =
                true,
            sizeDp =
                sizeDp
        )
    }
}


/*
 * Avatar, name, the "Messages" pill and the round close button.
 */
@Composable
private fun QuickReplyCardHeader(
    entry: QuickReplyEntry,
    onOpenFull: () -> Unit,
    onClose: () -> Unit
) {

    val appName =
        entry.appName
            ?.takeIf {
                it.isNotBlank()
            }

    val openLabel =
        when {
            /*
             * Short labels so the contact's name has room: just
             * the app's name, e.g. "Messages" or "Slack".
             */
            entry.isMessage || appName == null -> "Messages"
            appName.length <= 10 -> appName
            else -> "Open"
        }

    val canOpenFull =
        (
                entry.isMessage &&
                        entry.phoneNumber != null
                ) ||
                entry.notificationKey != null

    Row(
        modifier =
            Modifier
                .fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp
            )
    ) {

        QuickReplyAvatar(
            entry =
                entry
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
        ) {

            Text(
                text =
                    entry.title,
                color =
                    PrimaryText,
                fontSize =
                    11.sp,
                lineHeight =
                    13.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.SemiBold,
                maxLines =
                    1,
                overflow =
                    TextOverflow.Ellipsis
            )

        }

        if (
            canOpenFull
        ) {

            Row(
                modifier =
                    Modifier
                        .height(
                            30.dp
                        )
                        .clip(
                            RoundedCornerShape(
                                percent = 50
                            )
                        )
                        .background(
                            AccentOrange.copy(
                                alpha = 0.16f
                            )
                        )
                        .clickable {
                            onOpenFull()
                        }
                        .padding(
                            horizontal = 10.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(
                        5.dp
                    )
            ) {

                Text(
                    text =
                        openLabel,
                    color =
                        AccentOrange,
                    fontSize =
                        9.sp,
                    lineHeight =
                        11.sp,
                    fontFamily =
                        InterfaceFont,
                    fontWeight =
                        FontWeight.SemiBold,
                    maxLines =
                        1
                )

                /*
                 * Arrow pointing up and out: M7 17L17 7 M9 7h8v8.
                 */
                QuickReplyLineIcon(
                    segments =
                        floatArrayOf(
                            7f, 17f, 17f, 7f,
                            9f, 7f, 17f, 7f,
                            17f, 7f, 17f, 15f
                        ),
                    tint =
                        AccentOrange,
                    size =
                        10.5.dp
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .size(
                        30.dp
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        BackgroundBlack
                    )
                    .clickable {
                        onClose()
                    },
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Close,
                contentDescription =
                    "Close",
                tint =
                    PrimaryText,
                modifier =
                    Modifier
                        .size(
                            13.dp
                        )
            )
        }
    }
}


/*
 * The recent exchange as bubbles: theirs on the left in the page
 * tint, yours on the right in the accent. Falls back to one incoming
 * bubble holding the preview when the app sent no structured
 * messages.
 */
@Composable
private fun QuickReplyBubbles(
    entry: QuickReplyEntry
) {

    val context =
        LocalContext.current

    val shown =
        if (
            entry.messages.isNotEmpty()
        ) {
            entry.messages
        } else {
            listOf(
                NotificationMessage(
                    sender =
                        "",
                    text =
                        entry.preview,
                    timestamp =
                        entry.timestamp,
                    fromUser =
                        false
                )
            )
        }

    /*
     * Your own replies: a notification usually carries only what you
     * haven't read, so the stored texts fill in the rest of the
     * exchange (DaylightThreadHistory.kt). Loaded off the main thread;
     * the card shows the notification's messages until it arrives.
     */
    var smsGranted by
    remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            smsGranted = granted
        }

    /*
     * Reading your past texts needs the SMS permission, which
     * Daylight only asks for here, the first time a text thread opens.
     */
    LaunchedEffect(
        entry.id
    ) {
        if (
            !smsGranted &&
            isTextThread(context, entry)
        ) {
            val prefs =
                context.getSharedPreferences(
                    "daylight_thread_log_meta",
                    android.content.Context.MODE_PRIVATE
                )

            if (
                !prefs.getBoolean("asked_sms", false)
            ) {
                prefs.edit().putBoolean("asked_sms", true).apply()
                smsPermission.launch(
                    Manifest.permission.READ_SMS
                )
            }
        }
    }

    /*
     * Re-reads the thread every few seconds while the card is open, so
     * a reply sent from Messages (or a new text) shows up in place.
     */
    var historyTick by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(
        entry.id
    ) {
        while (true) {
            kotlinx.coroutines.delay(5_000)
            historyTick++
        }
    }

    val history by
    produceState(
        initialValue =
            emptyList<NotificationMessage>(),
        entry.id,
        smsGranted,
        entry.messages.size,
        historyTick
    ) {
        value =
            withContext(
                Dispatchers.IO
            ) {
                runCatching {

                    /*
                     * Keep what this notification carried, so the
                     * exchange is still there after it's cleared.
                     */
                    DaylightThreadLog.record(
                        context,
                        entry,
                        entry.messages
                    )

                    mergeThreadHistory(
                        DaylightThreadLog.load(
                            context,
                            entry
                        ),
                        loadSmsThreadHistory(
                            context,
                            entry
                        )
                    )
                }
                    .getOrDefault(
                        emptyList()
                    )
            }
    }

    val messages =
        mergeThreadHistory(
            shown,
            history
        )
            .takeLast(
                40
            )

    /*
     * Opens at the newest message rather than the oldest: the reason
     * to look is what just arrived.
     */
    val bubbleScroll =
        rememberScrollState()

    LaunchedEffect(
        entry.id,
        messages.size,
        bubbleScroll.maxValue
    ) {
        bubbleScroll.scrollTo(
            bubbleScroll.maxValue
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                /*
                 * About six bubbles. Anything older scrolls rather
                 * than pushing the reply field off the card.
                 */
                .heightIn(
                    max = 210.dp
                )
                .verticalScroll(
                    bubbleScroll
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                5.dp
            )
    ) {

        messages.forEach { message ->

            val outgoing =
                message.fromUser

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                horizontalArrangement =
                    if (outgoing) Arrangement.End else Arrangement.Start
            ) {

                /*
                 * 78% of the card, like the mockup's max-width.
                 */
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(
                                0.78f
                            ),
                    contentAlignment =
                        if (outgoing) Alignment.CenterEnd else Alignment.CenterStart
                ) {

                    Column(
                        modifier =
                            Modifier
                                .background(
                                    if (outgoing) AccentOrange else BackgroundBlack,
                                    if (outgoing) {
                                        RoundedCornerShape(
                                            topStart = 13.dp,
                                            topEnd = 13.dp,
                                            bottomEnd = 3.5.dp,
                                            bottomStart = 13.dp
                                        )
                                    } else {
                                        RoundedCornerShape(
                                            topStart = 13.dp,
                                            topEnd = 13.dp,
                                            bottomEnd = 13.dp,
                                            bottomStart = 3.5.dp
                                        )
                                    }
                                )
                                .padding(
                                    horizontal = 10.5.dp,
                                    vertical = 7.5.dp
                                )
                    ) {

                        if (
                            !outgoing &&
                            message.sender.isNotBlank() &&
                            /*
                             * Names only matter when more than one
                             * person is talking, so a one-to-one chat
                             * doesn't repeat the contact's name on
                             * every bubble.
                             */
                            (
                                    entry.isGroup ||
                                            entry.messages
                                                .filter { !it.fromUser }
                                                .map { it.sender }
                                                .distinct()
                                                .size > 1
                                    )
                        ) {

                            Text(
                                text =
                                    message.sender,
                                color =
                                    SecondaryText,
                                fontSize =
                                    7.5.sp,
                                lineHeight =
                                    9.sp,
                                fontFamily =
                                    InterfaceFont,
                                fontWeight =
                                    FontWeight.SemiBold,
                                maxLines =
                                    1
                            )
                        }

                        /*
                         * Pictures first, like a messaging app. If
                         * one shows, the "📷 Photo" stand-in text is
                         * dropped; if none can be opened, the text
                         * stays so the message isn't blank.
                         */
                        val picturesShown =
                            QuickReplyPictures(
                                uris =
                                    message.imageUris
                            )

                        val caption =
                            if (picturesShown) {
                                message.text
                                    .lines()
                                    .filterNot {
                                        val line = it.trim().lowercase()
                                        line.startsWith("📷") ||
                                                line in setOf(
                                            "image", "photo", "picture",
                                            "sent an image", "sent a photo", "sent a picture"
                                        )
                                    }
                                    .joinToString("\n")
                                    .trim()
                            } else {
                                message.text
                            }

                        if (
                            caption.isNotBlank()
                        ) {
                            Text(
                                text =
                                    caption,
                                color =
                                    if (outgoing) BackgroundBlack else PrimaryText,
                                fontSize =
                                    10.sp,
                                lineHeight =
                                    13.sp,
                                fontFamily =
                                    InterfaceFont,
                                modifier =
                                    if (picturesShown) {
                                        Modifier.padding(top = 5.dp)
                                    } else {
                                        Modifier
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}


/*
 * PICTURES IN QUICK REPLY
 *
 * Decoded off the main thread, scaled down to thumbnail size and
 * kept in a small memory cache, so scrolling the conversation and the
 * five-second refresh don't decode them again.
 */
private val QuickReplyPictureCache =
    object : android.util.LruCache<String, android.graphics.Bitmap>(
        12 * 1024 * 1024
    ) {
        override fun sizeOf(
            key: String,
            value: android.graphics.Bitmap
        ): Int =
            value.byteCount
    }

private fun loadQuickReplyPicture(
    context: android.content.Context,
    uri: String,
    maxSide: Int = 720
): android.graphics.Bitmap? {

    QuickReplyPictureCache.get(uri)?.let { return it }

    return runCatching {

        val parsed =
            android.net.Uri.parse(uri)

        val bounds =
            android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

        context.contentResolver
            .openInputStream(parsed)
            ?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, bounds)
            }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return@runCatching null
        }

        var sample = 1
        while (
            bounds.outWidth / (sample * 2) >= maxSide ||
            bounds.outHeight / (sample * 2) >= maxSide
        ) {
            sample *= 2
        }

        context.contentResolver
            .openInputStream(parsed)
            ?.use {
                android.graphics.BitmapFactory.decodeStream(
                    it,
                    null,
                    android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = sample
                    }
                )
            }
    }
        .getOrNull()
        ?.also {
            QuickReplyPictureCache.put(uri, it)
        }
}

/*
 * The message's pictures, stacked. Returns whether any could be
 * shown (a notification's image may not be readable by Prompt).
 */
@Composable
private fun QuickReplyPictures(
    uris: List<String>
): Boolean {

    if (uris.isEmpty()) {
        return false
    }

    val context =
        LocalContext.current

    val bitmaps by
    produceState(
        initialValue =
            uris.mapNotNull { QuickReplyPictureCache.get(it) },
        uris
    ) {
        value =
            withContext(
                Dispatchers.IO
            ) {
                uris.mapNotNull { uri ->
                    loadQuickReplyPicture(context, uri)
                }
            }
    }

    if (bitmaps.isEmpty()) {
        return false
    }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {
        bitmaps.forEach { bitmap ->

            val ratio =
                (bitmap.width.toFloat() / bitmap.height.toFloat())
                    .coerceIn(0.5f, 2f)

            Image(
                bitmap =
                    bitmap.asImageBitmap(),
                contentDescription =
                    "Picture",
                contentScale =
                    ContentScale.Crop,
                modifier =
                    Modifier
                        .widthIn(max = 180.dp)
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .heightIn(max = 220.dp)
                        .clip(
                            RoundedCornerShape(9.dp)
                        )
            )
        }
    }

    return true
}


/*
 * The reply field: an accent-outlined box with the text field and a
 * square accent send button. Enter sends too.
 */
@Composable
private fun QuickReplyField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    onSend: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    1.25.dp,
                    AccentOrange,
                    RoundedCornerShape(
                        11.5.dp
                    )
                )
                .padding(
                    start = 11.5.dp,
                    end = 8.dp,
                    top = 7.5.dp,
                    bottom = 7.5.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp
            )
    ) {

        Box(
            modifier =
                Modifier
                    .weight(1f),
            contentAlignment =
                Alignment.CenterStart
        ) {

            if (
                value.isEmpty()
            ) {

                Text(
                    text =
                        placeholder,
                    color =
                        TertiaryText,
                    fontSize =
                        10.sp,
                    lineHeight =
                        13.sp,
                    fontFamily =
                        InterfaceFont,
                    maxLines =
                        1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            BasicTextField(
                value =
                    value,
                onValueChange =
                    onValueChange,
                singleLine =
                    true,
                textStyle =
                    TextStyle(
                        color =
                            PrimaryText,
                        fontSize =
                            10.sp,
                        lineHeight =
                            13.sp,
                        fontFamily =
                            InterfaceFont
                    ),
                cursorBrush =
                    SolidColor(
                        AccentOrange
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(
                            focusRequester
                        )
                        .onPreviewKeyEvent { event ->

                            if (
                                event.type ==
                                KeyEventType.KeyDown &&
                                event.key == Key.Enter
                            ) {
                                onSend()
                                true
                            } else {
                                false
                            }
                        }
            )
        }

        Box(
            modifier =
                Modifier
                    .size(
                        28.dp
                    )
                    .clip(
                        RoundedCornerShape(
                            8.dp
                        )
                    )
                    .background(
                        AccentOrange
                    )
                    .clickable {
                        onSend()
                    },
            contentAlignment =
                Alignment.Center
        ) {

            /*
             * Up arrow: M12 19V5 M6 11l6-6 6 6.
             */
            QuickReplyLineIcon(
                segments =
                    floatArrayOf(
                        12f, 19f, 12f, 5f,
                        6f, 11f, 12f, 5f,
                        18f, 11f, 12f, 5f
                    ),
                tint =
                    BackgroundBlack,
                size =
                    13.dp
            )
        }
    }
}


/*
 * A small stroked icon drawn from line segments on the mockup's
 * 24-unit grid (x1, y1, x2, y2 per segment), so it matches the
 * mockup's SVG exactly without another icon dependency.
 */
@Composable
private fun QuickReplyLineIcon(
    segments: FloatArray,
    tint: Color,
    size: Dp,
    strokeUnits: Float = 2.4f
) {

    Canvas(
        modifier =
            Modifier
                .size(
                    size
                )
    ) {

        val unit =
            this.size.width / 24f

        var index = 0

        while (
            index + 3 < segments.size
        ) {

            drawLine(
                color =
                    tint,
                start =
                    Offset(
                        segments[index] * unit,
                        segments[index + 1] * unit
                    ),
                end =
                    Offset(
                        segments[index + 2] * unit,
                        segments[index + 3] * unit
                    ),
                strokeWidth =
                    strokeUnits * unit,
                cap =
                    StrokeCap.Round
            )

            index += 4
        }
    }
}


@Composable
private fun QuickReplyNotice(
    text: String,
    modifier: Modifier = Modifier
) {

    Text(
        text =
            text,
        color =
            AccentOrange,
        fontSize =
            8.5.sp,
        lineHeight =
            10.5.sp,
        fontFamily =
            InterfaceFont,
        modifier =
            modifier
    )
}


/*
 * Nothing waiting on the user.
 */
@Composable
private fun QuickReplyEmptyState() {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 20.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text =
                "All caught up",
            color =
                PrimaryText,
            fontSize =
                11.sp,
            lineHeight =
                13.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            modifier =
                Modifier
                    .height(
                        3.dp
                    )
        )

        Text(
            text =
                "go enjoy life",
            color =
                SecondaryText,
            fontSize =
                9.sp,
            lineHeight =
                11.sp,
            fontFamily =
                InterfaceFont
        )
    }
}


/*
 * One waiting conversation in the list: avatar, name, preview and
 * time, with a small clear button under the time.
 */
@Composable
private fun QuickReplyRow(
    entry: QuickReplyEntry,
    onDismissEntry: () -> Unit,
    onClick: () -> Unit
) {

    /*
     * A notification's own app icon, as a small badge on the avatar,
     * says where it came from.
     */
    val sourceIcon =
        rememberAppIconBitmap(
            entry.sourcePackage
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                }
                .padding(
                    horizontal = QuickReplyCardPadding,
                    vertical = 9.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        QuickReplyAvatarSize.dp
                    )
        ) {

            QuickReplyAvatar(
                entry =
                    entry
            )

            if (
                !entry.isMessage &&
                sourceIcon != null
            ) {

                Image(
                    bitmap =
                        sourceIcon,
                    contentDescription =
                        entry.appName,
                    modifier =
                        Modifier
                            .align(
                                Alignment.BottomEnd
                            )
                            .size(
                                12.dp
                            )
                            .clip(
                                CircleShape
                            )
                            .background(
                                SurfaceBlack
                            )
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
        ) {

            Text(
                text =
                    entry.title,
                color =
                    PrimaryText,
                fontSize =
                    10.5.sp,
                lineHeight =
                    12.5.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    if (entry.unread) FontWeight.SemiBold else FontWeight.Medium,
                maxLines =
                    1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                text =
                    entry.preview,
                color =
                    SecondaryText,
                fontSize =
                    9.sp,
                lineHeight =
                    11.sp,
                fontFamily =
                    InterfaceFont,
                maxLines =
                    1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }

        Column(
            horizontalAlignment =
                Alignment.End
        ) {

            Text(
                text =
                    formatInboxTime(
                        entry.timestamp
                    ),
                color =
                    TertiaryText,
                fontSize =
                    8.sp,
                lineHeight =
                    10.sp,
                fontFamily =
                    InterfaceFont,
                maxLines =
                    1
            )

            /*
             * Clears the alert without replying. Its own clickable
             * consumes the tap, so the row underneath does not open.
             */
            Box(
                modifier =
                    Modifier
                        .size(
                            18.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .clickable {
                            onDismissEntry()
                        },
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Close,
                    contentDescription =
                        "clear alert",
                    tint =
                        TertiaryText,
                    modifier =
                        Modifier
                            .size(
                                10.dp
                            )
                )
            }
        }
    }
}


/*
 * The swipeable row of people at the top of quick reply.
 *
 * The first chip starts to the right of the Titan's camera. The left
 * edge is masked with a gradient so a name fades out as it is swiped
 * toward the camera and fades back in when swiped back; the right
 * edge fades too, hinting that there is more to swipe.
 */
@Composable
private fun QuickReplyPeopleStrip(
    entries: List<QuickReplyEntry>,
    selectedId: String?,
    onSelect: (QuickReplyEntry) -> Unit
) {

    val listState =
        rememberLazyListState()

    /*
     * Keep the selected person in view when the selection changes,
     * whether from a chip or from somewhere else.
     */
    LaunchedEffect(
        selectedId
    ) {
        val index =
            entries.indexOfFirst {
                it.id == selectedId
            }

        if (
            index >= 0
        ) {
            listState.animateScrollToItem(
                index
            )
        }
    }

    val density =
        LocalDensity.current

    /*
     * Mockup mask: clear to 40 px, solid from 64 px, fading over the
     * last 24 px on the right.
     */
    val fadeStartPx =
        with(density) { 33.dp.toPx() }

    val fadeEndPx =
        with(density) { 52.dp.toPx() }

    val rightFadePx =
        with(density) { 20.dp.toPx() }

    LazyRow(
        state =
            listState,
        contentPadding =
            PaddingValues(
                start = 52.dp,
                end = QuickReplyPageGutter
            ),
        horizontalArrangement =
            Arrangement.spacedBy(
                6.5.dp
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    compositingStrategy =
                        CompositingStrategy.Offscreen
                }
                .drawWithContent {

                    drawContent()

                    val width =
                        size.width

                    if (
                        width > fadeEndPx + rightFadePx
                    ) {
                        drawRect(
                            brush =
                                Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    (fadeStartPx / width) to Color.Transparent,
                                    (fadeEndPx / width) to Color.Black,
                                    ((width - rightFadePx) / width) to Color.Black,
                                    1f to Color.Transparent
                                ),
                            blendMode =
                                BlendMode.DstIn
                        )
                    }
                }
    ) {

        items(
            items =
                entries,
            key = {
                it.id
            }
        ) { entry ->

            val selected =
                entry.id == selectedId

            val label =
                quickReplyFirstName(
                    entry
                ) +
                        if (
                            entry.unreadCount > 1
                        ) {
                            " · " + entry.unreadCount
                        } else {
                            ""
                        }

            Text(
                text =
                    label,
                color =
                    if (selected) BackgroundBlack else PrimaryText,
                fontSize =
                    9.sp,
                lineHeight =
                    11.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines =
                    1,
                overflow =
                    TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .widthIn(
                            max = 130.dp
                        )
                        .clip(
                            RoundedCornerShape(
                                percent = 50
                            )
                        )
                        .background(
                            if (selected) PrimaryText else SurfaceBlack
                        )
                        .clickable {
                            onSelect(
                                entry
                            )
                        }
                        .padding(
                            horizontal = 11.5.dp,
                            vertical = 6.dp
                        )
            )
        }
    }
}