package com.forrest.titanlauncher.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

/*
 * PHONE CALENDARS (no Google sign-in)
 *
 * Reads and writes the calendars already on the phone through
 * Android's own calendar store, the one every calendar app uses:
 * Samsung, Outlook/Exchange, Proton, Etar, DAVx⁵, local calendars, and
 * Google accounts synced to the phone too. No account, token or
 * network on Prompt's side.
 *
 * Calendar ids are kept as "device:<id>" so they can never be mixed up
 * with a Google calendar id saved before.
 */
object DeviceCalendarRepository {

    private const val IdPrefix = "device:"

    fun isDeviceId(
        id: String?
    ): Boolean =
        id?.startsWith(IdPrefix) == true

    private fun rawId(
        id: String
    ): Long? =
        id.removePrefix(IdPrefix).toLongOrNull()

    fun canRead(
        context: Context
    ): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

    fun canWrite(
        context: Context
    ): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

    /*
     * Calendars new events can go into (owner, editor or contributor
     * access), visible ones first, the account's primary first.
     */
    suspend fun loadWritableCalendars(
        context: Context
    ): List<GoogleCalendarOption> =
        withContext(Dispatchers.IO) {

            if (!canRead(context)) return@withContext emptyList()

            val calendars =
                mutableListOf<Pair<GoogleCalendarOption, Boolean>>()

            runCatching {
                context.contentResolver
                    .query(
                        CalendarContract.Calendars.CONTENT_URI,
                        arrayOf(
                            CalendarContract.Calendars._ID,
                            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                            CalendarContract.Calendars.ACCOUNT_NAME,
                            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                            CalendarContract.Calendars.IS_PRIMARY,
                            CalendarContract.Calendars.VISIBLE
                        ),
                        CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= ?",
                        arrayOf(
                            CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()
                        ),
                        null
                    )
                    ?.use { cursor ->
                        while (cursor.moveToNext()) {

                            val id = cursor.getLong(0)
                            val name = cursor.getString(1).orEmpty()
                            val account = cursor.getString(2).orEmpty()
                            val primary = cursor.getInt(4) == 1
                            val visible = cursor.getInt(5) == 1

                            val label =
                                when {
                                    name.isBlank() -> account.ifBlank { "Calendar" }
                                    account.isBlank() || account == name -> name
                                    else -> "$name · $account"
                                }

                            calendars +=
                                GoogleCalendarOption(
                                    id = IdPrefix + id,
                                    summary = label,
                                    primary = primary,
                                    accessRole = "phone"
                                ) to visible
                        }
                    }
            }

            calendars
                .sortedWith(
                    compareByDescending<Pair<GoogleCalendarOption, Boolean>> { it.second }
                        .thenByDescending { it.first.primary }
                        .thenBy { it.first.summary.lowercase() }
                )
                .map { it.first }
        }

    /*
     * Upcoming events across the phone's visible calendars (recurring
     * events expanded), for the home card and Assist.
     */
    suspend fun loadSnapshot(
        context: Context,
        agendaDays: Long,
        nextEventDays: Long,
        onlyCalendarId: String? = null
    ): CalendarAssistSnapshot =
        withContext(Dispatchers.IO) {

            if (!canRead(context)) {
                return@withContext CalendarAssistSnapshot(
                    nextEvent = null,
                    agendaEvents = emptyList()
                )
            }

            val now = System.currentTimeMillis()

            val dayStart =
                java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis

            val end =
                dayStart + maxOf(agendaDays, nextEventDays) * 24L * 60 * 60 * 1000

            val builder =
                CalendarContract.Instances.CONTENT_URI.buildUpon()

            ContentUris.appendId(builder, dayStart)
            ContentUris.appendId(builder, end)

            val calendarFilter =
                onlyCalendarId?.let { rawId(it) }

            val selection =
                if (calendarFilter != null) {
                    CalendarContract.Instances.VISIBLE + " = 1 AND " +
                            CalendarContract.Instances.CALENDAR_ID + " = ?"
                } else {
                    CalendarContract.Instances.VISIBLE + " = 1"
                }

            val args =
                calendarFilter?.let { arrayOf(it.toString()) }

            val events =
                mutableListOf<CalendarAssistEvent>()

            runCatching {
                context.contentResolver
                    .query(
                        builder.build(),
                        arrayOf(
                            CalendarContract.Instances.TITLE,
                            CalendarContract.Instances.BEGIN,
                            CalendarContract.Instances.ALL_DAY,
                            CalendarContract.Instances.EVENT_LOCATION
                        ),
                        selection,
                        args,
                        CalendarContract.Instances.BEGIN + " ASC"
                    )
                    ?.use { cursor ->
                        while (cursor.moveToNext()) {

                            val allDay = cursor.getInt(2) == 1

                            /*
                             * All-day events are stored at UTC
                             * midnight; shift to local midnight so
                             * they land on the right day.
                             */
                            var begin = cursor.getLong(1)
                            if (allDay) {
                                begin -= TimeZone.getDefault().getOffset(begin)
                            }

                            events +=
                                CalendarAssistEvent(
                                    title = cursor.getString(0).orEmpty().ifBlank { "(No title)" },
                                    startTime = begin,
                                    allDay = allDay,
                                    location = cursor.getString(3).orEmpty()
                                )
                        }
                    }
            }

            val agendaEnd =
                dayStart + agendaDays * 24L * 60 * 60 * 1000

            CalendarAssistSnapshot(
                nextEvent =
                    events.firstOrNull { !it.allDay && it.startTime >= now },
                agendaEvents =
                    events.filter { it.startTime < agendaEnd }
            )
        }

    /*
     * Adds an event to a phone calendar. The phone's calendar app
     * syncs it to that calendar's account as usual.
     */
    suspend fun createEvent(
        context: Context,
        calendarId: String,
        draft: CalendarEventDraft
    ): CalendarCreateResult =
        withContext(Dispatchers.IO) {

            val id =
                rawId(calendarId)
                    ?: return@withContext CalendarCreateResult(
                        success = false,
                        message = "RUN CALENDARSETUP"
                    )

            if (!canWrite(context)) {
                return@withContext CalendarCreateResult(
                    success = false,
                    message = "ALLOW CALENDAR ACCESS"
                )
            }

            val values =
                ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, id)
                    put(CalendarContract.Events.TITLE, draft.title)
                    put(CalendarContract.Events.DTSTART, draft.startMillis)
                    put(CalendarContract.Events.DTEND, draft.endMillis)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }

            val inserted =
                runCatching {
                    context.contentResolver.insert(
                        CalendarContract.Events.CONTENT_URI,
                        values
                    )
                }
                    .getOrNull()

            if (inserted != null) {
                CalendarCreateResult(
                    success = true,
                    message = "✓ EVENT ADDED · ${draft.confirmationTime}"
                )
            } else {
                CalendarCreateResult(
                    success = false,
                    message = "COULD NOT ADD EVENT"
                )
            }
        }
}
