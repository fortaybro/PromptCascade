package com.forrest.titanlauncher

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.CallLog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.forrest.titanlauncher.calendar.CalendarSelectionStore
import com.forrest.titanlauncher.calendar.DeviceCalendarRepository
import com.forrest.titanlauncher.contacts.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/*
 * MISSED CALLS ON HOME
 *
 * A missed call lights the ⚡ in a pulsing fire colour and shows at the
 * top of quick reply, with Call back, Text and Remind me.
 *
 * A missed call stops counting once you:
 *  - act on it from quick reply (call back, text, reminder, ✕),
 *  - talk to that person afterwards (any later answered or outgoing
 *    call with the same number), or
 *  - leave it for a day.
 *
 * Read from the phone's call log, so it needs the call log
 * permission the hub already asks for.
 */
internal data class MissedCall(
    val id: Long,
    val number: String,
    val name: String,
    val timestamp: Long
)

/*
 * Missed calls still waiting, newest first. Kept up to date while the
 * home screen is showing (see MissedCallWatcher).
 */
internal var MissedCallsWaiting by
mutableStateOf<List<MissedCall>>(
    emptyList()
)

private const val MissedCallPrefs = "prompt_missed_calls"
private const val HandledKey = "handled_ids"
private const val MissedCallWindowMillis = 24L * 60 * 60 * 1000

internal object MissedCallStore {

    fun handled(
        context: Context
    ): Set<String> =
        context
            .getSharedPreferences(MissedCallPrefs, Context.MODE_PRIVATE)
            .getStringSet(HandledKey, emptySet())
            .orEmpty()

    /*
     * Marks it dealt with and drops it from the bolt straight away.
     */
    fun markHandled(
        context: Context,
        call: MissedCall
    ) {
        val prefs =
            context.getSharedPreferences(MissedCallPrefs, Context.MODE_PRIVATE)

        val updated =
            (prefs.getStringSet(HandledKey, emptySet()).orEmpty() + call.id.toString())
                .toList()
                .takeLast(200)
                .toSet()

        prefs.edit().putStringSet(HandledKey, updated).apply()

        MissedCallsWaiting =
            MissedCallsWaiting.filterNot { it.id == call.id }
    }
}

private fun sameNumber(
    a: String,
    b: String
): Boolean {
    val left = a.filter { it.isDigit() }.takeLast(10)
    val right = b.filter { it.isDigit() }.takeLast(10)
    return left.isNotEmpty() && left == right
}

internal suspend fun loadMissedCallsWaiting(
    context: Context
): List<MissedCall> =
    withContext(Dispatchers.IO) {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext emptyList()
        }

        val since =
            System.currentTimeMillis() - MissedCallWindowMillis

        data class Row(
            val id: Long,
            val number: String,
            val date: Long,
            val type: Int
        )

        val rows =
            mutableListOf<Row>()

        runCatching {
            context.contentResolver
                .query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        CallLog.Calls._ID,
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.DATE,
                        CallLog.Calls.TYPE
                    ),
                    CallLog.Calls.DATE + " > ?",
                    arrayOf(since.toString()),
                    CallLog.Calls.DATE + " DESC"
                )
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        rows +=
                            Row(
                                id = cursor.getLong(0),
                                number = cursor.getString(1).orEmpty(),
                                date = cursor.getLong(2),
                                type = cursor.getInt(3)
                            )
                    }
                }
        }

        val handled =
            MissedCallStore.handled(context)

        val contacts =
            ContactRepository(context)

        rows
            .filter { it.type == CallLog.Calls.MISSED_TYPE }
            .filterNot { it.id.toString() in handled }
            /*
             * Talked since: an answered or outgoing call with the
             * same person after the missed one.
             */
            .filterNot { missed ->
                rows.any { later ->
                    later.date > missed.date &&
                            later.type != CallLog.Calls.MISSED_TYPE &&
                            later.type != CallLog.Calls.REJECTED_TYPE &&
                            sameNumber(later.number, missed.number)
                }
            }
            /*
             * One row per person: their latest missed call.
             */
            .distinctBy { it.number.filter { c -> c.isDigit() }.takeLast(10).ifEmpty { it.number } }
            .map { row ->
                MissedCall(
                    id = row.id,
                    number = row.number,
                    name =
                        runCatching {
                            contacts.findContactNameByPhoneNumber(row.number)
                        }
                            .getOrNull()
                            ?.takeIf { it.isNotBlank() }
                            ?: row.number.ifBlank { "Unknown caller" },
                    timestamp = row.date
                )
            }
    }

/*
 * Keeps MissedCallsWaiting current: reads the call log now and again
 * whenever it changes (a call ends, the dialer clears a missed call).
 */
@Composable
internal fun MissedCallWatcher() {

    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    LaunchedEffect(Unit) {
        MissedCallsWaiting =
            loadMissedCallsWaiting(context)
    }

    DisposableEffect(Unit) {

        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    scope.launch {
                        MissedCallsWaiting =
                            loadMissedCallsWaiting(context)
                    }
                }
            }

        runCatching {
            context.contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                observer
            )
        }

        onDispose {
            runCatching {
                context.contentResolver.unregisterContentObserver(observer)
            }
        }
    }
}

internal fun dialMissedCall(
    context: Context,
    call: MissedCall
) {
    runCatching {
        context.startActivity(
            Intent(
                Intent.ACTION_DIAL,
                Uri.parse("tel:" + call.number)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/*
 * "CALL Sarah" at the chosen date and time.
 *
 * Within the next 24 hours it is a real alarm in the Clock app,
 * labelled "CALL Sarah". Android's Clock only takes a time of day, not
 * a date, so anything further out goes on the phone calendar picked
 * in calendarsetup as a "CALL Sarah" event that alerts at that time.
 *
 * Returns the line for the status bubble.
 */
internal fun scheduleCallBack(
    context: Context,
    call: MissedCall,
    atMillis: Long
): String {

    val label =
        "CALL " + call.name

    val calendar =
        java.util.Calendar.getInstance().apply {
            timeInMillis = atMillis
        }

    val whenText =
        SimpleDateFormat("EEE MMM d · h:mm a", Locale.getDefault())
            .format(Date(atMillis))

    val withinADay =
        atMillis - System.currentTimeMillis() in 0..MissedCallWindowMillis

    if (withinADay) {

        val set =
            runCatching {
                context.startActivity(
                    Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_HOUR, calendar.get(java.util.Calendar.HOUR_OF_DAY))
                        .putExtra(AlarmClock.EXTRA_MINUTES, calendar.get(java.util.Calendar.MINUTE))
                        .putExtra(AlarmClock.EXTRA_MESSAGE, label)
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            }
                .getOrDefault(false)

        return if (set) {
            MissedCallStore.markHandled(context, call)
            "✓ ALARM SET · $label · $whenText"
        } else {
            "COULD NOT SET ALARM"
        }
    }

    val calendarId =
        CalendarSelectionStore(context)
            .getPersonalCalendarId()
            ?.takeIf { DeviceCalendarRepository.isDeviceId(it) }
            ?.removePrefix("device:")
            ?.toLongOrNull()

    if (
        calendarId == null ||
        !DeviceCalendarRepository.canWrite(context)
    ) {
        return "PICK A PHONE CALENDAR IN CALENDARSETUP FOR LATER REMINDERS"
    }

    val added =
        runCatching {
            val eventUri =
                context.contentResolver.insert(
                    CalendarContract.Events.CONTENT_URI,
                    ContentValues().apply {
                        put(CalendarContract.Events.CALENDAR_ID, calendarId)
                        put(CalendarContract.Events.TITLE, label)
                        put(CalendarContract.Events.DESCRIPTION, call.number)
                        put(CalendarContract.Events.DTSTART, atMillis)
                        put(CalendarContract.Events.DTEND, atMillis + 15 * 60 * 1000)
                        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                        put(CalendarContract.Events.HAS_ALARM, 1)
                    }
                )
                    ?: return@runCatching false

            context.contentResolver.insert(
                CalendarContract.Reminders.CONTENT_URI,
                ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventUri.lastPathSegment?.toLong())
                    put(CalendarContract.Reminders.MINUTES, 0)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
            )

            true
        }
            .getOrDefault(false)

    return if (added) {
        MissedCallStore.markHandled(context, call)
        "✓ REMINDER SET · $label · $whenText"
    } else {
        "COULD NOT SET REMINDER"
    }
}

/*
 * Android's own date then time pickers, starting an hour from now.
 */
internal fun pickCallBackTime(
    context: Context,
    onPicked: (Long) -> Unit
) {

    val start =
        java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.HOUR_OF_DAY, 1)
            set(java.util.Calendar.MINUTE, 0)
        }

    android.app.DatePickerDialog(
        context,
        { _, year, month, day ->
            android.app.TimePickerDialog(
                context,
                { _, hour, minute ->
                    val picked =
                        java.util.Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                    onPicked(picked.timeInMillis)
                },
                start.get(java.util.Calendar.HOUR_OF_DAY),
                start.get(java.util.Calendar.MINUTE),
                android.text.format.DateFormat.is24HourFormat(context)
            ).show()
        },
        start.get(java.util.Calendar.YEAR),
        start.get(java.util.Calendar.MONTH),
        start.get(java.util.Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = System.currentTimeMillis() - 1000
    }.show()
}

/*
 * ---------------------------------------------------------------
 * UI
 * ---------------------------------------------------------------
 */

internal val MissedCallFire =
    androidx.compose.ui.graphics.Color(0xFFFF6A2B)

private fun missedCallTime(
    timestamp: Long
): String {
    val today =
        android.text.format.DateUtils.isToday(timestamp)

    val time =
        SimpleDateFormat("h:mm a", Locale.getDefault())
            .format(Date(timestamp))
            .lowercase()

    return if (today) time else "yesterday $time"
}

private fun missedCallInitials(
    name: String
): String {
    if (name.any { it.isDigit() } && name.none { it.isLetter() }) return "#"
    return name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
}

/*
 * One missed call with its three actions. Used in quick reply and on
 * the home alert.
 */
@Composable
internal fun MissedCallActionsCard(
    call: MissedCall,
    onCallBack: () -> Unit,
    onText: () -> Unit,
    onRemind: () -> Unit,
    onClose: (() -> Unit)? = null
) {

    androidx.compose.foundation.layout.Column(
        modifier =
            androidx.compose.ui.Modifier
                .fillMaxWidth()
                .background(
                    SurfaceBlack,
                    androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                )
                .padding(13.dp),
        verticalArrangement =
            androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {

        androidx.compose.foundation.layout.Row(
            verticalAlignment =
                androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement =
                androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
        ) {

            androidx.compose.foundation.layout.Box(
                modifier =
                    androidx.compose.ui.Modifier
                        .size(34.dp)
                        .background(
                            MissedCallFire.copy(alpha = 0.18f),
                            androidx.compose.foundation.shape.CircleShape
                        ),
                contentAlignment =
                    androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = missedCallInitials(call.name),
                    color = MissedCallFire,
                    fontSize = 11.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            }

            androidx.compose.foundation.layout.Column(
                modifier =
                    androidx.compose.ui.Modifier.weight(1f)
            ) {
                androidx.compose.material3.Text(
                    text = call.name,
                    color = PrimaryText,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                androidx.compose.material3.Text(
                    text = "Missed call · " + missedCallTime(call.timestamp),
                    color = MissedCallFire,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontFamily = InterfaceFont
                )
            }

            if (onClose != null) {
                androidx.compose.material3.Text(
                    text = "✕",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    fontFamily = InterfaceFont,
                    modifier =
                        androidx.compose.ui.Modifier
                            .clickable { onClose() }
                            .padding(6.dp)
                )
            }
        }

        androidx.compose.foundation.layout.Row(
            horizontalArrangement =
                androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            MissedCallButton(
                label = "Call",
                primary = true,
                modifier = androidx.compose.ui.Modifier.weight(1f),
                onClick = onCallBack
            )

            MissedCallButton(
                label = "Text",
                modifier = androidx.compose.ui.Modifier.weight(1f),
                onClick = onText
            )

            MissedCallButton(
                label = "Reminder",
                modifier = androidx.compose.ui.Modifier.weight(1f),
                onClick = onRemind
            )
        }
    }
}

/*
 * The three action buttons: same size, same text, one line each.
 */
@Composable
private fun MissedCallButton(
    label: String,
    modifier: androidx.compose.ui.Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier =
            modifier
                .height(38.dp)
                .background(
                    if (primary) AccentOrange else BackgroundBlack,
                    androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                .clickable { onClick() },
        contentAlignment =
            androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = label,
            color = if (primary) BackgroundBlack else PrimaryText,
            fontSize = 10.5.sp,
            lineHeight = 12.sp,
            fontFamily = InterfaceFont,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/*
 * HOME ALERT
 *
 * After a missed call, home opens behind a card you have to clear:
 * ✕, Back or the Home button. Each call shows once here; it stays on
 * the ⚡ until you act on it.
 */
private const val AlertedKey = "alerted_ids"

@Composable
internal fun MissedCallsLayer(
    showAlert: Boolean,
    homeRequestVersion: Int,
    onStatus: (String) -> Unit
) {

    MissedCallWatcher()

    val context =
        LocalContext.current

    val prefs =
        androidx.compose.runtime.remember {
            context.getSharedPreferences(MissedCallPrefs, Context.MODE_PRIVATE)
        }

    var alerted by
    androidx.compose.runtime.remember {
        mutableStateOf(
            prefs.getStringSet(AlertedKey, emptySet()).orEmpty()
        )
    }

    val toShow =
        MissedCallsWaiting.filterNot { it.id.toString() in alerted }

    fun dismissAlert() {
        val updated =
            (alerted + toShow.map { it.id.toString() })
                .toList()
                .takeLast(200)
                .toSet()

        prefs.edit().putStringSet(AlertedKey, updated).apply()
        alerted = updated
    }

    /*
     * The Home button clears it too. Only a press after the alert
     * appeared counts.
     */
    val versionWhenShown =
        androidx.compose.runtime.remember(toShow.isNotEmpty()) {
            homeRequestVersion
        }

    LaunchedEffect(homeRequestVersion) {
        if (
            toShow.isNotEmpty() &&
            homeRequestVersion != versionWhenShown
        ) {
            dismissAlert()
        }
    }

    if (
        !showAlert ||
        toShow.isEmpty()
    ) {
        return
    }

    androidx.activity.compose.BackHandler {
        dismissAlert()
    }

    androidx.compose.foundation.layout.Box(
        modifier =
            androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(BackgroundBlack.copy(alpha = 0.94f))
                .pointerInput(Unit) {
                    detectTapGestures { }
                }
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 18.dp,
                    bottom = 16.dp
                )
    ) {

        androidx.compose.foundation.layout.Column(
            verticalArrangement =
                androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
        ) {

            androidx.compose.foundation.layout.Row(
                modifier =
                    androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(start = DaylightCameraLane),
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text(
                    text =
                        if (toShow.size == 1) "Missed call"
                        else "${toShow.size} missed calls",
                    color = MissedCallFire,
                    fontSize = 18.sp,
                    lineHeight = 21.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                )

                androidx.compose.foundation.layout.Box(
                    modifier =
                        androidx.compose.ui.Modifier
                            .size(30.dp)
                            .background(
                                SurfaceBlack,
                                androidx.compose.foundation.shape.CircleShape
                            )
                            .clickable { dismissAlert() },
                    contentAlignment =
                        androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "✕",
                        color = PrimaryText,
                        fontSize = 12.sp,
                        fontFamily = InterfaceFont
                    )
                }
            }

            androidx.compose.foundation.layout.Column(
                modifier =
                    androidx.compose.ui.Modifier
                        .verticalScroll(
                            androidx.compose.foundation.rememberScrollState()
                        ),
                verticalArrangement =
                    androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                toShow.forEach { call ->
                    MissedCallActionsCard(
                        call = call,
                        onCallBack = {
                            MissedCallStore.markHandled(context, call)
                            dialMissedCall(context, call)
                        },
                        onText = {
                            MissedCallStore.markHandled(context, call)
                            textMissedCall(context, call)
                        },
                        onRemind = {
                            pickCallBackTime(context) { millis ->

                                val result =
                                    scheduleCallBack(context, call, millis)

                                onStatus(result)

                                /*
                                 * The status bubble sits behind this
                                 * card, so say it here too.
                                 */
                                android.widget.Toast
                                    .makeText(
                                        context,
                                        result.removePrefix("✓ "),
                                        android.widget.Toast.LENGTH_LONG
                                    )
                                    .show()
                            }
                        }
                    )
                }
            }
        }
    }
}

/*
 * From the home alert: a new text to them in the messaging app.
 */
internal fun textMissedCall(
    context: Context,
    call: MissedCall
) {
    runCatching {
        context.startActivity(
            Intent(
                Intent.ACTION_SENDTO,
                Uri.parse("smsto:" + call.number)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
