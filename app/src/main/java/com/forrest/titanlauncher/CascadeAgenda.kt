package com.forrest.titanlauncher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * CASCADE AGENDA
 *
 * Tapping an event card on home opens the calendar as a lilac card in
 * the deck's place, instead of jumping straight to the calendar app:
 * a strip of the next seven days, then that day's events. "Calendar"
 * still opens the phone's calendar app, ✕ or Back closes the card,
 * and Home closes it too.
 *
 * Events come from the same agenda the home deck and Assist use (the
 * next two weeks), so nothing new is read from the phone.
 */

/*
 * True while the agenda card is showing in place of the deck.
 */
internal var CascadeAgendaOpen by mutableStateOf(false)

private val AgendaTimeFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

private val AgendaDayLetters: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

private val AgendaTitleFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())

/*
 * The day an event falls on. All-day events are stored at midnight
 * UTC, so they're read in UTC; timed events in the phone's zone.
 */
private fun agendaDateOf(
    event: UpcomingCalendarEvent
): LocalDate =
    Instant
        .ofEpochMilli(event.startTime)
        .atZone(
            if (event.allDay) ZoneOffset.UTC else ZoneId.systemDefault()
        )
        .toLocalDate()

@Composable
internal fun CascadeAgendaCard(
    events: List<UpcomingCalendarEvent>,
    onOpenCalendarApp: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {

    BackHandler {
        onClose()
    }

    val today = remember { LocalDate.now() }
    var selectedDay by remember { mutableStateOf(today) }

    val days = remember(today) { (0L until 7L).map { today.plusDays(it) } }

    val eventsByDay =
        remember(events) {
            events.groupBy { agendaDateOf(it) }
        }

    val dayEvents =
        (eventsByDay[selectedDay] ?: emptyList())
            .sortedWith(
                compareByDescending<UpcomingCalendarEvent> { it.allDay }
                    .thenBy { it.startTime }
            )

    val ink = CascadeColors.Ink
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier =
            modifier
                .clip(shape)
                .background(CascadeColors.Calendar, shape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {

        /*
         * Label, the calendar app button and ✕.
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text =
                    if (selectedDay == today) {
                        "TODAY"
                    } else {
                        selectedDay.format(AgendaTitleFormat).uppercase()
                    },
                color = ink,
                fontSize = 8.sp,
                letterSpacing = 0.6.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ink.copy(alpha = 0.1f))
                        .clickable { onOpenCalendarApp() }
                        .padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Calendar ↗",
                    color = ink,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ink.copy(alpha = 0.1f))
                        .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close calendar",
                    tint = ink,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        /*
         * The week strip: the selected day filled in ink, a dot under
         * days that have something on.
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            days.forEach { day ->

                val selected = day == selectedDay
                val hasEvents = !eventsByDay[day].isNullOrEmpty()

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) ink else ink.copy(alpha = 0.08f)
                            )
                            .clickable { selectedDay = day }
                            .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = day.format(AgendaDayLetters),
                        color = if (selected) CascadeColors.Calendar else ink,
                        fontSize = 7.5.sp,
                        lineHeight = 9.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )

                    Text(
                        text = day.dayOfMonth.toString(),
                        color = if (selected) CascadeColors.Calendar else ink,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Box(
                        modifier =
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !hasEvents -> ink.copy(alpha = 0f)
                                        selected -> CascadeColors.Calendar
                                        else -> ink
                                    }
                                )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        /*
         * The day's events: time on the left, title and place on the
         * right, divided by thin ink lines.
         */
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
        ) {

            if (dayEvents.isEmpty()) {

                item(key = "agenda-empty") {
                    Text(
                        text = "Nothing planned",
                        color = ink.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp)
                    )
                }

            } else {

                items(
                    items = dayEvents,
                    key = { "${it.startTime}-${it.title}" }
                ) { event ->

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCalendarApp() }
                                .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.Top
                    ) {

                        Text(
                            text =
                                if (event.allDay) {
                                    "All day"
                                } else {
                                    Instant
                                        .ofEpochMilli(event.startTime)
                                        .atZone(ZoneId.systemDefault())
                                        .format(AgendaTimeFormat)
                                        .lowercase()
                                },
                            color = ink,
                            fontSize = 9.5.sp,
                            lineHeight = 13.sp,
                            fontFamily = InterfaceFont,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.width(52.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = event.title.ifBlank { "Event" },
                                color = ink,
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                fontFamily = InterfaceFont,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (event.location.isNotBlank()) {
                                Text(
                                    text = event.location,
                                    color = ink.copy(alpha = 0.72f),
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp,
                                    fontFamily = InterfaceFont,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(0.75.dp)
                                .background(ink.copy(alpha = 0.15f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Type : to add an event",
            color = ink.copy(alpha = 0.7f),
            fontSize = 8.sp,
            fontFamily = InterfaceFont,
            maxLines = 1
        )
    }
}
