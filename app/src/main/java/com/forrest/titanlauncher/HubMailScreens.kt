package com.forrest.titanlauncher

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.LauncherApps
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.Email
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forrest.titanlauncher.ai.GeminiApiKeyStore
import com.forrest.titanlauncher.ai.GeminiRepository
import com.forrest.titanlauncher.calendar.CalendarCommandParser
import com.forrest.titanlauncher.calendar.CalendarEventDraft
import com.forrest.titanlauncher.calendar.CalendarRepository
import com.forrest.titanlauncher.calendar.CalendarSelectionStore
import com.forrest.titanlauncher.calendar.CalendarTarget
import com.forrest.titanlauncher.calendar.GoogleCalendarAuthManager
import com.forrest.titanlauncher.calendar.GoogleCalendarOption
import com.forrest.titanlauncher.calls.CallLogEntry
import com.forrest.titanlauncher.calls.CallLogRepository
import com.forrest.titanlauncher.calls.CallType
import com.forrest.titanlauncher.commands.Command
import com.forrest.titanlauncher.commands.CommandParser
import com.forrest.titanlauncher.contacts.Contact
import com.forrest.titanlauncher.contacts.ContactRepository
import com.forrest.titanlauncher.contacts.EmailContact
import com.forrest.titanlauncher.messages.SmsDatabase
import com.forrest.titanlauncher.messages.SmsMessage
import com.forrest.titanlauncher.messages.SmsRoleManager
import com.forrest.titanlauncher.messages.SmsSender
import com.forrest.titanlauncher.mail.GmailRepository
import com.forrest.titanlauncher.mail.GoogleMailAuthManager
import com.forrest.titanlauncher.mail.MailMessage
import com.forrest.titanlauncher.mail.allMailPackages
import com.forrest.titanlauncher.notes.NoteCategory
import com.forrest.titanlauncher.notes.NoteItem
import com.forrest.titanlauncher.notes.NoteStyleRange
import com.forrest.titanlauncher.notes.NoteTextStyle
import com.forrest.titanlauncher.notes.NoteStore
import com.forrest.titanlauncher.notifications.HubNotification
import com.forrest.titanlauncher.notifications.NotificationCenter
import com.forrest.titanlauncher.settings.CommandSurfaceTone
import com.forrest.titanlauncher.settings.HomeCornerStyle
import com.forrest.titanlauncher.settings.HomeElementSize
import com.forrest.titanlauncher.settings.HomeSurfaceTone
import com.forrest.titanlauncher.settings.LauncherAccent
import com.forrest.titanlauncher.settings.LauncherInterfaceFont
import com.forrest.titanlauncher.settings.LauncherSettings
import com.forrest.titanlauncher.settings.LauncherSettingsStore
import com.forrest.titanlauncher.settings.ReadabilityContrast
import com.forrest.titanlauncher.settings.ReadabilityMotion
import com.forrest.titanlauncher.settings.ReadabilityTextSize
import com.forrest.titanlauncher.settings.ReadabilityTextWeight
import com.forrest.titanlauncher.settings.LauncherThemeMode
import com.forrest.titanlauncher.todoist.TodoistRepository
import com.forrest.titanlauncher.todoist.TodoistTokenStore
import com.forrest.titanlauncher.usage.CurrentHourUsageDiagnostics
import com.forrest.titanlauncher.usage.HourProductivityStatus
import com.forrest.titanlauncher.usage.UsageStatsRepository
import com.forrest.titanlauncher.weather.WeatherRepository
import com.forrest.titanlauncher.weather.WeatherSnapshot
import com.forrest.titanlauncher.ui.theme.TitanLauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun HubScreen(
    smsDatabase: SmsDatabase,
    contactRepository: ContactRepository,
    hasContactsPermission: Boolean,
    mailMessages: List<MailMessage>,
    callLogEntries: List<CallLogEntry>,
    mutedNotificationApps: Set<String>,
    hasCallLogPermission: Boolean,
    selectedCategory: HubCategory,
    onSelectedCategoryChange: (HubCategory) -> Unit,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onRequestCallLogPermission: () -> Unit,
    onOpenConversation: (Contact) -> Unit,
    onOpenThread: (ConversationThread, Contact) -> Unit = { _, _ -> },
    onOpenMail: (MailMessage) -> Unit,
    onDial: (String) -> Unit,
    mailNeedsAuthorization: Boolean = false,
    onConnectMail: () -> Unit = {},
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    /*
     * Set when a call row is opened. Drives the call action sheet.
     */
    var callActionsItem by remember {
        mutableStateOf<HubDisplayItem?>(null)
    }

    /*
     * Set when an email from another mail app is opened. Drives the
     * Daylight mail preview card.
     */
    var mailPreviewItem by remember {
        mutableStateOf<HubDisplayItem?>(null)
    }

    val hubClearedStore =
        remember {
            HubClearedStore(
                context
            )
        }

    var clearedIds by remember {
        mutableStateOf(
            hubClearedStore.load()
        )
    }

    val appNotifications by
    NotificationCenter
        .notifications
        .collectAsStateWithLifecycle()

    var notificationAccessEnabled by remember {
        mutableStateOf(
            hasNotificationListenerAccess(
                context
            )
        )
    }

    val focusRequester =
        remember {
            FocusRequester()
        }

    val listState =
        rememberLazyListState()

    LaunchedEffect(Unit) {
        while (
            true
        ) {
            notificationAccessEnabled =
                hasNotificationListenerAccess(
                    context
                )

            if (
                notificationAccessEnabled
            ) {
                NotificationCenter
                    .requestRefresh()
            }

            delay(
                1000
            )
        }
    }

    val allMessages by
    smsDatabase
        .smsDao()
        .observeAllMessages()
        .collectAsStateWithLifecycle(
            initialValue =
                emptyList()
        )

    val hubItems by
    produceState(
        initialValue =
            emptyList<HubDisplayItem>(),
        key1 =
            allMessages,
        key2 =
            mailMessages,
        key3 =
            listOf(
                callLogEntries,
                hasContactsPermission,
                appNotifications,
                mutedNotificationApps
            )
    ) {

        value =
            withContext(
                Dispatchers.IO
            ) {

                val ownNumber =
                    OwnNumberStore(
                        context
                    )
                        .resolve(
                            allMessages
                        )

                val messageItems =
                    allMessages
                        .groupBy {

                            /*
                             * Grouped by thread, so a group shows as
                             * one row rather than separate entries
                             * from each member. Rows written before
                             * the migration fall back to their number.
                             */
                            it.threadKey
                                .ifBlank {
                                    normalizePhoneNumber(
                                        it.phoneNumber
                                    )
                                }
                        }
                        .values
                        .mapNotNull {
                                thread ->

                            val latest =
                                thread
                                    .maxByOrNull {
                                        it.id
                                    }
                                    ?: return@mapNotNull null

                            /*
                             * Only texts still waiting on you: a
                             * thread you've read, or where your reply
                             * is the last word, leaves the hub.
                             */
                            if (
                                !latest.incoming ||
                                thread.none { message ->
                                    message.incoming &&
                                            !message.isRead
                                }
                            ) {
                                return@mapNotNull null
                            }

                            val contactName =
                                if (
                                    hasContactsPermission
                                ) {

                                    contactRepository
                                        .findContactNameByPhoneNumber(
                                            latest.phoneNumber
                                        )

                                } else {

                                    null
                                }

                            val contactPhotoUri =
                                if (
                                    hasContactsPermission
                                ) {

                                    contactRepository
                                        .findContactPhotoUriByPhoneNumber(
                                            latest.phoneNumber
                                        )

                                } else {

                                    null
                                }

                            val isGroup =
                                thread.any {
                                    it.isGroup
                                }

                            val threadKey =
                                latest.threadKey
                                    .ifBlank {
                                        normalizePhoneNumber(
                                            latest.phoneNumber
                                        )
                                    }

                            /*
                             * A group is named by its members, the
                             * same way the messages inbox names it.
                             */
                            val title =
                                if (
                                    isGroup
                                ) {

                                    groupTitleFor(
                                        participants =
                                            threadKey
                                                .split(",")
                                                .filter {
                                                    it.isNotBlank()
                                                },
                                        ownNumber =
                                            ownNumber
                                    ) { number ->

                                        if (
                                            hasContactsPermission
                                        ) {
                                            contactRepository
                                                .findContactNameByPhoneNumber(
                                                    number
                                                )
                                        } else {
                                            null
                                        }
                                    }

                                } else {

                                    contactName
                                        ?: latest.phoneNumber
                                }

                            HubDisplayItem(
                                id =
                                    "sms:" + threadKey + ":" + latest.id,
                                type =
                                    HubItemType.MESSAGE,
                                title =
                                    title,
                                preview =
                                    if (
                                        latest.incoming
                                    ) {
                                        latest.body
                                    } else {
                                        "you: ${latest.body}"
                                    },
                                timestamp =
                                    latest.timestamp,
                                unread =
                                    thread.any {
                                            message ->

                                        message.incoming &&
                                                !message.isRead
                                    },
                                phoneNumber =
                                    latest.phoneNumber,
                                photoUri =
                                    if (
                                        isGroup
                                    ) {
                                        null
                                    } else {
                                        contactPhotoUri
                                    },
                                threadKey =
                                    threadKey,
                                isGroup =
                                    isGroup
                            )
                        }

                /*
                 * Email now works like texting: rows come from the
                 * email apps' own notifications (below), and tapping
                 * one opens it in that app. Gmail read through the API
                 * is no longer listed here, which would only duplicate
                 * Gmail's notifications.
                 */
                val emailItems =
                    emptyList<MailMessage>().map {
                            message ->

                        HubDisplayItem(
                            id =
                                "mail:${message.id}",
                            type =
                                HubItemType.EMAIL,
                            title =
                                message.sender,
                            preview =
                                message.subject
                                    .ifBlank {
                                        message.snippet
                                    },
                            timestamp =
                                message.timestamp,
                            unread =
                                message.unread,
                            mailMessage =
                                message
                        )
                    }

                val callItems =
                    callLogEntries.map {
                            call ->

                        val contactName =
                            if (
                                hasContactsPermission
                            ) {

                                contactRepository
                                    .findContactNameByPhoneNumber(
                                        call.phoneNumber
                                    )

                            } else {

                                null
                            }

                        val contactPhotoUri =
                            if (
                                hasContactsPermission
                            ) {

                                contactRepository
                                    .findContactPhotoUriByPhoneNumber(
                                        call.phoneNumber
                                    )

                            } else {

                                null
                            }

                        val callLabel =
                            when (
                                call.type
                            ) {

                                CallType.MISSED ->
                                    "missed call"

                                CallType.INCOMING ->
                                    if (
                                        call.durationSeconds > 0
                                    ) {
                                        "incoming · ${formatCallDuration(call.durationSeconds)}"
                                    } else {
                                        "incoming call"
                                    }

                                CallType.OUTGOING ->
                                    if (
                                        call.durationSeconds > 0
                                    ) {
                                        "outgoing · ${formatCallDuration(call.durationSeconds)}"
                                    } else {
                                        "outgoing call"
                                    }

                                CallType.REJECTED ->
                                    "declined call"

                                CallType.OTHER ->
                                    "call"
                            }

                        HubDisplayItem(
                            id =
                                "call:${call.id}",
                            type =
                                HubItemType.CALL,
                            title =
                                contactName
                                    ?: call.phoneNumber,
                            preview =
                                callLabel,
                            timestamp =
                                call.timestamp,
                            missed =
                                call.type ==
                                        CallType.MISSED,
                            phoneNumber =
                                call.phoneNumber,
                            photoUri =
                                contactPhotoUri
                        )
                    }

                /*
                 * Spark, Samsung Email, Outlook and other mail apps:
                 * their notifications are filed as email, not noti.
                 */
                val mailPackages =
                    if (
                        EnabledMailApps.isNotEmpty()
                    ) {
                        EnabledMailApps.toSet()
                    } else {
                        allMailPackages(
                            context
                        )
                    }

                val notificationItems =
                    appNotifications
                        .filter { notification ->
                            notification.packageName !in
                                    mutedNotificationApps
                        }
                        /*
                         * A chat whose newest message is your own
                         * reply has been answered, so it leaves the
                         * hub. (Read chats go on their own: the
                         * messaging app clears the notification.)
                         */
                        .filterNot { notification ->
                            notification.messages
                                .lastOrNull()
                                ?.fromUser == true
                        }
                        .map {
                                notification ->

                            val preview =
                                listOf(
                                    notification.title,
                                    notification.text
                                )
                                    .filter {
                                        it.isNotBlank()
                                    }
                                    .distinct()
                                    .joinToString(
                                        separator = " · "
                                    )

                            /*
                             * A notification carrying MessagingStyle
                             * messages is a conversation, so it is
                             * filed under messages with the @ glyph
                             * rather than sitting in noti.
                             */
                            val isConversation =
                                notification.messages.isNotEmpty()

                            val newestMessage =
                                notification
                                    .messages
                                    .lastOrNull()

                            val isMailApp =
                                !isConversation &&
                                        notification.packageName in
                                        mailPackages

                            /*
                             * The row's id includes when it was posted.
                             * Mail apps reuse one notification for
                             * each new email, so with the key alone an
                             * email cleared here once kept every later
                             * email from that app out of the hub
                             * (while home still showed it).
                             */
                            val rowId =
                                "notification:${notification.key}@${notification.timestamp}"
                            if (
                                isMailApp
                            ) {
                                /*
                                 * Sender as the title, the app name
                                 * and subject/preview underneath.
                                 */
                                return@map HubDisplayItem(
                                    id =
                                        rowId,
                                    type =
                                        HubItemType.EMAIL,
                                    title =
                                        notification.title
                                            .ifBlank {
                                                notification.appName
                                            },
                                    preview =
                                        listOf(
                                            notification.appName,
                                            notification.text
                                        )
                                            .filter {
                                                it.isNotBlank()
                                            }
                                            .joinToString(
                                                separator = " · "
                                            ),
                                    timestamp =
                                        notification.timestamp,
                                    unread =
                                        true,
                                    notificationKey =
                                        notification.key
                                )
                            }

                            HubDisplayItem(
                                id =
                                    rowId,
                                type =
                                    if (
                                        isConversation
                                    ) {
                                        HubItemType.MESSAGE
                                    } else {
                                        HubItemType.NOTIFICATION
                                    },
                                title =
                                    if (
                                        isConversation &&
                                        notification.title.isNotBlank()
                                    ) {
                                        notification.title
                                    } else {
                                        notification.appName
                                    },
                                preview =
                                    if (
                                        isConversation &&
                                        newestMessage != null
                                    ) {

                                        if (
                                            newestMessage.fromUser
                                        ) {
                                            "you: " + newestMessage.text
                                        } else {
                                            newestMessage.text
                                        }

                                    } else {
                                        preview
                                    },
                                timestamp =
                                    notification.timestamp,
                                unread =
                                    true,
                                notificationKey =
                                    notification.key
                            )
                        }

                (
                        messageItems +
                                emailItems +
                                callItems +
                                notificationItems
                        )
                    .sortedByDescending {
                        it.timestamp
                    }
                    .take(
                        60
                    )
            }
    }

    /*
     * Dismissed rows are removed before anything else, so the category
     * counts and the empty state agree with what is on screen.
     */
    val visibleItems =
        hubItems.filterNot {
            it.id in clearedIds
        }

    fun clearItems(
        items: List<HubDisplayItem>
    ) {

        /*
         * Notifications are real system state, so clearing one here
         * dismisses it for the device too rather than only hiding it.
         */
        items.forEach { item ->

            if (
                item.type ==
                HubItemType.NOTIFICATION ||
                item.type ==
                HubItemType.EMAIL
            ) {

                item.notificationKey
                    ?.let {
                        NotificationCenter
                            .dismissNotification(
                                it
                            )
                    }
            }
        }

        clearedIds =
            hubClearedStore.clear(
                items.map {
                    it.id
                }
            )
    }

    val categoryItems =
        when (
            selectedCategory
        ) {

            HubCategory.ALL ->
                visibleItems

            HubCategory.MESSAGES ->
                visibleItems.filter {
                    it.type ==
                            HubItemType.MESSAGE
                }

            HubCategory.CALLS ->
                visibleItems.filter {
                    it.type ==
                            HubItemType.CALL
                }

            HubCategory.EMAIL ->
                visibleItems.filter {
                    it.type ==
                            HubItemType.EMAIL
                }

            HubCategory.NOTIFICATIONS ->
                visibleItems.filter {
                    it.type ==
                            HubItemType.NOTIFICATION
                }
        }

    /*
     * Daylight groups the hub into "now" (anything unread, or from the
     * last two hours) and "earlier". Keyboard selection runs over one
     * flat index across both sections, now first, so filteredItems is
     * the two lists joined in display order.
     */
    val nowCutoff =
        System.currentTimeMillis() -
                TimeUnit.HOURS.toMillis(2)

    val nowItems =
        categoryItems.filter {
            it.unread ||
                    it.timestamp >= nowCutoff
        }

    val earlierItems =
        categoryItems.filterNot {
            it.unread ||
                    it.timestamp >= nowCutoff
        }

    val filteredItems =
        nowItems + earlierItems

    val nowCount =
        nowItems.size

    /*
     * Lazy list position of a row, counting the section headers.
     * The first row of a section scrolls to its header instead, so
     * the label stays in view.
     */
    fun lazyIndexFor(
        index: Int
    ): Int =
        when {
            index <= 0 -> 0
            index < nowCount -> index + 1
            index == nowCount -> nowCount + 1
            else -> index + 2
        }

    val newCount =
        filteredItems.count {
            it.unread
        }

    val hubSubtitleAccent: String
    val hubSubtitleRest: String

    if (
        newCount == 0
    ) {
        hubSubtitleAccent = ""
        hubSubtitleRest =
            if (filteredItems.isEmpty()) "Nothing new" else "All caught up"
    } else if (
        selectedCategory == HubCategory.EMAIL
    ) {
        val emailApps =
            filteredItems
                .mapNotNull { item ->
                    appNotifications
                        .firstOrNull {
                            it.key == item.notificationKey
                        }
                        ?.appName
                        ?.takeIf {
                            it.isNotBlank()
                        }
                }
                .distinct()

        hubSubtitleAccent =
            if (newCount == 1) "1 new email" else "$newCount new emails"

        hubSubtitleRest =
            when (emailApps.size) {
                0 -> ""
                1 -> " in " + emailApps.first()
                else -> " across " + emailApps.size + " apps"
            }
    } else {
        val startOfToday =
            java.util.Calendar
                .getInstance()
                .apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                .timeInMillis

        val oldestUnread =
            filteredItems
                .filter {
                    it.unread
                }
                .minOfOrNull {
                    it.timestamp
                }
                ?: 0L

        hubSubtitleAccent =
            "$newCount new"

        hubSubtitleRest =
            if (oldestUnread >= startOfToday) " since this morning" else " waiting for you"
    }

    LaunchedEffect(Unit) {

        focusRequester
            .requestFocus()
    }

    LaunchedEffect(
        selectedCategory,
        filteredItems.size
    ) {

        onSelectedIndexChange(
            selectedIndex
                .coerceIn(
                    0,
                    filteredItems
                        .lastIndex
                        .coerceAtLeast(
                            0
                        )
                )
        )
    }

    LaunchedEffect(
        selectedIndex,
        filteredItems.size
    ) {

        if (
            filteredItems.isNotEmpty()
        ) {

            listState
                .animateScrollToItem(
                    lazyIndexFor(
                        selectedIndex
                    )
                )
        }
    }

    /*
     * Overlays take key focus while they are up; hand it back to the
     * hub list once they close.
     */
    LaunchedEffect(
        mailPreviewItem == null &&
                callActionsItem == null
    ) {
        if (
            mailPreviewItem == null &&
            callActionsItem == null
        ) {
            runCatching {
                focusRequester.requestFocus()
            }
        }
    }

    fun openItem(
        item: HubDisplayItem
    ) {

        when (
            item.type
        ) {

            HubItemType.MESSAGE -> {

                /*
                 * A text that arrived as a notification lives in the
                 * messaging app, so tapping it goes there rather than
                 * opening a copy inside the launcher.
                 */
                val notificationKey =
                    item.notificationKey

                if (
                    notificationKey != null
                ) {

                    NotificationCenter
                        .openNotification(
                            key =
                                notificationKey,
                            context =
                                context
                        )

                    return
                }

                val phoneNumber =
                    item.phoneNumber
                        ?: return

                val contact =
                    Contact(
                        name =
                            item.title,
                        phoneNumber =
                            phoneNumber
                    )

                /*
                 * When another app owns SMS, open the conversation
                 * there. For a group, every participant except this
                 * phone goes into the recipient list.
                 */
                val handedOff =
                    if (
                        item.isGroup
                    ) {
                        val ownNumber =
                            OwnNumberStore(
                                context
                            )
                                .get()
                                ?.let {
                                    normalizePhoneNumber(
                                        it
                                    )
                                }

                        val others =
                            item.threadKey
                                .split(",")
                                .map {
                                    it.trim()
                                }
                                .filter {
                                    it.isNotBlank() &&
                                            normalizePhoneNumber(
                                                it
                                            ) != ownNumber
                                }

                        others.isNotEmpty() &&
                                openThreadInMessagingAppIfNotDefault(
                                    context,
                                    others.joinToString(
                                        ";"
                                    )
                                )
                    } else {
                        openThreadInMessagingAppIfNotDefault(
                            context,
                            phoneNumber
                        )
                    }

                if (
                    handedOff
                ) {
                    return
                }

                /*
                 * A group carries its whole participant list through,
                 * so the conversation screen can show who said what
                 * and reply to everyone.
                 */
                if (
                    item.isGroup
                ) {

                    onOpenThread(
                        ConversationThread(
                            threadKey =
                                item.threadKey,
                            title =
                                item.title,
                            participants =
                                item.threadKey
                                    .split(",")
                                    .filter {
                                        it.isNotBlank()
                                    },
                            isGroup =
                                true
                        ),
                        contact
                    )

                } else {

                    onOpenConversation(
                        contact
                    )
                }
            }

            HubItemType.EMAIL -> {

                val mailMessage =
                    item.mailMessage

                if (
                    mailMessage != null
                ) {
                    onOpenMail(
                        mailMessage
                    )
                } else {
                    /*
                     * An email from another mail app: show the
                     * Daylight preview card, which hands the full
                     * email to that app.
                     */
                    if (
                        item.notificationKey != null
                    ) {
                        mailPreviewItem =
                            item
                    }
                }
            }

            HubItemType.CALL -> {

                /*
                 * Calls open an action sheet rather than dialling
                 * straight away, so a missed call can be answered with
                 * a text just as easily as a callback.
                 */
                if (
                    item.phoneNumber != null
                ) {
                    callActionsItem =
                        item
                }
            }

            HubItemType.NOTIFICATION -> {

                item.notificationKey
                    ?.let {
                        NotificationCenter
                            .openNotification(
                                it
                            )
                    }
            }
        }
    }

    /*
     * Used by the keyboard path, where the selected index is already
     * settled. Clicks pass their own item instead, because calling
     * onSelectedIndexChange and then reading selectedIndex in the same
     * lambda reads the previous value — state has not recomposed yet.
     * That was opening whichever row had been selected before rather
     * than the one just tapped.
     */
    fun openSelectedItem() {

        filteredItems
            .getOrNull(
                selectedIndex
            )
            ?.let {
                openItem(
                    it
                )
            }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        BackgroundBlack
                    )
                    .padding(
                        horizontal =
                            DaylightPageHorizontal,
                        vertical =
                            DaylightPageVertical
                    )
                    .focusRequester(
                        focusRequester
                    )
                    .focusable()
                    .onPreviewKeyEvent {
                            event ->

                        if (
                            event.type !=
                            KeyEventType.KeyDown
                        ) {

                            false

                        } else {

                            when (
                                event.key
                            ) {

                                Key.DirectionDown -> {

                                    if (
                                        filteredItems.isNotEmpty()
                                    ) {

                                        onSelectedIndexChange(
                                            (
                                                    selectedIndex + 1
                                                    )
                                                .coerceAtMost(
                                                    filteredItems.lastIndex
                                                )
                                        )
                                    }

                                    true
                                }

                                Key.DirectionUp -> {

                                    onSelectedIndexChange(
                                        (
                                                selectedIndex - 1
                                                )
                                            .coerceAtLeast(
                                                0
                                            )
                                    )

                                    true
                                }

                                Key.DirectionRight -> {

                                    val categories =
                                        HubCategory.entries

                                    val index =
                                        categories.indexOf(
                                            selectedCategory
                                        )

                                    onSelectedCategoryChange(
                                        categories[
                                            (
                                                    index + 1
                                                    )
                                                .coerceAtMost(
                                                    categories.lastIndex
                                                )
                                        ]
                                    )

                                    onSelectedIndexChange(
                                        0
                                    )

                                    true
                                }

                                Key.DirectionLeft -> {

                                    val categories =
                                        HubCategory.entries

                                    val index =
                                        categories.indexOf(
                                            selectedCategory
                                        )

                                    onSelectedCategoryChange(
                                        categories[
                                            (
                                                    index - 1
                                                    )
                                                .coerceAtLeast(
                                                    0
                                                )
                                        ]
                                    )

                                    onSelectedIndexChange(
                                        0
                                    )

                                    true
                                }

                                Key.Enter -> {

                                    openSelectedItem()
                                    true
                                }

                                Key.Escape -> {

                                    onBack()
                                    true
                                }

                                else ->
                                    false
                            }
                        }
                    }
        ) {

            /*
             * Daylight header: big "Hub" title right of the camera, with
             * the unread count for the current tab underneath.
             */
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = DaylightCameraLane),
                verticalAlignment =
                    Alignment.Top
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "Hub",
                        color = PrimaryText,
                        fontSize = 24.sp,
                        lineHeight = 25.sp,
                        letterSpacing = (-0.7).sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text =
                            buildAnnotatedString {
                                if (hubSubtitleAccent.isNotEmpty()) {
                                    withStyle(
                                        SpanStyle(
                                            color = AccentOrange,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    ) {
                                        append(hubSubtitleAccent)
                                    }
                                }
                                append(hubSubtitleRest)
                            },
                        color = SecondaryText,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontFamily = InterfaceFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                /*
                 * Clears whatever the current tab is showing rather
                 * than the whole hub, so it is predictable from
                 * wherever you happen to be.
                 */
                if (
                    filteredItems.isNotEmpty()
                ) {

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Clear all",
                        color = SecondaryText,
                        fontSize = 8.5.sp,
                        lineHeight = 10.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier =
                            Modifier
                                .padding(top = 4.dp)
                                .background(
                                    SurfaceBlack,
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable {
                                    clearItems(
                                        filteredItems
                                    )
                                }
                                .padding(
                                    horizontal = 9.dp,
                                    vertical = 4.dp
                                )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            /*
             * Text tabs; the selected one in full text colour with an
             * accent underline the width of its label.
             */
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(15.dp)
            ) {

                HubCategory.entries.forEach { category ->

                    val selected =
                        category == selectedCategory

                    val categoryLabel =
                        when (category) {
                            HubCategory.ALL -> "All"
                            HubCategory.MESSAGES -> "Messages"
                            HubCategory.CALLS -> "Calls"
                            HubCategory.EMAIL -> "Email"
                            HubCategory.NOTIFICATIONS -> "Noti"
                        }

                    Column(
                        modifier =
                            Modifier
                                .width(IntrinsicSize.Max)
                                .clickable {

                                    onSelectedCategoryChange(
                                        category
                                    )

                                    onSelectedIndexChange(
                                        0
                                    )
                                }
                    ) {

                        Text(
                            text = categoryLabel,
                            color =
                                if (selected) PrimaryText
                                else SecondaryText,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontFamily = InterfaceFont,
                            fontWeight =
                                if (selected) FontWeight.SemiBold
                                else FontWeight.Normal,
                            maxLines = 1
                        )

                        Spacer(Modifier.height(5.dp))

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.6.dp)
                                    .background(
                                        if (selected) AccentOrange
                                        else Color.Transparent
                                    )
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (
                !notificationAccessEnabled
            ) {

                HubPermissionBanner(
                    text = "Notifications hidden · tap to allow notification access",
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                                )
                            )
                        } catch (
                            _: Exception
                        ) {
                        }
                    }
                )

                Spacer(Modifier.height(6.dp))
            }

            if (
                !hasCallLogPermission
            ) {

                HubPermissionBanner(
                    text = "Calls hidden · tap to allow call history",
                    onClick = {
                        onRequestCallLogPermission()
                    }
                )

                Spacer(Modifier.height(6.dp))
            }

            if (
                filteredItems.isEmpty()
            ) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(
                                1f
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    /*
                     * An empty email list usually means mail was never
                     * authorized rather than that the inbox is clear,
                     * so say which and offer the way out.
                     */
                    val mailTabNeedsConnecting =
                        mailNeedsAuthorization &&
                                (
                                        selectedCategory ==
                                                HubCategory.EMAIL ||
                                                selectedCategory ==
                                                HubCategory.ALL
                                        )

                    if (
                        mailTabNeedsConnecting
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "Mail is not connected",
                                color = SecondaryText,
                                fontSize = 10.sp,
                                fontFamily = InterfaceFont
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Sign in",
                                color = BackgroundBlack,
                                fontSize = 10.sp,
                                fontFamily = InterfaceFont,
                                fontWeight = FontWeight.SemiBold,
                                modifier =
                                    Modifier
                                        .background(
                                            AccentOrange,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            onConnectMail()
                                        }
                                        .padding(
                                            horizontal = 14.dp,
                                            vertical = 7.dp
                                        )
                            )
                        }

                    } else {

                        Text(
                            text = "Nothing here",
                            color = SecondaryText,
                            fontSize = 10.sp,
                            fontFamily = InterfaceFont
                        )
                    }
                }

            } else {

                LazyColumn(
                    state =
                        listState,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(
                                1f
                            )
                ) {

                    /*
                     * Section headers are their own lazy items but not
                     * part of the keyboard index: rows use one flat
                     * index across both sections (see lazyIndexFor).
                     */
                    if (
                        nowItems.isNotEmpty()
                    ) {

                        item(
                            key = "hub-section-now"
                        ) {
                            HubSectionHeader(
                                text =
                                    if (selectedCategory == HubCategory.EMAIL) "New"
                                    else "Now",
                                first = true
                            )
                        }

                        itemsIndexed(
                            items =
                                nowItems,
                            key = { _, item ->
                                item.id
                            }
                        ) { index, item ->

                            Box(
                                /*
                                 * Cascade: every item is its own card in its
                                 * kind's colour, with a small gap between.
                                 */
                                modifier =
                                    Modifier.padding(
                                        bottom = 6.dp
                                    )
                            ) {
                                HubRow(
                                    item =
                                        item,
                                    cascadeColor =
                                        cascadeColorForHubItem(
                                            item
                                        ),
                                    selected =
                                        index ==
                                                selectedIndex,
                                    onClick = {

                                        onSelectedIndexChange(
                                            index
                                        )

                                        openItem(
                                            item
                                        )
                                    },
                                    onDismiss = {
                                        clearItems(
                                            listOf(
                                                item
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (
                        earlierItems.isNotEmpty()
                    ) {

                        item(
                            key = "hub-section-earlier"
                        ) {
                            HubSectionHeader(
                                text = "Earlier",
                                first = nowItems.isEmpty()
                            )
                        }

                        itemsIndexed(
                            items =
                                earlierItems,
                            key = { _, item ->
                                item.id
                            }
                        ) { index, item ->

                            val flatIndex =
                                nowCount + index

                            Box(
                                /*
                                 * Cascade: every item is its own card in its
                                 * kind's colour, with a small gap between.
                                 */
                                modifier =
                                    Modifier.padding(
                                        bottom = 6.dp
                                    )
                            ) {
                                HubRow(
                                    item =
                                        item,
                                    cascadeColor =
                                        cascadeColorForHubItem(
                                            item
                                        ),
                                    selected =
                                        flatIndex ==
                                                selectedIndex,
                                    onClick = {

                                        onSelectedIndexChange(
                                            flatIndex
                                        )

                                        openItem(
                                            item
                                        )
                                    },
                                    onDismiss = {
                                        clearItems(
                                            listOf(
                                                item
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (
                        selectedCategory == HubCategory.EMAIL
                    ) {

                        item(
                            key = "hub-email-footer"
                        ) {
                            Text(
                                text = "Read an email in its app and it drops off this list",
                                color = SecondaryText,
                                fontSize = 8.5.sp,
                                lineHeight = 11.sp,
                                fontFamily = InterfaceFont,
                                textAlign = TextAlign.Center,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = 12.dp,
                                            bottom = 4.dp
                                        )
                            )
                        }
                    }
                }
            }

        }

        val activeCallItem =
            callActionsItem

        if (
            activeCallItem?.phoneNumber != null
        ) {

            CallActionsOverlay(
                displayName =
                    activeCallItem.title,
                phoneNumber =
                    activeCallItem.phoneNumber,
                onText = {

                    callActionsItem =
                        null

                    /*
                     * When another app owns SMS, reply in that app's
                     * thread; Prompt's own thread is only used when
                     * Prompt is the default SMS app.
                     */
                    if (
                        !openThreadInMessagingAppIfNotDefault(
                            context,
                            activeCallItem.phoneNumber
                        )
                    ) {
                        onOpenConversation(
                            Contact(
                                name =
                                    activeCallItem.title,
                                phoneNumber =
                                    activeCallItem.phoneNumber
                            )
                        )
                    }
                },
                onCall = {

                    callActionsItem =
                        null

                    onDial(
                        activeCallItem.phoneNumber
                    )
                },
                onDismiss = {
                    callActionsItem =
                        null
                }
            )
        }

        /*
         * Rendered outside the hub's focused column so its keys go to
         * the preview card rather than the list underneath.
         */
        mailPreviewItem?.let { previewItem ->
            DaylightMailPreview(
                item = previewItem,
                onDismiss = {
                    mailPreviewItem =
                        null
                }
            )
        }
    }
}

/*
 * "NOW" / "EARLIER" over a hub section card.
 */
@Composable
private fun HubSectionHeader(
    text: String,
    first: Boolean
) {
    DaylightSectionLabel(
        text = text,
        modifier =
            Modifier.padding(
                top = if (first) 0.dp else 12.dp,
                bottom = 7.dp
            )
    )
}

/*
 * The quiet card asking for notification or call-log access.
 */
@Composable
private fun HubPermissionBanner(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = AccentOrange,
        fontSize = 8.5.sp,
        lineHeight = 10.sp,
        fontFamily = InterfaceFont,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    SurfaceBlack,
                    RoundedCornerShape(12.dp)
                )
                .clickable {
                    onClick()
                }
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )
    )
}

/*
 * One row's slice of a white rounded section card. Lazy lists cannot
 * wrap several items in one background, so each row draws its part of
 * the card: rounded top on the first, rounded bottom on the last, and
 * a hairline under every row but the last.
 */
@Composable
internal fun DaylightCardSegment(
    isFirst: Boolean,
    isLast: Boolean,
    content: @Composable () -> Unit
) {

    val radius =
        14.dp

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    SurfaceBlack,
                    RoundedCornerShape(
                        topStart = if (isFirst) radius else 0.dp,
                        topEnd = if (isFirst) radius else 0.dp,
                        bottomStart = if (isLast) radius else 0.dp,
                        bottomEnd = if (isLast) radius else 0.dp
                    )
                )
                .padding(
                    start = 6.dp,
                    end = 6.dp,
                    top = if (isFirst) 3.dp else 0.dp,
                    bottom = if (isLast) 3.dp else 0.dp
                )
    ) {

        content()

        if (!isLast) {
            Box(
                modifier =
                    Modifier.padding(
                        horizontal = 6.dp
                    )
            ) {
                DaylightRowDivider()
            }
        }
    }
}

/*
 * "now", "14m", "3h", then the date. With long = true: "14 minutes
 * ago", as the mail preview shows it.
 */
internal fun hubRelativeTime(
    timestamp: Long,
    long: Boolean = false
): String {

    val elapsed =
        (System.currentTimeMillis() - timestamp)
            .coerceAtLeast(0L)

    val minutes =
        TimeUnit.MILLISECONDS.toMinutes(elapsed)

    val hours =
        TimeUnit.MILLISECONDS.toHours(elapsed)

    return when {
        minutes < 1L ->
            if (long) "just now" else "now"

        minutes < 60L ->
            if (!long) "${minutes}m"
            else if (minutes == 1L) "1 minute ago"
            else "$minutes minutes ago"

        hours < 24L ->
            if (!long) "${hours}h"
            else if (hours == 1L) "1 hour ago"
            else "$hours hours ago"

        else ->
            formatInboxTime(timestamp)
    }
}

/*
 * Decoded contact thumbnails, keyed by photo URI.
 *
 * The hub rebuilds its item list whenever messages, calls or
 * notifications change, so without this the same faces would be
 * decoded from the content provider on every pass and again on every
 * scroll. Thumbnails are small and the number of correspondents is
 * bounded, so holding them for the process lifetime is cheap.
 */
/*
 * Diagnostic for contact photos. With this on, message and call rows
 * lacking an avatar show "n" when no photo URI resolved and "d" when
 * a URI resolved but the image would not decode. Left in place
 * because it costs nothing off and answers the question instantly.
 */
private const val DebugContactPhotos =
    false

private val contactPhotoCache =
    mutableMapOf<String, ImageBitmap?>()

@Composable
internal fun rememberContactPhoto(
    photoUri: String?
): ImageBitmap? {

    val context =
        LocalContext.current

    if (
        photoUri.isNullOrBlank()
    ) {
        return null
    }

    return remember(
        photoUri
    ) {

        contactPhotoCache.getOrPut(
            photoUri
        ) {

            runCatching {

                context.contentResolver
                    .openInputStream(
                        Uri.parse(
                            photoUri
                        )
                    )
                    ?.use { stream ->

                        BitmapFactory
                            .decodeStream(
                                stream
                            )
                            ?.asImageBitmap()
                    }
            }
                .getOrNull()
        }
    }
}

@Composable
internal fun HubRow(
    item: HubDisplayItem,
    selected: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    /*
     * Cascade: draw the row as a card in this colour with dark ink
     * text. Null keeps the plain row (used inside other cards).
     */
    cascadeColor: Color? = null
) {
    val onCard =
        cascadeColor != null
    val mainText =
        if (onCard) CascadeColors.Ink else PrimaryText
    val mutedText =
        if (onCard) CascadeColors.Ink.copy(alpha = 0.72f) else SecondaryText

    val contactPhoto =
        rememberContactPhoto(
            item.photoUri
        )

    val emailParts =
        if (item.type == HubItemType.EMAIL) {
            rememberHubEmailParts(item)
        } else {
            null
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .then(
                    if (cascadeColor != null) {
                        Modifier
                            .background(
                                cascadeColor,
                                RoundedCornerShape(16.dp)
                            )
                            .then(
                                if (selected) {
                                    Modifier.border(
                                        1.5.dp,
                                        CascadeColors.Ink,
                                        RoundedCornerShape(16.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    } else {
                        Modifier.background(
                            if (selected) AccentOrange.copy(alpha = 0.08f)
                            else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                    }
                )
                .clickable {
                    onClick()
                }
                .padding(
                    start = if (onCard) 10.dp else 6.dp,
                    end = if (onCard) 6.dp else 2.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
        verticalAlignment =
            if (emailParts != null) Alignment.Top
            else Alignment.CenterVertically
    ) {

        HubAvatar(
            item = item,
            photo = contactPhoto,
            cardColor = cascadeColor
        )

        Spacer(Modifier.width(10.dp))

        if (emailParts != null) {

            /*
             * Email, as in the inbox mockup: sender and time, then the
             * subject, then one line of preview, with the mail app's
             * name as a small grey tag on the right.
             */
            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        color = mainText,
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(6.dp))

                    HubTimeText(item, onCard)
                }

                if (emailParts.subject.isNotBlank()) {
                    Text(
                        text = emailParts.subject,
                        color = mainText,
                        fontSize = 10.sp,
                        lineHeight = 12.5.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (emailParts.body.isNotBlank()) {
                    Text(
                        text = emailParts.body.replace('\n', ' '),
                        color = mutedText,
                        fontSize = 9.sp,
                        lineHeight = 11.5.sp,
                        fontFamily = InterfaceFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (emailParts.tag != null) {

                Spacer(Modifier.width(8.dp))

                Text(
                    text = emailParts.tag,
                    color = mutedText,
                    fontSize = 7.5.sp,
                    lineHeight = 9.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .align(Alignment.CenterVertically)
                            .widthIn(max = 70.dp)
                            .background(
                                if (onCard) CascadeColors.Ink.copy(alpha = 0.12f)
                                else BackgroundBlack,
                                RoundedCornerShape(999.dp)
                            )
                            .padding(
                                horizontal = 6.5.dp,
                                vertical = 2.5.dp
                            )
                )
            }

        } else {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = item.title,
                    color = mainText,
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(1.dp))

                Text(
                    text =
                        if (item.type == HubItemType.CALL) {
                            item.preview.replaceFirstChar { it.uppercase() }
                        } else {
                            item.preview.replace('\n', ' ')
                        },
                    color =
                        if (item.missed && !onCard) ProductivityRed
                        else mutedText,
                    fontSize = 9.sp,
                    lineHeight = 11.5.sp,
                    fontFamily = InterfaceFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            HubTimeText(item, onCard)
        }

        /*
         * Clear, kept quiet. Its own clickable consumes the tap, so
         * the row underneath does not open.
         */
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterVertically)
                    .size(18.dp)
                    .clickable {
                        onDismiss()
                    },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "clear",
                tint =
                    if (onCard) CascadeColors.Ink.copy(alpha = 0.55f)
                    else TertiaryText.copy(alpha = 0.7f),
                modifier = Modifier.size(9.dp)
            )
        }
    }
}

/*
 * "2m" in the accent colour while unread, muted once seen.
 */
@Composable
private fun HubTimeText(
    item: HubDisplayItem,
    onCard: Boolean = false
) {
    Text(
        text = hubRelativeTime(item.timestamp),
        color =
            when {
                onCard -> CascadeColors.Ink
                item.unread -> AccentOrange
                else -> SecondaryText
            },
        fontSize = 8.5.sp,
        lineHeight = 10.sp,
        fontFamily = InterfaceFont,
        fontWeight =
            if (item.unread) FontWeight.SemiBold
            else FontWeight.Normal,
        maxLines = 1
    )
}

/*
 * The 30 dp circle at the start of a hub row: the contact's photo when
 * there is one, otherwise initials (filled for an unread text), the
 * member count for a group, or an icon in a tinted circle. Missed
 * calls are tinted orange.
 */
@Composable
private fun HubAvatar(
    item: HubDisplayItem,
    photo: ImageBitmap?,
    cardColor: Color? = null
) {

    val avatarSize =
        30.dp

    if (photo != null) {
        Image(
            bitmap = photo,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
        )
        return
    }

    val initials =
        contactInitials(
            item.title
        )

    if (cardColor != null) {
        /*
         * Cascade card: an ink circle with initials (or the kind's
         * icon) in the card's own colour.
         */
        Box(
            modifier =
                Modifier
                    .size(avatarSize)
                    .background(
                        CascadeColors.Ink,
                        CircleShape
                    ),
            contentAlignment = Alignment.Center
        ) {
            val showInitials =
                (item.type == HubItemType.MESSAGE ||
                        item.type == HubItemType.EMAIL ||
                        item.type == HubItemType.CALL) &&
                        initials != "#" &&
                        !item.isGroup
            if (showInitials) {
                Text(
                    text = initials,
                    color = cardColor,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            } else {
                Icon(
                    imageVector =
                        when (item.type) {
                            HubItemType.CALL -> Icons.Outlined.Call
                            HubItemType.EMAIL -> Icons.Outlined.Email
                            HubItemType.MESSAGE -> Icons.Outlined.Message
                            HubItemType.NOTIFICATION -> Icons.Outlined.Notifications
                        },
                    contentDescription = null,
                    tint = cardColor,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        return
    }

    when {

        item.type == HubItemType.MESSAGE &&
                item.isGroup -> {

            val memberCount =
                item.threadKey
                    .split(",")
                    .count {
                        it.isNotBlank()
                    }

            Box(
                modifier =
                    Modifier
                        .size(avatarSize)
                        .background(
                            BorderGray,
                            CircleShape
                        ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text =
                        if (memberCount > 0) memberCount.toString()
                        else "#",
                    color = PrimaryText,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        (item.type == HubItemType.MESSAGE ||
                item.type == HubItemType.EMAIL) &&
                initials != "#" -> {

            DaylightAvatar(
                label = initials,
                filled =
                    item.type == HubItemType.MESSAGE &&
                            item.unread,
                sizeDp = 30
            )
        }

        else -> {

            val tint =
                if (item.missed) ProductivityRed
                else AccentOrange

            Box(
                modifier =
                    Modifier
                        .size(avatarSize)
                        .background(
                            tint.copy(alpha = 0.14f),
                            CircleShape
                        ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector =
                        when (item.type) {
                            HubItemType.CALL -> Icons.Outlined.Call
                            HubItemType.EMAIL -> Icons.Outlined.Email
                            HubItemType.MESSAGE -> Icons.Outlined.Message
                            HubItemType.NOTIFICATION -> Icons.Outlined.Notifications
                        },
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/*
 * An email row's pieces. Mail-app notifications arrive as
 * "AppName · text"; the app name becomes the tag and the text splits
 * into subject and preview.
 */
private data class HubEmailParts(
    val tag: String?,
    val subject: String,
    val body: String
)

@Composable
private fun rememberHubEmailParts(
    item: HubDisplayItem
): HubEmailParts =
    remember(
        item.notificationKey,
        item.preview,
        item.mailMessage
    ) {

        val mailMessage =
            item.mailMessage

        if (mailMessage != null) {

            HubEmailParts(
                tag = "Gmail",
                subject = mailMessage.subject,
                body = mailMessage.snippet
            )

        } else {

            val appName =
                item.notificationKey
                    ?.let { key ->
                        NotificationCenter
                            .notifications
                            .value
                            .firstOrNull {
                                it.key == key
                            }
                            ?.appName
                    }
                    .orEmpty()
                    .trim()

            val prefix =
                "$appName · "

            val tagged =
                appName.isNotBlank() &&
                        (item.preview.startsWith(prefix) ||
                                item.preview == appName)

            val text =
                when {
                    !tagged -> item.preview
                    item.preview == appName -> ""
                    else -> item.preview.removePrefix(prefix)
                }

            val (subject, body) =
                splitMailNotificationText(
                    text
                )

            HubEmailParts(
                tag = if (tagged) appName else null,
                subject = subject,
                body = body
            )
        }
    }

fun formatCallDuration(
    seconds: Long
): String {

    val minutes =
        seconds / 60

    val remainingSeconds =
        seconds % 60

    return if (
        minutes > 0
    ) {
        "${minutes}m ${remainingSeconds}s"
    } else {
        "${remainingSeconds}s"
    }
}

@Composable
fun MailInboxScreen(
    messages: List<MailMessage>,
    loading: Boolean,
    onOpenMessage: (MailMessage) -> Unit,
    onTrashMessage: (MailMessage) -> Unit,
    onMarkUnread: (MailMessage) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {

    val focusRequester =
        remember {
            FocusRequester()
        }

    val listState =
        rememberLazyListState()

    var selectedIndex by remember {
        mutableIntStateOf(
            0
        )
    }

    LaunchedEffect(Unit) {

        focusRequester
            .requestFocus()
    }

    LaunchedEffect(
        messages.size
    ) {

        if (
            messages.isEmpty()
        ) {

            selectedIndex =
                0

        } else if (
            selectedIndex >
            messages.lastIndex
        ) {

            selectedIndex =
                messages.lastIndex
        }
    }

    LaunchedEffect(
        selectedIndex
    ) {

        if (
            messages.isNotEmpty()
        ) {

            listState
                .animateScrollToItem(
                    selectedIndex
                )
        }
    }

    val unreadCount =
        messages.count {
            it.unread
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .padding(
                    horizontal =
                        DaylightPageHorizontal,
                    vertical =
                        DaylightPageVertical
                )
                .focusRequester(
                    focusRequester
                )
                .focusable()
                .onPreviewKeyEvent {
                        event ->

                    if (
                        event.type !=
                        KeyEventType.KeyDown
                    ) {

                        false

                    } else {

                        when (
                            event.key
                        ) {

                            Key.DirectionDown -> {

                                if (
                                    messages.isNotEmpty()
                                ) {

                                    selectedIndex =
                                        (
                                                selectedIndex + 1
                                                )
                                            .coerceAtMost(
                                                messages.lastIndex
                                            )
                                }

                                true
                            }

                            Key.DirectionUp -> {

                                selectedIndex =
                                    (
                                            selectedIndex - 1
                                            )
                                        .coerceAtLeast(
                                            0
                                        )

                                true
                            }

                            Key.Enter -> {

                                messages
                                    .getOrNull(
                                        selectedIndex
                                    )
                                    ?.let {

                                        onOpenMessage(
                                            it
                                        )
                                    }

                                true
                            }

                            Key.Escape -> {

                                onBack()

                                true
                            }

                            else -> {

                                if (
                                    event.key ==
                                    Key.R
                                ) {

                                    onRefresh()

                                    true

                                } else {

                                    false
                                }
                            }
                        }
                    }
                }
    ) {

        /*
         * Daylight header: title right of the camera, unread count
         * underneath, a round close button on the right.
         */
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = DaylightCameraLane),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "Mail",
                    color = PrimaryText,
                    fontSize = 24.sp,
                    lineHeight = 25.sp,
                    letterSpacing = (-0.7).sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text =
                        buildAnnotatedString {
                            when {
                                loading && messages.isEmpty() ->
                                    append("Loading Gmail")

                                unreadCount > 0 -> {
                                    withStyle(
                                        SpanStyle(
                                            color = AccentOrange,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    ) {
                                        append("$unreadCount unread")
                                    }
                                    append(" in Gmail")
                                }

                                else ->
                                    append("Gmail · all read")
                            }
                        },
                    color = SecondaryText,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontFamily = InterfaceFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            MailRoundIconButton(
                imageVector = Icons.Outlined.Close,
                contentDescription = "close",
                onClick = onBack
            )
        }

        Spacer(Modifier.height(12.dp))

        if (
            messages.isEmpty()
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(
                            1f
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        if (
                            loading
                        ) {
                            "Loading mail"
                        } else {
                            "No inbox mail"
                        },
                    color =
                        SecondaryText,
                    fontSize =
                        10.sp,
                    fontFamily =
                        InterfaceFont
                )
            }

        } else {

            LazyColumn(
                state =
                    listState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(
                            1f
                        )
            ) {

                itemsIndexed(
                    items =
                        messages,
                    key = {
                            _,
                            message ->

                        message.id
                    }
                ) {
                        index,
                        message ->

                    DaylightCardSegment(
                        isFirst = index == 0,
                        isLast = index == messages.lastIndex
                    ) {
                        MailInboxRow(
                            message =
                                message,
                            selected =
                                index ==
                                        selectedIndex,
                            onClick = {

                                selectedIndex =
                                    index

                                onOpenMessage(
                                    message
                                )
                            },
                            onTrash = {

                                onTrashMessage(
                                    message
                                )
                            },
                            onMarkUnread = {

                                onMarkUnread(
                                    message
                                )
                            }
                        )
                    }
                }
            }
        }

    }
}

@Composable
fun MailInboxRow(
    message: MailMessage,
    selected: Boolean,
    onClick: () -> Unit,
    onTrash: () -> Unit,
    onMarkUnread: () -> Unit
) {

    var horizontalOffset by remember(
        message.id
    ) {
        mutableStateOf(
            0f
        )
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
    ) {

        /*
         * Revealed under the row while it is swiped: right marks
         * unread, left trashes.
         */
        Text(
            text =
                "Unread",
            color =
                AccentOrange,
            fontSize =
                8.5.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.SemiBold,
            modifier =
                Modifier
                    .align(
                        Alignment.CenterStart
                    )
                    .padding(
                        start =
                            10.dp
                    )
        )

        Text(
            text =
                "Trash",
            color =
                ProductivityRed,
            fontSize =
                8.5.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.SemiBold,
            modifier =
                Modifier
                    .align(
                        Alignment.CenterEnd
                    )
                    .padding(
                        end =
                            10.dp
                    )
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .offset {
                        IntOffset(
                            horizontalOffset
                                .roundToInt(),
                            0
                        )
                    }
                    .background(
                        SurfaceBlack,
                        RoundedCornerShape(10.dp)
                    )
                    .background(
                        if (selected) AccentOrange.copy(alpha = 0.08f)
                        else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .pointerInput(
                        message.id
                    ) {

                        detectHorizontalDragGestures(
                            onHorizontalDrag = {
                                    _,
                                    dragAmount ->

                                horizontalOffset =
                                    (
                                            horizontalOffset +
                                                    dragAmount
                                            )
                                        .coerceIn(
                                            -220f,
                                            220f
                                        )
                            },
                            onDragEnd = {

                                if (
                                    horizontalOffset <=
                                    -110f
                                ) {

                                    onTrash()

                                } else if (
                                    horizontalOffset >=
                                    110f
                                ) {

                                    onMarkUnread()
                                }

                                horizontalOffset =
                                    0f
                            },
                            onDragCancel = {

                                horizontalOffset =
                                    0f
                            }
                        )
                    }
                    .clickable {

                        onClick()
                    }
                    .padding(
                        start = 6.dp,
                        end = 6.dp,
                        top = 9.dp,
                        bottom = 9.dp
                    ),
            verticalAlignment =
                Alignment.Top
        ) {

            DaylightAvatar(
                label =
                    contactInitials(
                        message.sender
                    ),
                filled =
                    message.unread,
                sizeDp =
                    30
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            message.sender,
                        color =
                            PrimaryText,
                        fontSize =
                            10.5.sp,
                        lineHeight =
                            13.sp,
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            if (
                                message.unread
                            ) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Medium
                            },
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis,
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    if (
                        message.important
                    ) {

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = "Important",
                            color = AccentOrange,
                            fontSize = 7.5.sp,
                            lineHeight = 9.sp,
                            fontFamily = InterfaceFont,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .background(
                                        AccentOrange.copy(alpha = 0.10f),
                                        RoundedCornerShape(999.dp)
                                    )
                                    .padding(
                                        horizontal = 6.dp,
                                        vertical = 2.dp
                                    )
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text =
                            hubRelativeTime(
                                message.timestamp
                            ),
                        color =
                            if (message.unread) AccentOrange
                            else SecondaryText,
                        fontSize =
                            8.5.sp,
                        lineHeight =
                            10.sp,
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            if (message.unread) FontWeight.SemiBold
                            else FontWeight.Normal,
                        maxLines =
                            1
                    )
                }

                if (
                    message.subject.isNotBlank()
                ) {
                    Text(
                        text =
                            message.subject,
                        color =
                            PrimaryText,
                        fontSize =
                            10.sp,
                        lineHeight =
                            12.5.sp,
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            FontWeight.Medium,
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }

                Text(
                    text =
                        message.snippet,
                    color =
                        SecondaryText,
                    fontSize =
                        9.sp,
                    lineHeight =
                        11.5.sp,
                    fontFamily =
                        InterfaceFont,
                    maxLines =
                        1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MailDetailScreen(
    message: MailMessage,
    onReply: (MailMessage) -> Unit,
    onReplyAll: (MailMessage) -> Unit,
    onForward: (MailMessage) -> Unit,
    onArchive: (MailMessage) -> Unit,
    onSnooze24Hours: (MailMessage) -> Unit,
    onTrash: (MailMessage) -> Unit,
    onBack: () -> Unit,
    floatingMode: Boolean = false
) {

    val focusRequester =
        remember {
            FocusRequester()
        }

    var showMoreMenu by
    remember {
        mutableStateOf(
            false
        )
    }

    LaunchedEffect(Unit) {

        focusRequester
            .requestFocus()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .padding(
                    horizontal =
                        if (
                            floatingMode
                        ) {
                            12.dp
                        } else {
                            DaylightPageHorizontal
                        },
                    vertical =
                        if (
                            floatingMode
                        ) {
                            10.dp
                        } else {
                            DaylightPageVertical
                        }
                )
                .focusRequester(
                    focusRequester
                )
                .focusable()
                .onPreviewKeyEvent {
                        event ->

                    if (
                        event.type !=
                        KeyEventType.KeyDown
                    ) {

                        false

                    } else {

                        when (
                            event.key
                        ) {

                            Key.Escape -> {

                                onBack()
                                true
                            }

                            Key.R -> {

                                onReply(
                                    message
                                )
                                true
                            }

                            else ->
                                false
                        }
                    }
                }
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {

            /*
             * Header: sender with initials, the date underneath, round
             * back and more buttons.
             */
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start =
                                if (
                                    floatingMode
                                ) {
                                    0.dp
                                } else {
                                    DaylightCameraLane
                                }
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                DaylightAvatar(
                    label =
                        contactInitials(
                            message.sender
                        ),
                    filled =
                        true,
                    sizeDp =
                        33
                )

                Spacer(Modifier.width(9.dp))

                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(
                        text =
                            message.sender,
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
                            "Gmail · " +
                                    formatMailDate(
                                        message.timestamp
                                    ),
                        color =
                            SecondaryText,
                        fontSize =
                            8.5.sp,
                        lineHeight =
                            10.sp,
                        fontFamily =
                            InterfaceFont,
                        maxLines =
                            1
                    )
                }

                Spacer(Modifier.width(6.dp))

                MailRoundIconButton(
                    imageVector = Icons.Outlined.MoreHoriz,
                    contentDescription = "more",
                    background = SurfaceBlack,
                    onClick = {
                        showMoreMenu =
                            !showMoreMenu
                    }
                )

                Spacer(Modifier.width(6.dp))

                MailRoundIconButton(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "close",
                    background = SurfaceBlack,
                    onClick = onBack
                )
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(
                            1f
                        )
            ) {

                item {

                    DaylightCard(
                        horizontalPadding = 15.dp,
                        verticalPadding = 14.dp
                    ) {

                        Text(
                            text =
                                message.subject.ifBlank {
                                    "(no subject)"
                                },
                            color =
                                PrimaryText,
                            fontSize =
                                if (
                                    floatingMode
                                ) {
                                    15.sp
                                } else {
                                    14.sp
                                },
                            lineHeight =
                                if (
                                    floatingMode
                                ) {
                                    17.5.sp
                                } else {
                                    16.sp
                                },
                            letterSpacing =
                                (-0.3).sp,
                            fontFamily =
                                InterfaceFont,
                            fontWeight =
                                FontWeight.Bold
                        )

                        if (
                            message.important ||
                            message.unread
                        ) {

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text =
                                    buildList {

                                        if (
                                            message.important
                                        ) {
                                            add(
                                                "Important"
                                            )
                                        }

                                        if (
                                            message.unread
                                        ) {
                                            add(
                                                "Unread"
                                            )
                                        }
                                    }
                                        .joinToString(
                                            " · "
                                        ),
                                color =
                                    AccentOrange,
                                fontSize =
                                    8.sp,
                                fontFamily =
                                    InterfaceFont,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text =
                                message.body,
                            color =
                                PrimaryText,
                            fontSize =
                                if (
                                    floatingMode
                                ) {
                                    10.8.sp
                                } else {
                                    10.sp
                                },
                            lineHeight =
                                if (
                                    floatingMode
                                ) {
                                    15.6.sp
                                } else {
                                    14.5.sp
                                },
                            fontFamily =
                                InterfaceFont
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            if (
                                floatingMode
                            ) {
                                38.dp
                            } else {
                                36.dp
                            }
                        ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                MailDetailAction(
                    text =
                        "Reply",
                    primary =
                        true,
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    onClick = {
                        onReply(
                            message
                        )
                    }
                )

                MailDetailAction(
                    text =
                        "Reply all",
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    onClick = {
                        onReplyAll(
                            message
                        )
                    }
                )

                MailDetailAction(
                    text =
                        "Forward",
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    onClick = {
                        onForward(
                            message
                        )
                    }
                )
            }
        }

        if (
            showMoreMenu
        ) {

            /*
             * Transparent dismiss layer behind the small overflow menu.
             * Any tap outside the menu closes it immediately.
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(
                            showMoreMenu
                        ) {

                            detectTapGestures {

                                showMoreMenu =
                                    false
                            }
                        }
            )

            Box(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopEnd
                        )
                        .padding(
                            top =
                                36.dp,
                            end =
                                1.dp
                        )
                        .width(
                            130.dp
                        )
                        .background(
                            SurfaceBlack,
                            RoundedCornerShape(
                                12.dp
                            )
                        )
                        .border(
                            0.75.dp,
                            BorderGray,
                            RoundedCornerShape(
                                12.dp
                            )
                        )
                        .padding(
                            vertical =
                                4.dp
                        )
            ) {

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    MailMoreMenuAction(
                        text =
                            "Archive",
                        onClick = {

                            showMoreMenu =
                                false

                            onArchive(
                                message
                            )
                        }
                    )

                    MailMoreMenuAction(
                        text =
                            "Snooze 24 hours",
                        onClick = {

                            showMoreMenu =
                                false

                            onSnooze24Hours(
                                message
                            )
                        }
                    )

                    MailMoreMenuAction(
                        text =
                            "Trash",
                        onClick = {

                            showMoreMenu =
                                false

                            onTrash(
                                message
                            )
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun MailMoreMenuAction(
    text: String,
    onClick: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    32.dp
                )
                .clickable(
                    onClick =
                        onClick
                )
                .padding(
                    horizontal =
                        12.dp
                ),
        contentAlignment =
            Alignment.CenterStart
    ) {

        Text(
            text =
                text,
            color =
                PrimaryText,
            fontSize =
                9.5.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.Medium
        )
    }
}


/*
 * Reply / Reply all / Forward: rounded Daylight buttons, the first in
 * the accent colour.
 */
@Composable
private fun MailDetailAction(
    text: String,
    modifier: Modifier =
        Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {

    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .background(
                    if (primary) AccentOrange else SurfaceBlack,
                    RoundedCornerShape(12.dp)
                )
                .clickable(
                    onClick =
                        onClick
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                text,
            color =
                if (primary) BackgroundBlack else PrimaryText,
            fontSize =
                10.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.SemiBold,
            maxLines =
                1
        )
    }
}


fun formatMailDate(
    timestamp: Long
): String {

    return SimpleDateFormat(
        "MMM d · h:mm a",
        Locale.getDefault()
    )
        .format(
            Date(
                timestamp
            )
        )
        .lowercase()
}


@Composable
fun MailComposeScreen(
    draft: MailComposeDraft,
    statusText: String,
    onSend: (
        String,
        String,
        String
    ) -> Unit,
    onBack: () -> Unit
) {

    var toAddress by remember(
        draft
    ) {
        mutableStateOf(
            draft.toAddress
        )
    }

    var subject by remember(
        draft
    ) {
        mutableStateOf(
            draft.subject
        )
    }

    var body by remember(
        draft
    ) {
        mutableStateOf(
            draft.body
        )
    }

    var sendFocused by remember {
        mutableStateOf(
            false
        )
    }

    val recipientFocusRequester =
        remember {
            FocusRequester()
        }

    val subjectFocusRequester =
        remember {
            FocusRequester()
        }

    val bodyFocusRequester =
        remember {
            FocusRequester()
        }

    val sendFocusRequester =
        remember {
            FocusRequester()
        }

    LaunchedEffect(
        draft
    ) {

        when {

            draft.recipientEditable -> {

                recipientFocusRequester
                    .requestFocus()
            }

            draft.isReply -> {

                bodyFocusRequester
                    .requestFocus()
            }

            else -> {

                subjectFocusRequester
                    .requestFocus()
            }
        }
    }

    /*
     * "To Jordan": the first name of whoever it is going to.
     */
    val recipientName =
        (
                if (draft.recipientEditable) toAddress
                else draft.toLabel.ifBlank { draft.toAddress }
                )
            .trim()
            .substringBefore("@")
            .split(" ")
            .firstOrNull {
                it.isNotBlank()
            }
            .orEmpty()

    val composeKind =
        when {
            draft.isForward -> "Forward"
            draft.isReplyAll -> "Reply all"
            draft.isReply -> "Reply"
            else -> "New email"
        }

    /*
     * Wide enough for "Subject" on one line at the scaled text size.
     */
    val labelWidth =
        58.dp

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .padding(
                    horizontal = DaylightPageHorizontal,
                    vertical = DaylightPageVertical
                )
    ) {

        /*
         * Caption and title right of the camera, round close button
         * on the right.
         */
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = DaylightCameraLane),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "$composeKind · Gmail",
                    color = SecondaryText,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontFamily = InterfaceFont,
                    maxLines = 1
                )

                Text(
                    text =
                        if (recipientName.isBlank()) "New email"
                        else "To $recipientName",
                    color = PrimaryText,
                    fontSize = 17.sp,
                    lineHeight = 19.sp,
                    letterSpacing = (-0.3).sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            MailRoundIconButton(
                imageVector = Icons.Outlined.Close,
                contentDescription = "discard",
                background = SurfaceBlack,
                onClick = onBack
            )
        }

        Spacer(Modifier.height(10.dp))

        /*
         * One card: To, Subject, then the message. Cascade: sky
         * blue, the mail colour, with dark ink text.
         */
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        CascadeColors.Mail,
                        RoundedCornerShape(15.dp)
                    )
                    .padding(
                        start = 13.dp,
                        end = 13.dp,
                        top = 3.dp,
                        bottom = 11.dp
                    )
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp)
                        .padding(vertical = 8.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "To",
                    color = CascadeColors.Ink.copy(alpha = 0.72f),
                    fontSize = 9.sp,
                    fontFamily = InterfaceFont,
                    modifier = Modifier.width(labelWidth)
                )

                if (
                    draft.recipientEditable
                ) {

                    BasicTextField(
                        value =
                            toAddress,
                        onValueChange = {

                            toAddress =
                                it.replace(
                                    "\n",
                                    " "
                                )
                        },
                        singleLine =
                            true,
                        textStyle =
                            TextStyle(
                                color =
                                    CascadeColors.Ink,
                                fontSize =
                                    10.sp,
                                fontFamily =
                                    InterfaceFont
                            ),
                        cursorBrush =
                            SolidColor(
                                CascadeColors.Ink
                            ),
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (toAddress.isEmpty()) {
                                    Text(
                                        text = "name@example.com",
                                        color = CascadeColors.Ink.copy(alpha = 0.5f),
                                        fontSize = 10.sp,
                                        fontFamily = InterfaceFont,
                                        maxLines = 1
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier =
                            Modifier
                                .weight(1f)
                                .focusRequester(
                                    recipientFocusRequester
                                )
                                .onPreviewKeyEvent {
                                        event ->

                                    if (
                                        event.type !=
                                        KeyEventType.KeyDown
                                    ) {

                                        false

                                    } else {

                                        when (
                                            event.key
                                        ) {

                                            Key.Escape -> {

                                                onBack()
                                                true
                                            }

                                            Key.Enter,
                                            Key.DirectionDown -> {

                                                bodyFocusRequester
                                                    .requestFocus()
                                                true
                                            }

                                            Key.Tab -> {

                                                subjectFocusRequester
                                                    .requestFocus()
                                                true
                                            }

                                            else ->
                                                false
                                        }
                                    }
                                }
                    )

                } else {

                    Text(
                        text =
                            draft.toLabel.ifBlank {
                                draft.toAddress
                            },
                        color = CascadeColors.Ink,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .widthIn(max = 150.dp)
                                .background(
                                    CascadeColors.Ink.copy(alpha = 0.10f),
                                    RoundedCornerShape(999.dp)
                                )
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 3.dp
                                )
                    )

                    if (
                        draft.toLabel.isNotBlank() &&
                        !draft.toLabel.equals(
                            draft.toAddress,
                            ignoreCase =
                                true
                        )
                    ) {

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = draft.toAddress,
                            color = CascadeColors.Ink.copy(alpha = 0.5f),
                            fontSize = 8.5.sp,
                            fontFamily = InterfaceFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(0.75.dp)
                        .background(CascadeColors.Ink.copy(alpha = 0.15f))
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp)
                        .padding(vertical = 8.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Subject",
                    color = CascadeColors.Ink.copy(alpha = 0.72f),
                    fontSize = 9.sp,
                    fontFamily = InterfaceFont,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(labelWidth)
                )

                BasicTextField(
                    value = subject,
                    onValueChange = {

                        subject =
                            it.replace(
                                "\n",
                                " "
                            )
                    },
                    singleLine = true,
                    textStyle =
                        TextStyle(
                            color = CascadeColors.Ink,
                            fontSize = 10.5.sp,
                            fontFamily = InterfaceFont,
                            fontWeight = FontWeight.SemiBold
                        ),
                    cursorBrush =
                        SolidColor(
                            CascadeColors.Ink
                        ),
                    modifier =
                        Modifier
                            .weight(1f)
                            .focusRequester(
                                subjectFocusRequester
                            )
                            .onPreviewKeyEvent {
                                    event ->

                                if (
                                    event.type != KeyEventType.KeyDown
                                ) {

                                    false

                                } else {

                                    when (
                                        event.key
                                    ) {

                                        Key.Escape -> {

                                            onBack()
                                            true
                                        }

                                        Key.Enter,
                                        Key.DirectionDown,
                                        Key.Tab -> {

                                            bodyFocusRequester
                                                .requestFocus()
                                            true
                                        }

                                        else -> false
                                    }
                                }
                            }
                )
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(0.75.dp)
                        .background(CascadeColors.Ink.copy(alpha = 0.15f))
            )

            if (
                draft.isForward &&
                draft.forwardedBody.isNotBlank()
            ) {

                Text(
                    text = "Original message included",
                    color = CascadeColors.Ink.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    fontFamily = InterfaceFont,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 10.dp)
            ) {

                BasicTextField(
                    value = body,
                    onValueChange = {

                        body = it
                    },
                    textStyle =
                        TextStyle(
                            color = CascadeColors.Ink,
                            fontSize = 10.5.sp,
                            fontFamily = InterfaceFont,
                            lineHeight = 15.5.sp
                        ),
                    cursorBrush =
                        SolidColor(
                            CascadeColors.Ink
                        ),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .focusRequester(
                                bodyFocusRequester
                            )
                            .onPreviewKeyEvent {
                                    event ->

                                if (
                                    event.type != KeyEventType.KeyDown
                                ) {

                                    false

                                } else if (
                                    event.key == Key.Enter &&
                                    event.isCtrlPressed
                                ) {

                                    onSend(
                                        toAddress,
                                        subject,
                                        body
                                    )
                                    true

                                } else {

                                    when (
                                        event.key
                                    ) {

                                        Key.Escape -> {

                                            onBack()
                                            true
                                        }

                                        Key.Tab,
                                        Key.DirectionDown -> {

                                            sendFocusRequester
                                                .requestFocus()
                                            true
                                        }

                                        else -> false
                                    }
                                }
                            }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        /*
         * Footer: the send status, if any, and the Send button.
         */
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * Only the send status ("sending…", an error) shows here;
             * the space keeps Send on the right.
             */
            Text(
                text = statusText,
                color = AccentOrange,
                fontSize = 8.5.sp,
                lineHeight = 10.5.sp,
                fontFamily = InterfaceFont,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            Row(
                modifier =
                    Modifier
                        .height(38.dp)
                        .border(
                            width = if (sendFocused) 2.dp else 0.dp,
                            color = if (sendFocused) PrimaryText else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            CascadeColors.Mail,
                            RoundedCornerShape(12.dp)
                        )
                        .focusRequester(
                            sendFocusRequester
                        )
                        .onFocusChanged {
                            sendFocused =
                                it.isFocused
                        }
                        .focusable()
                        .onPreviewKeyEvent {
                                event ->

                            if (
                                event.type != KeyEventType.KeyDown
                            ) {

                                false

                            } else {

                                when (
                                    event.key
                                ) {

                                    Key.Enter -> {

                                        onSend(
                                            toAddress,
                                            subject,
                                            body
                                        )
                                        true
                                    }

                                    Key.DirectionUp -> {

                                        bodyFocusRequester
                                            .requestFocus()
                                        true
                                    }

                                    Key.Escape -> {

                                        onBack()
                                        true
                                    }

                                    else -> false
                                }
                            }
                        }
                        .clickable {

                            onSend(
                                toAddress,
                                subject,
                                body
                            )
                        }
                        .padding(horizontal = 16.dp),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    text = "Send",
                    color = CascadeColors.Ink,
                    fontSize = 10.5.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.SemiBold
                )

                Icon(
                    imageVector = Icons.Outlined.ArrowUpward,
                    contentDescription = null,
                    tint = CascadeColors.Ink,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}


/*
 * Opens the conversation with this number in the phone's default SMS
 * app (Google Messages, Samsung Messages, ...) unless Prompt Launcher
 * itself is the default. Returns true when it handed off, false when
 * the caller should show Prompt's own thread instead.
 */
internal fun openThreadInMessagingAppIfNotDefault(
    context: Context,
    phoneNumber: String
): Boolean {

    val promptIsDefault =
        runCatching {
            com.forrest.titanlauncher.messages.SmsRoleManager(
                context
            ).isDefaultSmsApp()
        }
            .getOrDefault(
                false
            )

    if (
        promptIsDefault
    ) {
        return false
    }

    return runCatching {
        context.startActivity(
            Intent(
                Intent.ACTION_SENDTO,
                Uri.parse(
                    "smsto:" + phoneNumber
                )
            )
        )
        true
    }
        .getOrDefault(
            false
        )
}
