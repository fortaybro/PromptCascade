package com.forrest.titanlauncher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forrest.titanlauncher.mail.MailMessage
import com.forrest.titanlauncher.usage.HourProductivityStatus
import kotlinx.coroutines.launch

/*
 * CASCADE HOME DECK
 *
 * Prompt Cascade's home: everything that needs you as a deck of
 * cards in a fixed order of urgency (missed calls, the next event,
 * texts, mail, later events). The first card is drawn open; the rest
 * are layered underneath it like cards in a wallet, each showing its
 * top strip.
 *
 * Each kind of card has its own colour, the same everywhere in the
 * app, so you can tell what a card is before reading it. Card text
 * is always dark ink, since every card colour is light.
 *
 * Sizes follow the concept mockups: px x 0.82 = dp, px x 0.70 = sp.
 */

internal object CascadeColors {
    val Ink = Color(0xFF1E1A2E)
    val Calendar = Color(0xFFCBB8FF)
    val Later = Color(0xFFE3D9FF)
    val Text = Color(0xFFFFE38A)
    val Mail = Color(0xFFA9D8FF)
    val Todo = Color(0xFFB6F0C9)
    val Note = Color(0xFFFFC9A8)
    val Call = Color(0xFFFF6A2B)
    val Ask = Color(0xFFF4F1FA)
}

internal enum class CascadeKind(
    val label: String,
    val color: Color
) {
    CALL("MISSED CALL", CascadeColors.Call),
    NOW("NEXT", CascadeColors.Calendar),
    TEXT("TEXT", CascadeColors.Text),
    MAIL("MAIL", CascadeColors.Mail),
    LATER("LATER", CascadeColors.Later),
    TODO("TO-DO", CascadeColors.Todo),
    NOTE("NOTE", CascadeColors.Note)
}

/*
 * The card colour for a command being typed in the prompt, matching
 * the card it will become. Null for anything that isn't one of the
 * deck's kinds (app search, plain text, word commands).
 */
internal fun cascadeColorForAction(
    action: String
): Color? =
    when (action) {
        "add event" -> CascadeColors.Calendar
        "add task" -> CascadeColors.Todo
        "add note" -> CascadeColors.Note
        "set timer", "set alarm" -> CascadeColors.Later
        "compose mail" -> CascadeColors.Mail
        "message" -> CascadeColors.Text
        "ask assistant" -> CascadeColors.Ask
        else -> null
    }

/*
 * The card colour for a hub item: texts yellow, missed calls fire
 * orange, other calls pale lilac, mail blue, other apps' notifications
 * soft white.
 */
internal fun cascadeColorForHubItem(
    item: HubDisplayItem
): Color =
    when (item.type) {
        HubItemType.MESSAGE -> CascadeColors.Text
        HubItemType.CALL ->
            if (item.missed) CascadeColors.Call else CascadeColors.Later
        HubItemType.EMAIL -> CascadeColors.Mail
        HubItemType.NOTIFICATION -> CascadeColors.Ask
    }

/*
 * The colour of the Cascade card being drawn, if any. Pieces shared
 * with plain screens (quick reply's header, bubbles and field) read
 * it and switch to dark ink on a card.
 */
internal val LocalCascadeCard =
    staticCompositionLocalOf<Color?> { null }

/*
 * onCard when drawn on a Cascade card, otherwise the usual colour.
 */
@Composable
@ReadOnlyComposable
internal fun onCardOr(
    onCard: Color,
    otherwise: Color
): Color =
    if (LocalCascadeCard.current != null) onCard else otherwise

/*
 * The card colour for a quick reply entry. Texts are yellow whether
 * they come from Prompt's own SMS store or from a messaging app's
 * conversation notification (Google Messages and RCS arrive that
 * way), the same rule the hub uses. Other apps' alerts are soft white.
 */
internal fun cascadeColorForQuickReply(
    entry: QuickReplyEntry
): Color =
    if (entry.isMessage || entry.messages.isNotEmpty()) {
        CascadeColors.Text
    } else {
        CascadeColors.Ask
    }

internal data class CascadeCard(
    val key: String,
    val kind: CascadeKind,
    val title: String,
    val detail: String,
    val meta: String,
    val onClick: () -> Unit
)

/*
 * KEYBOARD DEALING
 *
 * (Swiping on the deck is the main way to deal; see CascadeCardStack.)
 * With the prompt closed, a keyboard's down and up, where there are
 * any, also move through the deck: each press deals the next card to the top, open,
 * and the ones passed go to the bottom of the stack. Enter opens the
 * top card. Typing anything else still opens the prompt as before.
 *
 * The prompt's hidden key field calls cascadeDeckMove and
 * cascadeDeckOpen (see CommandBar); they do nothing unless the deck
 * is on screen, so the prompt over other apps is unaffected.
 */
internal var CascadeDeckSelection by mutableIntStateOf(0)

private var cascadeDeckShowing = false
private var cascadeDeckSize = 0
private var cascadeTopCardAction: (() -> Unit)? = null

internal fun cascadeDeckMove(
    delta: Int
): Boolean {

    if (!cascadeDeckShowing || cascadeDeckSize < 2) {
        return false
    }

    CascadeDeckSelection =
        Math.floorMod(CascadeDeckSelection + delta, cascadeDeckSize)

    return true
}

internal fun cascadeDeckOpen(): Boolean {

    val action =
        cascadeTopCardAction
            ?.takeIf { cascadeDeckShowing }
            ?: return false

    CascadeDeckSelection = 0
    action()
    return true
}

private val CascadeCorner = 20.dp
private val CascadeOpenHeight = 128.dp
private val CascadeStripHeight = 66.dp
private val CascadeStripStep = 38.dp
private val CascadeGap = 10.dp

/*
 * The part of a strip that must show for it to be drawn: its label
 * row and a little card below it.
 */
private val CascadeStripLabelRoom = 34.dp

/*
 * The whole home body below the header: big time, weather and the
 * day's focus bars, then the deck, then a small row of shortcuts.
 */
@Composable
internal fun CascadeHomeDeck(
    time: String,
    weather: String?,
    focusHours: List<HourProductivityStatus>,
    currentHour: Int,
    showFocus: Boolean,
    onFocusClick: (() -> Unit)?,
    cards: List<CascadeCard>,
    shortcuts: List<AppButtonData>,
    modifier: Modifier = Modifier,
    /*
     * Changes when Home is pressed: the deck goes back to its first
     * card and the agenda card closes.
     */
    resetKey: Any? = null,
    /*
     * The next two weeks of events, for the agenda card that opens in
     * the deck's place (see CascadeAgenda.kt).
     */
    agendaEvents: List<UpcomingCalendarEvent> = emptyList(),
    onOpenCalendarApp: () -> Unit = {}
) {

    LaunchedEffect(resetKey) {
        CascadeDeckSelection = 0
        CascadeAgendaOpen = false
    }

    DisposableEffect(Unit) {
        cascadeDeckShowing = true
        onDispose {
            cascadeDeckShowing = false
            cascadeTopCardAction = null
        }
    }

    /*
     * The deck as dealt: the selected card on top, the cards before
     * it moved to the bottom.
     */
    val selection =
        if (cards.isEmpty()) 0 else Math.floorMod(CascadeDeckSelection, cards.size)

    val dealt =
        cards.drop(selection) + cards.take(selection)

    SideEffect {
        cascadeDeckSize = cards.size
        cascadeTopCardAction = dealt.firstOrNull()?.onClick
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {

        CascadeTimeRow(
            time = time,
            weather = weather,
            focusHours = focusHours,
            currentHour = currentHour,
            showFocus = showFocus,
            onFocusClick = onFocusClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (CascadeAgendaOpen) {

            CascadeAgendaCard(
                events = agendaEvents,
                onOpenCalendarApp = onOpenCalendarApp,
                onClose = {
                    CascadeAgendaOpen = false
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
            )

        } else {

        CascadeCardStack(
            cards = dealt,
            position = selection + 1,
            /*
             * Through the shared helpers, which read the deck's
             * current state: the swipe handler outlives a single
             * composition, so it must not hold on to an old selection.
             */
            onDealNext = {
                cascadeDeckMove(1)
            },
            onDealPrevious = {
                cascadeDeckMove(-1)
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
        )
        }

        Spacer(modifier = Modifier.height(8.dp))

        CascadeShortcutRow(
            shortcuts = shortcuts
        )
    }
}

@Composable
private fun CascadeTimeRow(
    time: String,
    weather: String?,
    focusHours: List<HourProductivityStatus>,
    currentHour: Int,
    showFocus: Boolean,
    onFocusClick: (() -> Unit)?
) {

    /*
     * "10:42 am" -> "10:42" large and "am" small beside it.
     */
    val clock = time.substringBefore(" ")
    val period = time.substringAfter(" ", "")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {

        Text(
            text = clock,
            color = PrimaryText,
            fontSize = 38.sp,
            lineHeight = 40.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Bold
        )

        if (period.isNotBlank()) {
            Text(
                text = period,
                color = SecondaryText,
                fontSize = 11.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(bottom = 5.dp)
        ) {

            if (!weather.isNullOrBlank()) {
                Text(
                    text = weather,
                    color = SecondaryText,
                    fontSize = 10.sp,
                    fontFamily = InterfaceFont,
                    maxLines = 1
                )
            }

            if (showFocus) {
                Spacer(modifier = Modifier.height(4.dp))
                CascadeFocusBars(
                    hours = focusHours,
                    currentHour = currentHour,
                    modifier =
                        Modifier
                            .width(84.dp)
                            .height(14.dp)
                            .then(
                                if (onFocusClick != null) {
                                    Modifier.clickable { onFocusClick() }
                                } else {
                                    Modifier
                                }
                            )
                )
            }
        }
    }
}

/*
 * The 24 productivity hours as a small bar row instead of the ring:
 * tall for productive hours, short for idle, the distracting colour
 * for distracted ones, the accent for the current hour.
 */
@Composable
private fun CascadeFocusBars(
    hours: List<HourProductivityStatus>,
    currentHour: Int,
    modifier: Modifier = Modifier
) {

    val done = PrimaryText
    val idle = ProductivityGray
    val distracted = ProductivityRed
    val accent = AccentOrange

    Canvas(modifier = modifier) {

        val slot = size.width / 24f
        val barWidth = slot * 0.62f

        for (hour in 0 until 24) {

            val status = hours.getOrNull(hour) ?: HourProductivityStatus.GRAY

            val (color, fraction) =
                when {
                    hour == currentHour -> accent to 1f
                    hour > currentHour -> idle.copy(alpha = 0.55f) to 0.22f
                    status == HourProductivityStatus.PRODUCTIVE -> done to 1f
                    status == HourProductivityStatus.UNPRODUCTIVE -> distracted to 0.7f
                    else -> idle to 0.22f
                }

            val barHeight = size.height * fraction

            drawRoundRect(
                color = color,
                topLeft = Offset(hour * slot, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/*
 * The deck itself. The first card is open; the rest sit below it as
 * strips, each overlapping the one before.
 *
 * Swiping on the deck deals the cards and the cards follow your
 * finger, like dragging between home and the hub:
 *   - drag up: the open card lifts away and fades while the next
 *     card rises into its place;
 *   - drag down: the open card slides down into the stack while the
 *     last card comes down from the top.
 * Let go past a quarter of the way and the deal finishes; otherwise
 * the cards spring back. Taps still open cards as before.
 */
@Composable
private fun CascadeCardStack(
    cards: List<CascadeCard>,
    position: Int,
    onDealNext: () -> Unit,
    onDealPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {

    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.clipToBounds()
    ) {

        val openHeight =
            if (maxHeight < CascadeOpenHeight + CascadeGap + CascadeStripLabelRoom) {
                maxHeight
            } else {
                CascadeOpenHeight
            }

        /*
         * How far one deal moves the cards: from the open slot to the
         * first strip's slot.
         */
        val travelPx = with(density) { (openHeight + CascadeGap).toPx() }
        val stripStepPx = with(density) { CascadeStripStep.toPx() }
        val canDeal = cards.size > 1

        /*
         * How many strips fit with at least their label row showing.
         * A strip cut through its text looks broken, so those are
         * left out and the last one shown says how many more wait.
         */
        val stripsRoom =
            maxHeight - openHeight - CascadeGap
        val stripsThatFit =
            if (stripsRoom < CascadeStripLabelRoom) {
                0
            } else {
                1 + ((stripsRoom - CascadeStripLabelRoom) / CascadeStripStep).toInt()
            }
        val strips =
            cards.drop(1)
        val shownStrips =
            strips.take(stripsThatFit)
        val hiddenCount =
            strips.size - shownStrips.size

        val offset = drag.value
        val progress = (kotlin.math.abs(offset) / travelPx).coerceIn(0f, 1f)

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(canDeal, travelPx) {

                        if (!canDeal) {
                            return@pointerInput
                        }

                        detectVerticalDragGestures(
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                scope.launch {
                                    drag.snapTo(
                                        (drag.value + amount)
                                            .coerceIn(-travelPx, travelPx)
                                    )
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    val value = drag.value
                                    when {
                                        value < -travelPx * 0.25f -> {
                                            drag.animateTo(-travelPx, tween(140))
                                            onDealNext()
                                            drag.snapTo(0f)
                                        }

                                        value > travelPx * 0.25f -> {
                                            drag.animateTo(travelPx, tween(140))
                                            onDealPrevious()
                                            drag.snapTo(0f)
                                        }

                                        else ->
                                            drag.animateTo(0f, spring())
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    drag.animateTo(0f, spring())
                                }
                            }
                        )
                    }
        ) {

            /*
             * Dragging down: the last card comes in from above.
             */
            if (offset > 0f && cards.size > 1) {
                CascadeOpenCard(
                    card = cards.last(),
                    position = if (position == 1) cards.size else position - 1,
                    total = cards.size,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(openHeight)
                            .graphicsLayer {
                                translationY = offset - travelPx
                                alpha = progress
                            }
                )
            }

            cards.firstOrNull()?.let { card ->
                CascadeOpenCard(
                    card = card,
                    position = position,
                    total = cards.size,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(openHeight)
                            .graphicsLayer {
                                translationY = offset
                                alpha =
                                    if (offset < 0f) 1f - progress * 0.7f else 1f
                            }
                )
            }

            shownStrips.forEachIndexed { index, card ->

                val isLastShown =
                    index == shownStrips.lastIndex

                /*
                 * Up: the first strip follows the finger into the open
                 * slot and the others each move up one step. Down:
                 * every strip moves down one step.
                 */
                val shift =
                    when {
                        offset < 0f && index == 0 -> offset
                        offset < 0f -> -progress * stripStepPx
                        else -> progress * stripStepPx
                    }

                CascadeStripCard(
                    card =
                        if (isLastShown && hiddenCount > 0) {
                            card.copy(meta = "+$hiddenCount more")
                        } else {
                            card
                        },
                    modifier =
                        Modifier
                            .offset(y = openHeight + CascadeGap + CascadeStripStep * index)
                            .fillMaxWidth()
                            .height(CascadeStripHeight)
                            .graphicsLayer {
                                translationY = shift
                            }
                )
            }
        }
    }
}

@Composable
private fun CascadeOpenCard(
    card: CascadeCard,
    position: Int,
    total: Int,
    modifier: Modifier = Modifier
) {

    val shape = RoundedCornerShape(CascadeCorner)

    Column(
        modifier =
            modifier
                .clip(shape)
                .background(card.kind.color, shape)
                .clickable { card.onClick() }
                .padding(horizontal = 15.dp, vertical = 13.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CascadeKindLabel(
                text =
                    if (total > 1) {
                        "${card.kind.label} · $position OF $total"
                    } else {
                        card.kind.label
                    },
                modifier = Modifier.weight(1f)
            )
            CascadeKindLabel(card.meta.uppercase())
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = card.title,
            color = CascadeColors.Ink,
            fontSize = 19.sp,
            lineHeight = 22.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.weight(1f))

        /*
         * Detail on the left, Open on the right, on one bottom row.
         * Stacking them made the button get squeezed on the Titan's
         * short screen.
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = card.detail,
                color = CascadeColors.Ink,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(CascadeColors.Ink)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Open",
                    color = card.kind.color,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CascadeStripCard(
    card: CascadeCard,
    modifier: Modifier = Modifier
) {

    val shape = RoundedCornerShape(CascadeCorner)

    Row(
        modifier =
            modifier
                .shadow(8.dp, shape, clip = false)
                .clip(shape)
                .background(card.kind.color, shape)
                .clickable { card.onClick() }
                .padding(start = 15.dp, end = 15.dp, top = 11.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        CascadeKindLabel(
            text = card.kind.label,
            modifier = Modifier.padding(top = 2.dp)
        )

        Text(
            text =
                if (card.detail.isBlank()) {
                    card.title
                } else {
                    "${card.title} · ${card.detail}"
                },
            color = CascadeColors.Ink,
            fontSize = 10.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        CascadeKindLabel(
            text = card.meta,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun CascadeKindLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = CascadeColors.Ink,
        fontSize = 8.sp,
        letterSpacing = 0.6.sp,
        fontFamily = InterfaceFont,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
    )
}

/*
 * The old 2 x 4 tiles as one row of round icon buttons, so the deck
 * gets the room. Same actions as before; the label becomes the
 * button's description for TalkBack.
 */
@Composable
private fun CascadeShortcutRow(
    shortcuts: List<AppButtonData>
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        shortcuts.forEach { button ->

            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceBlack)
                        .clickable { button.onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = button.icon,
                    contentDescription = button.label,
                    tint = PrimaryText,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/*
 * Builds the deck in order of urgency. Kept out of TitanHomeScreen so
 * that function doesn't grow (see the Oct 7 VerifyError lesson).
 */
internal fun cascadeHomeCards(
    now: Long,
    missedCalls: List<MissedCall>,
    quickReplyEntries: List<QuickReplyEntry>,
    unreadCount: Int,
    latestUnreadBody: String?,
    liveConversationPreview: String?,
    showMessages: Boolean,
    homeMailCount: Int,
    homeMailWord: String,
    importantUnreadMail: MailMessage?,
    newMailAppNames: List<String>,
    newMailTimestamp: Long?,
    showCalendar: Boolean,
    upcomingEvent: UpcomingCalendarEvent?,
    agendaEvents: List<UpcomingCalendarEvent>,
    onOpenMissedCalls: () -> Unit,
    onOpenLatestText: () -> Unit,
    onOpenQuickReply: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenMail: () -> Unit,
    onOpenCalendar: () -> Unit
): List<CascadeCard> =
    buildList {

        /*
         * Missed calls: one card for each person, newest first.
         */
        missedCalls
            .groupBy { it.number }
            .values
            .map { calls -> calls.maxBy { it.timestamp } to calls.size }
            .sortedByDescending { it.first.timestamp }
            .take(2)
            .forEach { (call, count) ->
                add(
                    CascadeCard(
                        key = "call-${call.id}",
                        kind = CascadeKind.CALL,
                        title = call.name.ifBlank { call.number },
                        detail = if (count > 1) "Called $count times" else "Missed call",
                        meta = daylightAgo(call.timestamp, now),
                        onClick = onOpenMissedCalls
                    )
                )
            }

        /*
         * The next event goes near the top when it's soon (within
         * three hours); otherwise it waits until after texts and mail.
         */
        val nextEventCard =
            if (showCalendar && upcomingEvent != null) {
                val minutesAway = (upcomingEvent.startTime - now) / 60_000L
                CascadeCard(
                    key = "event-${upcomingEvent.startTime}-${upcomingEvent.title}",
                    kind = CascadeKind.NOW,
                    title = upcomingEvent.title.ifBlank { "Event" },
                    detail =
                        listOf(
                            formatCalendarEventTime(upcomingEvent),
                            upcomingEvent.location
                        )
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                    meta =
                        when {
                            upcomingEvent.allDay -> "today"
                            minutesAway <= 0 -> "now"
                            minutesAway < 60 -> "in $minutesAway min"
                            minutesAway < 180 -> "in ${minutesAway / 60} h"
                            else -> "next"
                        },
                    onClick = onOpenCalendar
                )
            } else {
                null
            }

        val eventIsSoon =
            upcomingEvent != null &&
                    !upcomingEvent.allDay &&
                    upcomingEvent.startTime - now < 3L * 60 * 60 * 1000

        if (nextEventCard != null && eventIsSoon) {
            add(nextEventCard)
        }

        /*
         * Texts: one card per conversation waiting, up to three. The
         * newest opens straight to its thread; the others open quick
         * reply, where every waiting conversation is listed.
         */
        if (showMessages) {

            val unreadThreads =
                quickReplyEntries
                    .filter { it.isMessage && it.unread }
                    .sortedByDescending { it.timestamp }
                    .take(3)

            if (unreadThreads.isNotEmpty()) {

                unreadThreads.forEachIndexed { index, entry ->
                    add(
                        CascadeCard(
                            key = "text-${entry.id}",
                            kind = CascadeKind.TEXT,
                            title = entry.title.ifBlank { "New message" },
                            detail = entry.preview.take(80),
                            meta = daylightAgo(entry.timestamp, now),
                            onClick =
                                if (index == 0) onOpenLatestText else onOpenQuickReply
                        )
                    )
                }

            } else if (unreadCount > 0) {

                add(
                    CascadeCard(
                        key = "text-unread",
                        kind = CascadeKind.TEXT,
                        title =
                            liveConversationPreview?.substringBefore(": ")
                                ?: "New messages",
                        detail =
                            (latestUnreadBody
                                ?: liveConversationPreview?.substringAfter(": ")
                                ?: "").take(80),
                        meta = if (unreadCount == 1) "1 text" else "$unreadCount texts",
                        onClick = onOpenLatestText
                    )
                )
            }
        }

        /*
         * Mail: one card summing up what's new.
         */
        if (showMessages && homeMailCount > 0) {

            add(
                CascadeCard(
                    key = "mail",
                    kind = CascadeKind.MAIL,
                    title =
                        importantUnreadMail?.sender?.takeIf { it.isNotBlank() }
                            ?: if (homeMailCount == 1) {
                                "1 $homeMailWord email"
                            } else {
                                "$homeMailCount $homeMailWord emails"
                            },
                    detail =
                        importantUnreadMail?.subject?.take(80)
                            ?: newMailAppNames
                                .filter { it.isNotBlank() }
                                .distinct()
                                .joinToString(", "),
                    meta =
                        daylightAgo(
                            importantUnreadMail?.timestamp ?: newMailTimestamp ?: 0L,
                            now
                        ),
                    onClick = onOpenMail
                )
            )
        }

        if (nextEventCard != null && !eventIsSoon) {
            add(nextEventCard)
        }

        /*
         * Later events from the agenda, skipping the one already
         * shown as next.
         */
        if (showCalendar) {
            agendaEvents
                .filter { it.startTime > now || it.allDay }
                .filterNot {
                    upcomingEvent != null &&
                            it.title == upcomingEvent.title &&
                            it.startTime == upcomingEvent.startTime
                }
                .take(2)
                .forEach { event ->
                    add(
                        CascadeCard(
                            key = "later-${event.startTime}-${event.title}",
                            kind = CascadeKind.LATER,
                            title = event.title.ifBlank { "Event" },
                            detail = "",
                            meta = formatCalendarEventTime(event),
                            onClick = onOpenCalendar
                        )
                    )
                }
        }

        /*
         * Nothing waiting at all: one calm card so the deck is never
         * empty, opening messages like the old "all caught up" row.
         */
        if (isEmpty()) {
            add(
                CascadeCard(
                    key = "caught-up",
                    kind = CascadeKind.TODO,
                    title = "All caught up",
                    detail = "Type to write a note, add an event or ask anything.",
                    meta = "",
                    onClick = onOpenMessages
                )
            )
        }
    }
